import unittest

from datacat.auth import HMACAuth, HMACAuthSRS
from datacat import config_from_file, auth_from_config, client_from_config, client_from_config_file
from datacat.config import default_config, default_url

class ConfigTest(unittest.TestCase):

    def test_srs_config_file(self):
        config = config_from_file("config_srs.ini")
        auth = auth_from_config(config)
        self.assertEqual(type(auth), HMACAuthSRS)

    def test_srs_config_file_none(self):
        config = config_from_file("config_srs.ini", "dev")
        print(config)
        auth = auth_from_config(config)
        self.assertEqual(auth, None)

    def test_hmac_config_file(self):
        _file = "config_hmac.ini"
        config = config_from_file(_file)
        auth = auth_from_config(config)
        self.assertEqual(type(auth), HMACAuth)
        client = client_from_config_file(_file)
        self.assertEqual(type(client.http_client.auth_strategy), HMACAuth)

    def test_no_config(self):
        config = config_from_file()
        auth = auth_from_config(config)
        self.assertEqual(config["url"], default_url())
        self.assertEqual(auth, None)

        client = client_from_config_file()
        self.assertEqual(client.http_client.auth_strategy, None)

    def test_default_config_with_mode(self):
        config = default_config(mode="prod")
        auth = auth_from_config(config)
        self.assertEqual(config["url"], default_url(mode="prod"))
        self.assertEqual(auth, None)

        client = client_from_config(config)
        self.assertEqual(client.http_client.auth_strategy, None)
        self.assertEqual(client.url, default_url(mode="prod"))

    def test_no_config_with_mode(self):
        config = config_from_file(mode="prod")
        auth = auth_from_config(config)
        self.assertEqual(config["url"], default_url(mode="prod"))
        self.assertEqual(auth, None)

        client = client_from_config_file()
        self.assertEqual(client.http_client.auth_strategy, None)
