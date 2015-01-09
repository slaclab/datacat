#!/usr/bin/python

"""
Make sure you have your environment setup!
1. You need Python requests >= 2.3.0
2. You need the datacat module directory in your path.
"""

from datacat import Client, unpack
from datacat.config import CONFIG_URL

import pprint

client = Client(CONFIG_URL("exo", mode="dev"))

# Path example
print("\nPath Example:")

path = '/EXO/Data/Raw/cxd/run00006220-0000.cxd'

resp = client.path(path)

if resp.status_code == 200:
    dataset = unpack(resp.content)
    pprint.pprint(dataset.__dict__)
else:
    print("Error processing request:" + str(resp.status_code))

# Children example

print("\nChildren Example:")
path = '/EXO/Data/Raw'

resp = client.children(path, show_request=True)

if resp.status_code == 200:
    children = unpack(resp.content)
    pprint.pprint([child.__dict__ for child in children])
else:
    print("Error processing request:" + str(resp.status_code))


# Search example
print("\nSearch Example:")

path_pattern = "/EXO/Data/Raw/*"                     # Search containers under Raw
query = 'nRun gt 6200 and exo.runQuality =~ "GO%"'   # Filter query
sort = ["nRun-", "nEvents"]                          # Sort nRun desc, nEvents asc (asc default). These are retrieved.
show = ["nVetoEvents"]                               # Retrieve nVetoEvents as well.

resp = client.search(path_pattern, query=query, sort=sort, show=show)

if resp.status_code == 200: # 200 status code ==  success
    """
    The raw json representation of a datacatalog object (Dataset) packs up metadata into a list
    of objects of the form:
       [{'key':'nRun', '$type':'integer', 'value':6201}, ...]
    The unpack method fixes that, but also does a few other things to play nice, like putting
    the variables into the Dataset.__dict__ object, so you can address them directly
    """
    datasets = unpack(resp.content)    # unpack the json list to a python list
    for dataset in datasets:
        print(dataset.resource)

        """
        When searching, metadata will be returned to Dataset.metadata. When you are just querying a Dataset,
        using something like client.path("/path/to/dataset.dst"), the metadata will be in versionMetadata
        """
        print("\t" + str(dataset.metadata))
else:
    print("Error processing request:" + str(resp.status_code))


