from .client import client_from_config
from .config import config_from_file as cff
from .config import __version__

_DEV_SRS_URL = "http://srs.slac.stanford.edu/datacat-v%s-SNAPSHOT/r" % __version__
_PROD_SRS_URL = "http://srs.slac.stanford.edu/datacat-v%s/r" % __version__


def config_from_file(path=None, override_section=None, mode=None, override_url=None):
    """
    :param path: Path to read file from. If None, will read from
     default locations.
    :param override_section: Section in config file with overridden
     values. If None, only defaults section will be read.
    :param mode: prod or dev
    :param override_url: Optional overriding URL
    :return: Configured client
    :except: OSError if path is provided and the file doesn't exist.
    """
    url = override_url or _default_url(mode)
    return cff(path, override_section, override_url=url)


def client_from_config_file(path=None, override_section=None):
    """
    Return a new client from a config file.
    :param path: Path to read file from. If None, will read from
     default locations.
    :param override_section: Section in config file with overridden
     values. If None, only defaults section will be read.
    :return: Configured client
    :except: OSError if path is provided and the file doesn't exist.
    """
    config = config_from_file(path, override_section)
    return client_from_config(config)


def _default_url(mode=None):
    return _PROD_SRS_URL if mode in ('prod', None) else _DEV_SRS_URL
