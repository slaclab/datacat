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

    def __init__(self):
        self.client = Client(CONFIG_URL("lsst", mode="dev"))
        self.s = sched.scheduler(time.time, time.sleep)
        self.s.enter(60, 1, self.run, ())
        self.s.run()

    def get_cksum(self, file):
        cksum_proc = subprocess.Popen(["md5sum", file], stdout=subprocess.PIPE)
        ec = cksum_proc.wait()
        if ec != 0:
            # Handle error here, or raise exception/error
            pass
        cksum_out = cksum_proc.stdout.read().split(" ")
        cksum = cksum_out[0]
        return cksum

    def get_metadata(file):
        return None


    def run(self):
        results = None
        try:
            results = self.client.search(WATCH_FOLDER, site=WATCH_SITE, query="scanStatus = 'UNSCANNED'", max_num=1000)

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

        for dataset in results:
            file_path = dataset.resource
            dataset_path = dataset.path
            stat = os.stat(file)
            cksum = self.get_cksum(file)

            # Note: While there may only be one version of a dataset,
            # we tie the metadata to versionMetadata
            scan_result = {}
            scan_result["size"] = stat.st_size
            scan_result["checksum"] = str.format("md5:{}", cksum)
            scan_result["versionMetadata"] = self.get_metadata()

            try:
                print(scan_result)
                #result = self.client.patch_dataset().patchDataset(dataset_path, scan_result)
            except DcException as error:
                print("Encountered error while updating dataset")



