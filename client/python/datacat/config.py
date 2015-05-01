import ConfigParser
import os
from .auth import HMACAuth, HMACAuthSRS

MODES = "prod dev test".split(" ")
ENDPOINTS = "children path search datasets containers groups folders".split(" ")
DATATYPES = "json xml txt".split(" ")

__version__ = "0.2"

INSTALLATIONS = "srs fermi".split(" ")

DEV_SRS_URL = "http://scalnx-v04.slac.stanford.edu:8180/datacat-v%s-SNAPSHOT/r" %(__version__)
PROD_SRS_URL = "http://srs.slac.stanford.edu/datacat-v%s/r" %(__version__)
DEV_LSST_DM_URL = "http://lsst-db2.slac.stanford.edu:8180/datacat-v%s-SNAPSHOT/r" %(__version__)
PROD_LSST_DM_URL = "http://lsst-db2.slac.stanford.edu:8180/datacat-v%s/r" %(__version__)

SRS_EXPERIMENTS = "srs exo cdms lsst lsst-desc lsst-camera".split(" ")
LSST_DM_EXPERIMENT = "lsst-dm"

def default_url(domain=None, mode=None):
    domain = domain if domain else "srs"
    mode = mode if mode else "dev"
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
    config = ConfigParser.SafeConfigParser()
    defaults = default_config(domain, mode)    
    if path:
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

def auth_from_config(config):
    config = config.copy()
    auth_type = config.get("auth_type", None)
    if auth_type:
        del config["auth_type"]
        auth_params = {}
        auth_params["url"] = config.get("url")

        for key in config.keys():
            if key.startswith("auth_"):
                val = config.pop(key)
                key = key[len("auth_"):]
                auth_params[key] = val
        if auth_type == HMACAuth.__name__:
            auth_strategy = HMACAuth(**auth_params)
        elif auth_type == HMACAuthSRS.__name__:
            auth_strategy = HMACAuthSRS(**auth_params)
        else:
            auth_strategy = None
        return auth_strategy
    return None

