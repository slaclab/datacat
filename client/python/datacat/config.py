ENDPOINTS = "children path search datasets containers groups folders".split(" ")
DATATYPES = "json xml txt".split(" ")

__version__ = "0.2"

INSTALLATIONS = "srs fermi".split(" ")

DEV_SRS_URL = "http://scalnx-v04.slac.stanford.edu:8180/datacat-v%s-SNAPSHOT/r" %(__version__)
PROD_SRS_URL = "http://srs.slac.stanford.edu/datacat-v%s/r" %(__version__)
DEV_LSST_DM_URL = "http://lsst-db2.slac.stanford.edu:8180/datacat-v%s-SNAPSHOT/r" %(__version__)
#DEV_FERMI_URL

SRS_EXPERIMENTS = "srs exo cdms lsst lsst-desc lsst-camera".split(" ")
LSST_DM_EXPERIMENT = "lsst-dm"

def CONFIG_URL(experiment, mode="dev"):
    experiment = experiment or "srs"
    is_srs = experiment.lower() in SRS_EXPERIMENTS
    if is_srs:
        if mode == "dev":
            return DEV_SRS_URL
        elif mode == "prod":
            return PROD_SRS_URL
    if experiment == LSST_DM_EXPERIMENT:
        return DEV_LSST_DM_URL
    return None
