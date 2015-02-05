ENDPOINTS = "children path search datasets containers groups folders".split(" ")
DATATYPES = "json xml txt".split(" ")

__version__ = "0.2-ALPHA"

INSTALLATIONS = "srs fermi".split(" ")

DEV_SRS_URL = "http://scalnx-v04.slac.stanford.edu:8180/org-srs-webapps-datacat-0.2-SNAPSHOT/r"
#PROD_SRS_URL
#DEV_FERMI_URL

SRS_EXPERIMENTS = "srs exo cdms lsst lsst-desc lsst-camera".split(" ")

def CONFIG_URL(experiment, mode="dev"):
    is_srs = experiment.lower() in SRS_EXPERIMENTS
    if mode is "dev" and is_srs:
        return DEV_SRS_URL
