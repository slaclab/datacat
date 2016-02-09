# Datacat

Datacat is a system for the tracking and organization of files, their 
replicas, and user-defined metadata across heterogeneous storage systems.

Main Components
-------

Conceptually, Datacat can be viewed as these main components:

* A Internal Datastore Interface with implementations for Oracle, MySQL, and HSQLDB
* REST APIs to interact with the Datastore
* A Web Application which uses the REST APIs
* Client libraries (in Python and Java) which use the REST APIs
* A CLI implemented with the Python libraries
* Crawlers which query the API for changes and act upon them

## Primitives

Datacat defines two main primitives

* **Dataset**, the abstract representation of a data file
* **Containers** (groups and folders)*, which are analagous to POSIX directories.

### Datasets

A Dataset consists of one or more versions of a file, each with one or more possible physical locations.

* Metadata can be applied to a specific version
* By applying a view, you can disambiguate to a specific version at specific location(s)
* If no version is specified, the view is interpreted to use the latest version

### Containers

* *Folders* are the simplest containers. They are completely analagous to a POSIX directory
* *Groups* are a special container which may only contain datasets. They are meant to logically group datasets which might be treated as a macro dataset.

# Components

## REST api

We believe an open, well-defined REST API is the best way to support a globally diverse project without limiting users to a specific language.

* All supported clients are implemented using this REST API.

## Crawler

Listens/polls for resource updates (i.e. new datasets created/replicas added to existing datasets). The Crawler is meant to automatically extract metadata out of the file which may be useful for indexing and searching on. Typically, this includes the file size, checksum, event count, user metadata.

## Searching

Typically, search is conducted by defining container targets to search, and a query string. Optional arguments include sort fields.

For more information on this, see [Searching].


## Core (Internals and concepts)

### VFS to the Datastore

The core of Datacat Server is implemented as a VFS

* The VFS layer enforces permissions
* The VFS layer implements caching
* The VFS layer abstracts interactions with the Datastore layer

### ACLs

Datacat implements a subset of AFS ACLs. User authentication and group checking is pluggable

* Authentication handled by the REST web application
* Authorization handled in the VFS 
 * Kind of like: `True in [(e.group in userGroups and e.permission == requestedPermission) for e in acl]`

* It's recommended to use a web-based SSO system which has group support
* We have a "GroupManager" service which can retrieve the list of groups a user is in and manage group membership

### Metadata

There are several types of metadata for any given Datacat primitive:

1. Datacat metadata - dataset/container name, creation time, etc...
2. File Replica metadata - size in bytes, checksum, etc...
 * These are individual to each replica
3. User metadata - any key:value pair (where value is a string, number, or timestamp)
 * These are shared across all replicas
4. Foreign metadata - Relational metadata not maintained by the datacatalog (i.e. not file or user)

The first three types are indexed in our current SQL implementation.

#### Foreign Metadata

Foreign metadata is implemented by plugins which define a pseudo-table. Typically, foreign metadata is another table in the same database as the core datacat tables.

* That pseudo table MUST be relatable to some other form of data for a given dataset or container.
** But it doesn't actually have to be a table! It should just act like one. (For example, SIMBAD database?)
* Similar to PostgreSQL Foreign Data Wrappers
* More work still needed.

### Datastore

Datacat defines an interface to a datastore.

* Currently one implementation exists which uses a SQL database for the store
 * SQL database must implement temporary tables for Search
* Other datastores implementations are possible, such as BerkeleyDB, Cassandra, or even potentially a file system (btrfs?)
* Application-level replication could be implemented for high availability parallel deployments
 * Through use of Paxos, Raft, zab, etc...
* Datastore-level replication could be used as well

## Clients

These are clients we directly support.

* Support Web Application Client/WUI (Web User Interface)
* Support native python client and a java client
* Support a CLI implemented with python client

## Python client example (Search)

#### Dump Datacat path, resource path, vetoEvents metadata value for datasets at SLAC
```python
#!/usr/bin/python
from datacat import client_from_config_file

client = client_from_config_file()  # reads ~/.datacat/default.cfg

container_target_pattern = "/LSST/Data/Raw/*"     # Search inside child containers of Raw
query = 'run > 6200 and runQuality =~ "GO*"'      # Filter query (in SQL: run > 6200 AND runQuality LIKE 'GO%')
sort = ["run-", "events"]                         # Sort run desc, events asc (asc default). These are retrieved
show = ["vetoEvents"]                             # Retrieve vetoEvents metadata as well

datasets = client.search(container_target_pattern, site="SLAC", query=query, sort=sort, show=show)

print("Path\tResource\tvetoEvents")
for dataset in datasets:
    print("%s\t%s\t%s", %(dataset.path, dataset.resource, dataset.metadata['vetoEvents']))
    with f as open(dataset.resource, "r+b"):
        dat = f.read()
        # do some work with the file
```
