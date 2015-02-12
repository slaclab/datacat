
import os
import requests
import urllib
from config import ENDPOINTS, DATATYPES
from .model import *


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
    HTTP-level abstraction over
    '''

    ALLOWABLE_VERSIONS = "curr current latest new next".split(" ")
    ALLOWABLE_SITES = "master canonical all".split(" ")

    def __init__(self, base_url, *args, **kwargs):
        self.base_url = base_url
    
    def children(self, path, versionId=None, site=None, offset=None, max_num=None, accept="json", **kwargs):
        endpoint = "path"
        param_list = "offset:offset max_num:max".split(" ")
        param_map = dict([i.split(":") for i in param_list])
        params = {param_map[k]:v for k,v in locals().items() if k in param_map and v is not None}
        target = self._target(endpoint, path, versionId, site, accept) + ";children"
        return self._req("get", target, params, **kwargs)
    
    def path(self, path, versionId=None, site=None, accept="json", **kwargs):
        endpoint = "path"
        return self._req("get", self._target(endpoint, path, versionId, site, accept), **kwargs)

    def exists(self, path, versionId=None, site=None, **kwargs):
        endpoint = "path"
        try:
            resp = self._req("get", self._target(endpoint, path, versionId, site), accept="json", **kwargs)
            return True
        except DcException as e:
            if e.raw.status_code > 404 or e.raw.status_code == 400:
                raise e
        return False

    def mkfolder(self, path, parents=False, metadata=None, folderExtras=None, **kwargs):
        """
        Make a new Folder
        :param path: Container Target path
        :param parents: If true, will create intermediate Folders as required.
        :param metadata: Metadata to add to when creating folder
        :return: A :class`requests.Response` object. A user can use Response.content to get the content.
        The object will be a Folder
        """
        return self.mkdir(path, type="folder", parents=parents, metadata=metadata, extras=folderExtras, **kwargs)

    def mkgroup(self, path, parents=False, metadata=None, groupExtras=None, **kwargs):
        """
        Make a new Folder
        :param path: Container Target path
        :param parents: If true, will create intermediate Folders as required.
        :param metadata: Metadata to add to when creating folder
        :return: A :class`requests.Response` object. A user can use Response.content to get the content.
        The object will be a Folder
        """
        return self.mkdir(path, type="group", parents=parents, metadata=metadata, extras=groupExtras, **kwargs)

    def mkdir(self, path, type="folder", parents=False, metadata=None, extras=None, **kwargs):
        """
        Make a new Container
        :param path: Container Target path
        :param type: Container type. Defaults to folder.
        :param parents: If true, will create intermediate Folders as required.
        :param metadata: Metadata to add to when creating folder
        :return: A :class`requests.Response` object. A user can use Response.content to get the content.
        The object will be a Folder
        """
        if type.lower() == "folder":
            endpoint = "folders"
            container = Folder(path=path, name=path.split("/")[-1], metadata=metadata)
        elif type.lower() == "group":
            endpoint = "groups"
            container = Group(path=path, name=path.split("/")[-1], metadata=metadata)
        headers = kwargs.get("headers", {})
        headers["content-type"] = "application/json"
        kwargs["headers"] = headers
        if parents:
            parentpath = os.path.dirname(path)
            parts = []
            while not self.exists(parentpath):
                parts.append(os.path.split(parentpath)[1])
                parentpath = os.path.dirname(path)
            if len(parts):
                for part in parts:
                    parentpath = os.path.join(parentpath, parts.pop())
                    self.mkdir(parentpath)
        return self._req("post",self._target(endpoint, path), data=pack(container), **kwargs)

    def rmdir(self, path, type="folder", **kwargs):
        """
        Remove a container.
        :param path: Path of container to remove.
        :param type: Type of container (Group or Folder). This will be removed in a future version.
        :return: A :class`requests.Response` object. A client can inspect the status code.
        """
        if type.lower() == "container":
            endpoint = "folders"
        elif type.lower() == "group":
            endpoint = "groups"
        return self._req("delete",self._target(endpoint, path), **kwargs)

    def mkds(self, path, name, dataType, fileFormat, versionId="new", site=None, versionMetadata=None,
             resource=None, **kwargs):
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
        :return: A :class`requests.Response` object. A user can use Response.content to get the content.
        The object will be a Dataset.
        """
        return self.create_dataset(path, name, dataType, fileFormat, versionId, site, versionMetadata, resource)

    def rmds(self, path, **kwargs):
        """
        Remove a dataset.
        :param path: Path of dataset to remove
        :param kwargs:
        :return: A :class`requests.Response` object. A client can inspect the status code.
        """
        return self.delete_dataset(path, **kwargs)

    def search(self, target, versionId=None, site=None, query=None, sort=None, show=None, offset=None, max_num=None,
               accept="json", **kwargs):
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
        :return: A :class`requests.Response` object. A user can use Response.content to get the content.
        The object will be a collection.
        """
        endpoint = "search"
        param_list = "query:filter sort:sort show:show offset:offset max_num:max".split(" ")
        param_map = dict([i.split(":") for i in param_list])
        params = {param_map[k]:v for k,v in locals().items() if k in param_map and v is not None}
        return self._req("get",self._target(endpoint, target, versionId, site, accept), params, **kwargs)

    def create_dataset(self, path, name, dataType, fileFormat,
                       versionId="new", site=None, versionMetadata=None, resource=None,
                       datasetExtras=None, versionExtras=None, locationExtras=None,
                       **kwargs):
        """
        :param path: Container Target path
        :param name: Name of Dataset you wish to create
        :param dataType: User-Defined Data Type of Dataset. This is often a subtype of a file format.
        :param fileFormat: The File Format of the Dataset (i.e. root, fits, tar.gz, txt, etc...)
        :param versionId: Desired versionId. By default, it is set to "new", which will result in a versionId of 0.
        :param site: Site where the dataset physically resides (i.e. SLAC, IN2P3)
        :param versionMetadata: Metadata to add to registered version if registering a version.
        :param resource: The actual file resource path at the given site (i.e. /nfs/farm/g/glast/dataset.dat)
        :return: A :class`requests.Response` object. A user can use Response.content to get the content.
        The object will be a Dataset.
        """
        endpoint = "datasets"
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
        return self._req("post",self._target(endpoint, path), data=pack(ds), **kwargs)

    def patch_dataset(self, path, dataset, versionId="current", site=None, **kwargs):
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
        endpoint = "datasets"
        headers = kwargs.get("headers", {})
        headers["content-type"] = "application/json"
        kwargs["headers"] = headers
        req = dataset if type(dataset) == Dataset else Dataset(**dataset)
        return self._req("patch",self._target(endpoint, path, versionId, site),
                         data=pack(req), **kwargs)

    def patch_container(self, path, container, type="folder", **kwargs):
        """
        Patch a container.
        :param path: Path of the dataset to patch.
        :param type: Container type. Defaults to folder.
        :param container: A dict object or a dataset.model.Group/Folder object representing the changes to be applied to the
        container.
        :param kwargs:
        :return: A representation of the patched dataset
        """
        if type.lower() == "folder":
            endpoint = "folders"
        elif type.lower() == "group":
            endpoint = "groups"
        headers = kwargs.get("headers", {})
        headers["content-type"] = "application/json"
        kwargs["headers"] = headers
        if isinstance(container, Container):
            req = container
        elif type == "group":
            req = Group(**container)
        elif type == "folder":
            req = Folder(**container)
        return self._req("patch",self._target(endpoint, path), data=pack(req), **kwargs)

    def delete_dataset(self, path, **kwargs):
        endpoint = "datasets"
        return self._req("delete",self._target(endpoint, path), **kwargs)

    def _req(self, http_method, target, params=None, data=None, **kwargs):
        headers = kwargs["headers"] if "headers" in kwargs else None
        requests_method = getattr(requests, http_method)
        resp = requests_method(target, params=params, headers=headers, data=data)
        if kwargs.get('show_request', False):
            print("Request")
            if data:
                print("Data:\n" + str(data))
            print(resp.request.url)
        if kwargs.get('request_headers', False):
            print("Request Headers:")
            print(resp.request.headers)
        if kwargs.get('show_response', False):
            print("Response: %d" %resp.status_code)
        if kwargs.get('response_headers', False):
            print("Response Headers:")
            print(resp.headers)
        if kwargs.get('show_raw_response', False):
            print(kwargs["show_raw_response"])
            print("Response:")
            print(resp.content)
        if resp.status_code >= 300:
            raise DcException(resp)
        return resp

    def _target(self, endpoint, path, version=None, site=None, accept="json"):
        if version is not None:
            try:
                version = int(version)
            except ValueError as e:
                if version.lower() in self.ALLOWABLE_VERSIONS:
                    version = version.lower()
                else:
                    raise e

        def resource(endpoint, accept="json"):
            if endpoint in ENDPOINTS and accept in DATATYPES:
                return "%s.%s" %(endpoint, accept)
        def resolve(path, part):
            path = path if path[-1] != '/' else path[:-1]
            part = part if part[0] != '/' else (part[1:] if len(part) > 0 else "")
            return "%s/%s" %(path, urllib.quote(part))
        url = resolve(self.base_url, resource(endpoint, accept))
        view = ";v=" + str(version) if version is not None else ""
        view += ";s=" + site if site is not None else ""
        return resolve(url, path) + view

