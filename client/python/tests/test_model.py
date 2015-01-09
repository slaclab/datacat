__author__ = 'bvan'
import unittest
import json

from datacat.model import Dataset, Container, pack, unpack


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

        group = [
            {"_type":"group","name":"binary","path":"/EXO/Data/Raw/binary","pk":2274878,"parentPk":2274852},
            {"_type":"group","name":"cxd","path":"/EXO/Data/Raw/cxd","pk":3007508,"parentPk":2274852},
            {"_type":"group","name":"root","path":"/EXO/Data/Raw/root","pk":2321462,"parentPk":2274852}
        ]
        json_str = json.dumps(group)
        print json_str
        group_new = unpack(json_str)
        print(pack(group_new))
        group_new2 = unpack(pack(group_new), Container)
        print(pack(group_new2))


    def test_pack(self):
        vmd = {
                "nRun": 1234,
                "sQuality":"good"
                }
        ds = Dataset(name="ds001.dat",fileFormat="dat",dataType="DAT", versionMetadata=vmd)
        print(pack(ds))

if __name__ == '__main__':
    unittest.main()