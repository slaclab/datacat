
import os
from .model import *
from .http_client import HttpClient

class DcException(Exception):
    def __init__(self, resp):
        self.raw = resp
        try:
            err = resp.json()
            for k, v in err.items():
                setattr(self, k, v)
        except Exception:
            self.content = resp.content

    def __repr__(self):
        s = ""
        for i in self.__dict__.keys():
            s += "%s : %s\n" %(i, self.__dict__[i])
        return s


class Client(object):

    '''
    Pythonic Client for interacting with the data catalog. This client interacts solely through JSON.
    '''

    def __init__(self, base_url, *args, **kwargs):
        self.http_client = HttpClient(base_url, *args, **kwargs)
        self.base_url = base_url

    def path(self, path, versionId=None, site=None, **kwargs):
        resp = self.http_client.path(path, versionId, site, accept="json", **kwargs)
        self._check_response(resp)
        return unpack(resp.content)

    def children(self, path, versionId=None, site=None, offset=None, max_num=None, **kwargs):
        resp = self.http_client.children(path, versionId, site, offset, max_num, accept="json", **kwargs)
        self._check_response(resp)
        return unpack(resp.content)

    def exists(self, path, versionId=None, site=None, **kwargs):
        try:
            self.path(path, versionId, site)
            return True
        except DcException as e:
            if e.raw.status_code > 404 or e.raw.status_code == 400:
                raise e
        return False

    def mkdir(self, path, type="folder", parents=False, metadata=None, **kwargs):
        """
        Make a new Container
        :param path: Container Target path
        :param type: Container type. Defaults to folder.
        :param parents: If true, will create intermediate Folders as required.
        :param metadata: Metadata to add to when creating folder
        :return: A :class`requests.Response` object. A user can use Response.content to get the content.
        The object will be a Folder
        """
        parentpath = os.path.dirname(path)
        if type.lower() == "folder":
            container = Folder(path=path, name=path.split("/")[-1], metadata=metadata)
        elif type.lower() == "group":
            container = Group(path=path, name=path.split("/")[-1], metadata=metadata)
        if parents:
            parts = []
            while not self.exists(parentpath):
                parts.append(os.path.split(parentpath)[1])
                parentpath = os.path.dirname(path)
            if len(parts):
                for part in parts:
                    parentpath = os.path.join(parentpath, parts.pop())
                    self.mkdir(parentpath)
        headers = kwargs.get("headers", {})
        headers["content-type"] = "application/json"
        kwargs["headers"] = headers
        resp = self.http_client.mkdir(path, type, payload=pack(container), **kwargs)
        self._check_response(resp)
        return unpack(resp.content)

    def mkfolder(self, path, parents=False, metadata=None, **kwargs):
        """
        Make a new Folder
        :param path: Container Target path
        :param parents: If true, will create intermediate Folders as required.
        :param metadata: Metadata to add to when creating folder
        """
        return self.mkdir(path, "folder", parents, metadata, **kwargs)

    def mkgroup(self, path, parents=False, metadata=None, **kwargs):
        """
        Make a new Folder
        :param path: Container Target path
        :param parents: If true, will create intermediate Folders as required.
        :param metadata: Metadata to add to when creating folder
        :return: A :class`requests.Response` object. A user can use Response.content to get the content.
        The object will be a Folder
        """
        return self.mkdir(path, "group", parents, metadata, **kwargs)

    def mkds(self, path, name, dataType, fileFormat, versionId="new", site=None, versionMetadata=None,
             resource=None, datasetExtras=None, versionExtras=None, locationExtras=None, **kwargs):
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
        :return: A representation of the dataset that was just created.
        """
        has_version = versionId is not None
        has_location = site is not None and resource is not None
        if not has_version and has_location:
            versionId = "new"
        version = None
        location = None
        if has_version:
            version = {"versionId":versionId, "versionMetadata":versionMetadata}
            if versionExtras:
                version.update(versionExtras)
        if has_location:
            location = {"site":site, "resource":resource}
            if locationExtras:
                location.update(locationExtras)
        ds = Dataset(name=name, dataType=dataType, fileFormat=fileFormat,
                     locations=[location] if location else None, **version)
        headers = kwargs.get("headers", {})
        headers["content-type"] = "application/json"
        kwargs["headers"] = headers
        resp = self.http_client.mkds(path, pack(ds), **kwargs)
        self._check_response(resp)
        return unpack(resp.content)

    def create_dataset(self, path, name, dataType, fileFormat,
                       versionId="new", site=None, versionMetadata=None, resource=None,
                       datasetExtras=None, versionExtras=None, locationExtras=None,
                       **kwargs):
        """
        See mkds
        """
        return self.mkds(path, name, dataType, fileFormat, versionId, site, versionMetadata,
                        resource, datasetExtras, versionExtras, locationExtras, **kwargs)

    def rmdir(self, path, type="folder", **kwargs):
        """
        Remove a container.
        :param path: Path of container to remove.
        :param type: Type of container (Group or Folder). This will be removed in a future version.
        :return: A :class`requests.Response` object. A client can inspect the status code.
        """
        resp = self.http_client.rmdir(path, type, **kwargs)
        self._check_response(resp, 204)
        return True

    def rmds(self, path, **kwargs):
        """
        Remove a dataset.
        :param path: Path of dataset to remove
        :param kwargs:
        """
        resp = self.http_client.rmds(path, **kwargs)
        self._check_response(resp, 204)

    def delete_dataset(self, path, **kwargs):
        return self.rmds(path, **kwargs)

    def patchdir(self, path, container, type="folder", **kwargs):
        """
        Patch a container.
        :param path: Path of the dataset to patch.
        :param type: Container type. Defaults to folder.
        :param container: A dict object or a dataset.model.Group/Folder object representing the changes to be applied to the
        container.
        :param kwargs:
        :return: A representation of the patched dataset
        """
        headers = kwargs.get("headers", {})
        headers["content-type"] = "application/json"
        kwargs["headers"] = headers

        if isinstance(container, Container):
            pass
        elif type == "group":
            container = Group(**container)
        elif type == "folder":
            container = Folder(**container)

        resp = self.http_client.patchdir(path, pack(container), type, **kwargs)
        self._check_response(resp)
        return unpack(resp.content)

    def patchds(self, path, dataset, versionId="current", site=None, **kwargs):
        """
        Patch a dataset.
        :param path: Path of the dataset to patch.
        :param dataset: A dict object or a dataset.model.Dataset object representing the changes to be applied to the
        dataset
        :param versionId: If specified, identifies the version to patch. Otherwise, it's assumed to patch the current
        version, should it exist.
        :param site: If specified, identifies the specific location to be patched (i.e. SLAC, IN2P3)
        :param kwargs:
        :return: A representation of the patched dataset
        """
        headers = kwargs.get("headers", {})
        headers["content-type"] = "application/json"
        kwargs["headers"] = headers
        ds = dataset if type(dataset) == Dataset else Dataset(**dataset)
        resp = self.http_client.patchds(path, pack(ds), versionId, site, **kwargs)
        self._check_response(resp)
        return unpack(resp.content)

    def patch_container(self, path, container, type="folder", **kwargs):
        return self.patchdir(path, container, type, **kwargs)

    def patch_dataset(self, path, dataset, versionId="current", site=None, **kwargs):
        return self.patchds(path, dataset, versionId, site, **kwargs)

    def search(self, target, versionId=None, site=None, query=None, sort=None, show=None, offset=None, max_num=None,
               **kwargs):
        """Search a target. A target is a Container of some sort. It may also be specified as a glob, as in:
         1. /path/to - target /path/to _only_
         2. /path/to/* - target is all containers directly in /path/to/
         3. /path/to/** - target is all containers, recursively, under /path/to/
         4. /path/to/*$ - target is only folders directly under /path/to/
         5. /path/to/**^ - target is only groups, recursively, under /path/to/

        :param target: The path (or glob-like path) of which to search
        :param query: The query
        :param sort: Fields and Metadata fields to sort on.
        :param show: Metadata fields to optionally return
        :param offset: Offset at which to start returning objects.
        :param max_num: Maximum number of objects to return.
        :param accept: Format of the response object which is returned.
        """
        resp = self.http_client.search(target, versionId, site, query, sort, show, offset, max_num, **kwargs)
        self._check_response(resp)
        return unpack(resp.content)

    def _check_response(self, response, expected_status=None):
        if response.status_code >= 300:
            raise DcException(response)
        if expected_status and response.status_code != expected_status:
            raise DcException()
