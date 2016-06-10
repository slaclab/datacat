#!/usr/bin/python

"""
Make sure you have your environment setup!
1. You need Python requests >= 2.3.0
2. You need the datacat module directory in your path.
"""

from datacat import client_from_config
from datacat.srs import default_config

import pprint

client = client_from_config(default_config("exo", "prod"))

# Path example
print("\nPath Example:")

path = '/EXO/Data/Raw/cxd/run00006220-0000.cxd'

try:
    dataset = client.path(path)
    pprint.pprint(dataset.__dict__)
except Exception as e:
    print("Error processing request:" + str(e))

# Children example

print("\nChildren Example:")
path = '/EXO/Data/Raw'

try:
    children = client.children(path, show_request=True)
    pprint.pprint([child.__dict__ for child in children])
except Exception as e:
    print("Error processing request:" + str(e))


# Search example
print("\nSearch Example:")

path_pattern = "/EXO/Data/Raw/*"                     # Search containers under Raw
query = 'nRun gt 6200 and exo.runQuality =~ "GO*"'   # Filter query
sort = ["nRun-", "nEvents"]                          # Sort nRun desc, nEvents asc (asc default). These are retrieved.
show = ["nVetoEvents"]                               # Retrieve nVetoEvents as well.

try:
    datasets = client.search(path_pattern, query=query, sort=sort, show=show)
    for dataset in datasets:
        print(dataset.locations[0].resource)

        """
        When searching, metadata will be returned to Dataset.metadata. When you are just querying a Dataset,
        using something like client.path("/path/to/dataset.dst"), the metadata will be in versionMetadata
        """
        print("\t" + str(dataset.metadata))
except Exception as e:
    print("Error processing request:" + str(e))
