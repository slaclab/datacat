
import os
from .auth import auth_from_config
from .config import config_from_file
from .error import DcClientException, checked_error
from .http_client import HttpClient
from .model import *


# noinspection PyPep8Naming,PyShadowingBuiltins,PyUnusedLocal
class Client(object):

    """
    Pythonic Client for interacting with the data catalog. This client interacts solely through JSON.
    """

    def __init__(self, url, auth_strategy=None, *args, **kwargs):
        self.http_client = HttpClient(url, auth_strategy, *args, **kwargs)
        self.url = url

    @checked_error
    def path(self, path, versionId=None, site=None, stat=None):
        resp = self.http_client.path(path, versionId, site)
        return unpack(resp.content)

    @checked_error
    def children(self, path, versionId=None, site=None, stat=None, offset=None, max_num=None):
        resp = self.http_client.children(path, versionId, site, stat, offset, max_num)
        return unpack(resp.content)

    def exists(self, path, versionId=None, site=None):
        try:
            self.path(path, versionId, site)
            return True
        except DcClientException as e:
            if "NoSuchFile" in e.type:
                return False
            raise e

    @checked_error
    def mkdir(self, path, type="folder", parents=False, metadata=None, **kwargs):
        """
        Make a new Container
        :param path: Container Target path
        :param type: Container type. Defaults to folder.
        :param parents: If true, will create intermediate Folders as required.
        :param metadata: Metadata to add to when creating folder
        :param kwargs: Additional attributes to add to the container object
        :return: A :class`requests.Response` object. A user can use Response.content to get the content.
        The object will be a Folder
        """
        container = None
        if type.lower() == "folder":
            container = Folder(path=path, name=path.split("/")[-1], metadata=metadata, **kwargs)
        elif type.lower() == "group":
            container = Group(path=path, name=path.split("/")[-1], metadata=metadata, **kwargs)
        if parents:
            parts = []
            parentpath = os.path.dirname(path)
            while not self.exists(parentpath):
                parts.append(os.path.split(parentpath)[1])
                parentpath = os.path.dirname(parentpath)
            if len(parts):
                for part in reversed(parts):
                    parentpath = os.path.join(parentpath, part)
                    self.mkdir(parentpath)
        resp = self.http_client.mkdir(path, payload=pack(container), type=type)
        return unpack(resp.content)

    def mkfolder(self, path, parents=False, metadata=None, **kwargs):
        """
        Make a new Folder
        :param path: Container Target path
        :param parents: If true, will create intermediate Folders as required.
        :param metadata: Metadata to add to when creating folder
        :param kwargs: Additional attributes to add to the folder object
        """
        return self.mkdir(path, "folder", parents, metadata, **kwargs)

    def mkgroup(self, path, parents=False, metadata=None, **kwargs):
        """
        Make a new Folder
        :param path: Container Target path
        :param parents: If true, will create intermediate Folders as required.
        :param metadata: Metadata to add to when creating folder
        :param kwargs: Additional attributes to add to the folder object
        :return: A :class`requests.Response` object. A user can use Response.content to get the content.
        The object will be a Folder
        """
        return self.mkdir(path, "group", parents, metadata, **kwargs)

    @checked_error
    def mkds(self, path, name, dataType, fileFormat, versionId="new", site=None, resource=None, versionMetadata=None,
             **kwargs):
        """
        Make a dataset.
        :param path: Container Target path
        :param name: Name of Dataset you wish to create
        :param dataType: User-Defined Data Type of Dataset. This is often a subtype of a file format.
        :param fileFormat: The File Format of the Dataset (i.e. root, fits, tar.gz, txt, etc...)
        :param versionId: Desired versionId. By default, it is set to "new", which will result in a versionId of 0.
        :param site: Site where the dataset physically resides (i.e. SLAC, IN2P3)
        :param versionMetadata: Metadata to add to registered version if registering a version.
        :param resource: The actual file resource path at the given site (i.e. /nfs/farm/g/glast/dataset.dat)
        :param kwargs: Additional attributes to pass throught to build_dataset
        :return: A representation of the dataset that was just created.
        """
        ds = build_dataset(name, dataType, fileFormat, site=site, versionId=versionId, resource=resource,
                           versionMetadata=versionMetadata, **kwargs)
        resp = self.http_client.mkds(path, pack(ds), **kwargs)
        return unpack(resp.content)

    # noinspection PyIncorrectDocstring
    def create_dataset(self, path, name, dataType, fileFormat,
                       versionId="new", site=None, versionMetadata=None, resource=None, **kwargs):
        """
        See mkds
        """
        return self.mkds(path, name, dataType, fileFormat, versionId, site, resource, versionMetadata, **kwargs)

    @checked_error
    def mkver(self, path, versionId="new", site=None, resource=None, versionMetadata=None, **kwargs):
        """
        Make a dataset version and optionally include enough information for a location
        :param path: Target Dataset path
        :param versionId: Desired versionId. By default, it is set to "new", which will result in the next version id.
        :param site: Site where the dataset physically resides (i.e. SLAC, IN2P3)
        :param versionMetadata: Metadata to add
        :param resource: The actual file resource path at the given site (i.e. /nfs/farm/g/glast/dataset.dat)
        :param kwargs: Additional version or location attributes
        :return: A representation of the dataset that was just created.
        """
        # We piggy back off of build_dataset
        ds = build_dataset(versionId=versionId, site=site, resource=resource, versionMetadata=versionMetadata)
        resp = self.http_client.mkds(path, pack(ds), **kwargs)
        return unpack(resp.content)

    @checked_error
    def mkloc(self, path, site, resource, versionId="current", **kwargs):
        """
        Make a dataset location.
        :param path: Target Dataset path
        :param site: Site where the dataset physically resides (i.e. SLAC, IN2P3)
        :param versionId: Desired versionId to add this dataset to. Defaults to current version.
        :param resource: The actual file resource path at the given site (i.e. /nfs/farm/g/glast/dataset.dat)
        :param kwargs: Additional location attributes
        :return: A representation of the dataset that was just created.
        """
        # We piggy back off of build_dataset
        location = {site: site, resource: resource}
        location.update(**kwargs)
        ds = build_dataset(location=location)
        resp = self.http_client.mkds(path, pack(ds), versionId=versionId)
        return unpack(resp.content)

    @checked_error
    def rmdir(self, path, type="folder", **kwargs):
        """
        Remove a container.
        :param path: Path of container to remove.
        :param type: Type of container (Group or Folder). This will be removed in a future version.
        :return: A :class`requests.Response` object. A client can inspect the status code.
        """
        self.http_client.rmdir(path, type)
        return True

    @checked_error
    def rmds(self, path, **kwargs):
        """
        Remove a dataset.
        :param path: Path of dataset to remove
        :param kwargs:
        """
        self.http_client.rmds(path)
        return True

    # noinspection PyIncorrectDocstring
    def delete_dataset(self, path, **kwargs):
        """
        See rmds
        """
        return self.rmds(path)

    @checked_error
    def patchdir(self, path, container, type="folder", **kwargs):
        """
        Patch a container.
        :param path: Path of the dataset to patch.
        :param type: Container type. Defaults to folder.
        :param container: A dict object or a dataset.model.Group/Folder object representing the changes to be applied
        to the container.
        :param kwargs:
        :return: A representation of the patched dataset
        """
        if isinstance(container, Container):
            pass
        elif type == "group":
            container = Group(**container)
        elif type == "folder":
            container = Folder(**container)

        resp = self.http_client.patchdir(path, pack(container), type, **kwargs)
        return unpack(resp.content)

    @checked_error
    def patchds(self, path, dataset, versionId="current", site=None):
        """
        Patch a dataset.
        :param path: Path of the dataset to patch.
        :param dataset: A dict object or a dataset.model.Dataset object representing the changes to be applied to the
        dataset
        :param versionId: If specified, identifies the version to patch. Otherwise, it's assumed to patch the current
        version, should it exist.
        :param site: If specified, identifies the specific location to be patched (i.e. SLAC, IN2P3)
        :return: A representation of the patched dataset
        """
        ds = dataset if type(dataset) == Dataset else Dataset(**dataset)
        resp = self.http_client.patchds(path, pack(ds), versionId, site)
        return unpack(resp.content)

    # noinspection PyIncorrectDocstring
    def patch_container(self, path, container, type="folder", **kwargs):
        """
        See patchdir.
        """
        return self.patchdir(path, container, type, **kwargs)

    # noinspection PyIncorrectDocstring
    def patch_dataset(self, path, dataset, versionId="current", site=None, **kwargs):
        """
        See patchds
        """
        return self.patchds(path, dataset, versionId, site, **kwargs)

    @checked_error
    def search(self, target, versionId=None, site=None, query=None, sort=None, show=None, offset=None, max_num=None,
               **kwargs):
        """Search a target. A target is a Container of some sort. It may also be specified as a glob, as in:
         1. /path/to - target /path/to _only_
         2. /path/to/* - target is all containers directly in /path/to/
         3. /path/to/** - target is all containers, recursively, under /path/to/
         4. /path/to/*$ - target is only folders directly under /path/to/
         5. /path/to/**^ - target is only groups, recursively, under /path/to/

        :param target: The path (or glob-like path) of which to search
        :param versionId: Optional VersionId to filter by
        :param site: Optional site to filter by
        :param query: The query
        :param sort: Fields and Metadata fields to sort on.
        :param show: Metadata fields to optionally return
        :param offset: Offset at which to start returning objects.
        :param max_num: Maximum number of objects to return.
        """
        resp = self.http_client.search(target, versionId, site, query, sort, show, offset, max_num)
        return unpack(resp.content)

    @checked_error
    def permissions(self, path, group=None):
        """
        Retrieve the effective permissions.
        :param path: Path of the object to retrieve.
        :param group: If specified, this should be a group specification of the format "{name}@{project}"
        :return: A :class`datacat.model.AclEntry` object representing permissions
        """
        resp = self.http_client.permissions(path, group)
        return unpack(resp.content)

    @checked_error
    def listacl(self, path):
        """
        Retrieve the Access Control List of a given path
        :param path: Path of the object to retrieve.
        :return: A list of :class`datacat.model.AclEntry` objects.
        """
        resp = self.http_client.listacl(path)
        return unpack(resp.content)

    @checked_error
    def patchacl(self, path, acl):
        """
        Patch an ACL.
        :param path: Path of the object to patch (must be container)
        :param acl: A list of :class`datacat.model.AclEntry` objects or a similar dict
        :return: An updated list of :class`datacat.model.AclEntry` objects.
        """
        resp = self.http_client.patchacl(path, pack(acl))
        return unpack(resp.content)


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


def client_from_config(config):
    auth_strategy = auth_from_config(config)
    return Client(auth_strategy=auth_strategy, **config)
