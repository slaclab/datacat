
import gevent
import gevent.monkey
gevent.monkey.patch_socket()

from datacat.client import Client
from datacat.config import CONFIG_URL
from datacat.model import pack

from gevent.queue import Queue
import requests
import json
from jinja2 import Template

default_template = Template("{{ name }}.{{ fileFormat }}")

q = Queue()

c = Client(CONFIG_URL("srs","prod"))

for i in c.search("/LSST/mirror/BNL3/workarea/ccdtest/e2v/113-03/flat/20140709-112014", max_num=10):
    q.put(i)

dl_url = "http://srs.slac.stanford.edu/DataCatalog/get?dataset=%d&datasetVersion=%d&datasetLocation=%d"

def worker(thread_id):
    while not q.empty():
        dataset = q.get()
        ds = dataset.pk
        ver = dataset.versionPk
        loc = dataset.locations[0].pk
        # Roundabout way to create an environment for the template
        print(default_template.render(json.loads(pack(dataset))))
        response = requests.get(dl_url %(ds, ver, loc))
        for chunk in response.iter_content(8192):
            pass
        #print('Path %s: created %s on Worker %d' % (work_item, result["created"], thread_id))
    return True

def start_workers():
    threads = []
    for i in range(4):
        threads.append(gevent.spawn(worker, i))
    gevent.joinall(threads)

start_workers()
