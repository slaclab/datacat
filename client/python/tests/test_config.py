import unittest

from datacat.auth import HMACAuth, HMACAuthSRS
from datacat import auth_from_config, client_from_config_file, config_from_file
from datacat.srs import _default_url
from datacat.srs import config_from_file as srs_config_from_file
from datacat.srs import client_from_config_file as srs_client_from_config_file
import os

TEST_ROOT = os.path.dirname(__file__)


class ConfigTest(unittest.TestCase):

    def test_hmac_config_file(self):
        _file = "config_hmac.ini"
        config = config_from_file(os.path.join(TEST_ROOT, _file))
        auth = auth_from_config(config)
        self.assertEqual(type(auth), HMACAuth)
        client = client_from_config_file(os.path.join(TEST_ROOT, _file))
        self.assertEqual(type(client.http_client.auth_strategy), HMACAuth)

    def test_srs_config_file(self):
        config = srs_config_from_file(os.path.join(TEST_ROOT, "config_srs.ini"))
        auth = auth_from_config(config)
        self.assertEqual(type(auth), HMACAuthSRS)

    def test_srs_config_file_none(self):
        config = srs_config_from_file(os.path.join(TEST_ROOT, "config_srs.ini"), "dev")
        print(config)
        auth = auth_from_config(config)
        self.assertEqual(auth, None)

    def test_srs_no_config(self):
        config = srs_config_from_file(path=os.path.join(TEST_ROOT, "config_blank.ini"))
        auth = auth_from_config(config)
        self.assertEqual(config["url"], _default_url())
        self.assertEqual(auth, None)

        client = srs_client_from_config_file(path=os.path.join(TEST_ROOT, "config_blank.ini"))
        self.assertEqual(client.http_client.auth_strategy, None)

    def test_srs_no_config_with_mode(self):
        config = srs_config_from_file(os.path.join(TEST_ROOT, "config_blank.ini"), mode="prod")
        auth = auth_from_config(config)
        self.assertEqual(config["url"], _default_url(mode="prod"))
        self.assertEqual(auth, None)

        client = srs_client_from_config_file(path=os.path.join(TEST_ROOT, "config_blank.ini"))
        self.assertEqual(client.http_client.auth_strategy, None)
