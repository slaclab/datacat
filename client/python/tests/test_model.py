__author__ = 'bvan'
import unittest
import json

from datacat.model import Dataset, pack, unpack


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
                 "type": "integer"
                },
                {"key": "sQuality",
                 "value": "good",
                 "type": "string"
                }
            ]
        }
        json_str = json.dumps(ds)
        print json_str
        ds_new = unpack(json_str)
        print(pack(ds_new))
        ds_new2 = unpack(pack(ds_new), Dataset)
        print(pack(ds_new2))

    def test_pack(self):
        vmd = {
                "nRun": 1234,
                "sQuality":"good"
                }
        ds = Dataset(name="ds001.dat",fileFormat="dat",dataType="DAT", versionMetadata=vmd)
        print(pack(ds))

if __name__ == '__main__':
    unittest.main()