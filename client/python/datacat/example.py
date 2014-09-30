from client import Client
from model import unpack
from config import DEFAULT_BASE_URL
import pprint

client = Client(DEFAULT_BASE_URL)

path_pattern = "/EXO/Data/Raw/*"                     # Search containers under Raw
query = 'nRun gt 6200 and exo.runQuality =~ "GO%"'   # Filter query
sort = ["nRun-", "nEvents"]                          # Sort nRun desc, nEvents asc (asc default). These are also retrieved.
show = ["nVetoEvents"]                               # Retrieve nVetoEvents as well.

resp = client.search(path_pattern, q=query, sort=sort, show=show)

for raw_dataset in resp.json():
    dataset = unpack(raw_dataset)
    print(dataset.fileSystemPath)
    print("\t" + str(dataset.metadata))


