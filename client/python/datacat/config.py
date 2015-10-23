import ConfigParser
import os

MODES = "prod dev test".split(" ")
ENDPOINTS = "children path search datasets containers groups folders".split(" ")
DATATYPES = "json xml txt".split(" ")

__version__ = "0.4"

INSTALLATIONS = "srs fermi".split(" ")

DEV_SRS_URL = "http://srs.slac.stanford.edu/datacat-v%s-SNAPSHOT/r" % __version__
PROD_SRS_URL = "http://srs.slac.stanford.edu/datacat-v%s/r" % __version__
DEV_LSST_DM_URL = "http://lsst-db2.slac.stanford.edu:8180/datacat-v%s-SNAPSHOT/r" % __version__
PROD_LSST_DM_URL = "http://lsst-db2.slac.stanford.edu:8180/datacat-v%s/r" % __version__

SRS_EXPERIMENTS = "srs exo cdms lsst lsst-desc lsst-camera".split(" ")
LSST_DM_EXPERIMENT = "lsst-dm"


def default_url(domain=None, mode=None):
    domain = domain if domain else "srs"
    mode = mode if mode else "prod"
    is_srs = domain.lower() in SRS_EXPERIMENTS
    if is_srs:
        if mode == "dev":
            return DEV_SRS_URL
        elif mode == "prod":
            return PROD_SRS_URL
    if domain == LSST_DM_EXPERIMENT:
        if mode == "dev":
            return DEV_LSST_DM_URL
        elif mode == "prod":
            return PROD_LSST_DM_URL
    return None


def default_config(domain=None, mode=None):
    defaults = {}
    defaults.setdefault("url", default_url(domain, mode))
    return defaults


def config_from_file(path=None, override_section=None, domain=None, mode=None):
    """
    Return a new client from a config file.
    :param path: Path to read file from. If None, will read from
     default locations.
    :param override_section: Section in config file with overridden
     values. If None, only defaults section will be read.
    :param domain: Used to construct endpoint URL based on domain.
    :return: Configured client
    :except: OSError if path is provided and the file doesn't exist.
    """
    config = ConfigParser.SafeConfigParser()
    defaults = default_config(domain, mode)
    if path:
        os.stat(path)  # Check path exists, will throw OSError if it doesn't.
        config.read([path])
    else:
        config.read([
             os.path.expanduser("~/.datacat/default.cfg"),
             os.path.expanduser("~/.datacat.cfg")
        ])
    defaults.update(dict(config.items("defaults")) if config.has_section("defaults") else {})
    if not override_section:
        return defaults
    overrides = dict(config.items(override_section)) if config.has_section(override_section) else {}
    defaults.update(overrides)
    return defaults
