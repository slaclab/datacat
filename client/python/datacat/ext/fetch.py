
import gevent
import gevent.monkey
gevent.monkey.patch_socket()

from gevent.queue import Queue

import requests
import logging
logging.basicConfig(level=logging.DEBUG)

q = Queue()
for i in requests.get("http://scalnx-v04.slac.stanford.edu:8180/org-srs-webapps-datacat-0.2-SNAPSHOT/r/search.json/LSST/mirror/BNL3/workarea/ccdtest/e2v/113-03/flat/20140709-112014?max=10").json():
    q.put(i)

dl_url = "http://srs.slac.stanford.edu/DataCatalog/get?dataset=%d&datasetVersion=%d&datasetLocation=%d"

def worker(thread_id):
    while not q.empty():
        work_item = q.get()
        ds = work_item["pk"]
        ver = work_item["versionPk"]
        loc = work_item["locations"][0]["pk"]
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
