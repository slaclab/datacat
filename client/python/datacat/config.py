try:
    import configparser as cp
except ImportError:
    import ConfigParser as cp
import os

ENDPOINTS = "children path search datasets containers groups folders permissions".split(" ")
DATATYPES = "json xml txt".split(" ")

__version__ = "0.5"


def config_from_file(path=None, override_section=None, override_url=None):
    """
    Return a new client from a config file.
    :param path: Path to read file from. If None, will read from
     default locations.
    :param override_section: Section in config file with overridden
     values. If None, only defaults section will be read.
    :param override_url: Used to override URL from default
    :return: Configured client
    :except: OSError if path is provided and the file doesn't exist.
    """
    config = cp.SafeConfigParser()
    if path:
        os.stat(path)  # Check path exists, will throw OSError if it doesn't.
        config.read([path])
    else:
        config.read([
             os.path.expanduser("~/.datacat/default.cfg"),
             os.path.expanduser("~/.datacat.cfg")
        ])
    defaults = dict(config.items("defaults")) if config.has_section("defaults") else {}
    if override_section:
        overrides = dict(config.items(override_section)) if config.has_section(override_section) else {}
        defaults.update(overrides)
    if override_url:
        defaults["url"] = override_url
    return defaults
