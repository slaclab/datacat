from datacat.client import DcException

__author__ = 'bvan'

"""
A Simple single-threaded crawler-like application.

This crawler only scans one folder at a time, retrieving up to 1000 results at a time.

It searches for datasets which are unscanned for a particular location.

"""

import sys
import os
import sched, time
import subprocess
from datacat import Client, unpack
from datacat.config import CONFIG_URL


WATCH_FOLDER = '/LSST'
WATCH_SITE = 'SLAC'

class Crawler:

    RERUN_SECONDS = 5

    def __init__(self):
        self.client = Client("http://lsst-db2:8180/rest-datacat-v1/r")
        self.s = sched.scheduler(time.time, time.sleep)
        self.s.enter(Crawler.RERUN_SECONDS, 1, self.run, ())
        self.s.run()

    def get_cksum(self, path):
        cksum_proc = subprocess.Popen(["cksum", path], stdout=subprocess.PIPE)
        ec = cksum_proc.wait()
        if ec != 0:
            # Handle error here, or raise exception/error
            pass
        cksum_out = cksum_proc.stdout.read().split(" ")
        cksum = cksum_out[0]
        return cksum

    def get_metadata(self, path):
        return None


    def run(self):
        resp = None
        try:
            resp = self.client.search(WATCH_FOLDER, version="current", site=WATCH_SITE,
                                      query="scanStatus = 'UNSCANNED'", max_num=1000)
        except DcException as error:
            if hasattr(error, "message"):
                print("Error occurred:\nMessage: %s" %(error.message))
                if hasattr(error, "type"):
                    print("Type: %s" %(error.type))
                if hasattr(error, "cause"):
                    print("Cause: %s" %(error.cause))
            else:
                # Should have content
                print(error.content)
            sys.exit(1)

        results = [unpack(raw_dataset) for raw_dataset in resp.json()]

        for dataset in results:
            file_path = dataset.resource
            dataset_path = dataset.path
            stat = os.stat(file_path)
            cksum = self.get_cksum(file_path)

            # Note: While there may only be one version of a dataset,
            # we tie the metadata to versionMetadata
            scan_result = {}
            scan_result["size"] = stat.st_size
            scan_result["checksum"] = str(cksum)
            md = self.get_metadata(file_path)
            if md:
                scan_result["versionMetadata"] = md

            try:
                patch_resp = self.client.patch_dataset(dataset_path, versionId = dataset.versionId, site=dataset.site, scan_result)
            except DcException as error:
                print("Encountered error while updating dataset")



def main():
    c = Crawler()

if __name__ == '__main__':
    main()
