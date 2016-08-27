from __future__ import print_function
from datacat import client_from_config_file
from datacat.error import DcException
from datetime import datetime
import os
import sched
import subprocess
import sys
import time

__author__ = 'bvan'

"""
A Simple single-threaded crawler-like application.

This crawler only scans one folder at a time, retrieving up to 1000 results at a time.

It searches for datasets which are unscanned for a particular location.

"""

WATCH_FOLDER = '/LSST'
WATCH_SITE = 'SLAC'


# noinspection PyMethodMayBeStatic
class Crawler:
    RERUN_SECONDS = 5

    def __init__(self):
        self.client = client_from_config_file()  # Reads default config files or returns a default config
        self.sched = sched.scheduler(time.time, time.sleep)
        self._run()

    def start(self):
        self.sched.run()

    def _run(self):
        self.run()
        self.sched.enter(Crawler.RERUN_SECONDS, 1, self._run, ())

    def get_cksum(self, path):
        cksum_proc = subprocess.Popen(["cksum", path], stdout=subprocess.PIPE)
        ec = cksum_proc.wait()
        if ec != 0:
            # Handle error here, or raise exception/error
            pass
        cksum_out = cksum_proc.stdout.read().split(" ")
        cksum = cksum_out[0]
        return cksum

    # noinspection PyUnusedLocal
    def get_metadata(self, path):
        """
        Extract metadata from :param path
        """
        return {"FITS_RADIUS": 30}

    def run(self):
        print("Checking for new datasets at %s" % (datetime.now().ctime()))
        try:
            results = self.client.search(WATCH_FOLDER, version="current", site=WATCH_SITE,
                                         query="scanStatus = 'UNSCANNED'", max_num=1000)
        except DcException as error:
            print(error)
            sys.exit(1)

        for dataset in results:
            locations = dataset.locations
            check_location = None
            for location in locations:
                if location.site == WATCH_SITE:
                    check_location = location
                    break
            file_path = check_location.resource
            dataset_path = dataset.path
            stat = os.stat(file_path)
            cksum = self.get_cksum(file_path)

            # Note: While there may only be one version of a dataset,
            # we tie the metadata to versionMetadata
            scan_result = {"size": stat.st_size,
                           "checksum": str(cksum),
                           "locationScanned": datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%SZ'),
                           "scanStatus": "OK"}
            # UTC datetime in ISO format (Note: We need Z to denote UTC Time Zone)

            md = self.get_metadata(file_path)
            if md:
                scan_result["versionMetadata"] = md

            print(scan_result)
            try:
                patched_ds = self.client.patch_dataset(dataset_path, scan_result,
                                                       versionId=dataset.versionId, site=WATCH_SITE)
                print("Updated Dataset:")
                print(patched_ds)
            except DcException as error:
                print("Encountered error while updating dataset")
                print(error)

        return True


def main():
    c = Crawler()
    c.start()


if __name__ == '__main__':
    main()
