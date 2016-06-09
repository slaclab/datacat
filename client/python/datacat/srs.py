from .client import client_from_config
from .config import config_from_file as cff
from .config import __version__

_DEV_SRS_URL = "http://srs.slac.stanford.edu/datacat-v%s-SNAPSHOT/r" % __version__
_PROD_SRS_URL = "http://srs.slac.stanford.edu/datacat-v%s/r" % __version__
_SRS_EXPERIMENTS = "srs exo cdms lsst lsst-desc lsst-camera".split(" ")


def default_config(domain=None, mode=None):
    defaults = {}
    defaults.setdefault("url", _default_url(domain, mode))
    return defaults


def config_from_file(path=None, override_section=None, domain=None, mode=None, override_url=None):
    """
    :param path: Path to read file from. If None, will read from
     default locations.
    :param override_section: Section in config file with overridden
     values. If None, only defaults section will be read.
    :param domain: Experiment domain
    :param mode: prod or dev
    :param override_url: Optional overriding URL
    :return: Configured client
    :except: OSError if path is provided and the file doesn't exist.
    """
    url = override_url or _default_url(domain, mode)
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


def _default_url(domain=None, mode=None):
    domain = domain if domain else "srs"
    mode = mode if mode else "prod"
    is_srs = domain.lower() in _SRS_EXPERIMENTS
    if is_srs:
        if mode == "dev":
            return _DEV_SRS_URL
        elif mode == "prod":
            return _PROD_SRS_URL
    return None
