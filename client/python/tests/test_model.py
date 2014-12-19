__author__ = 'bvan'
import unittest
import json

from datacat.model import unpack, default, DatasetVersion, DatasetWithView, DatasetView


class ModelTest(unittest.TestCase):

    def __init__(self, *args, **kwargs):
        super(ModelTest, self).__init__(*args, **kwargs)

    def test_unpack(self):
        ds = {
            "_type": "dataset#withView",
            "name": "dataset001.dat",
            "path": "/LSST/dataset001.dat",
            "dataType":"dat",
            "versionId": 1,
            "versionMetadata": [
                {"key": "nRun",
                 "value": 1234,
                 "_type": "integer"
                },
                {"key": "sQuality",
                 "value": "good",
                 "_type": "string"
                }
            ]
        }
        json_str = json.dumps(ds)
        print json_str
        ds_new = unpack(json_str)
        print(type(ds_new))
        print(ds_new.__dict__)
        print(json.dumps(ds_new, default=default))

    def test_pack(self):
        vmd = {
                "nRun": 1234,
                "sQuality":"good"
                }
        v = DatasetVersion(versionMetadata=vmd)
        r = DatasetView(version=v)
        ds = DatasetWithView("ds001.dat","dat","DAT", view=r)

if __name__ == '__main__':
    unittest.main()