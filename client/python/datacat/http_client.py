
import os
import requests
import urllib
from config import ENDPOINTS, DATATYPES


class HttpClient(object):

    """
    HTTP-level abstraction over the RESTful Client.
    """

    ALLOWABLE_VERSIONS = "curr current latest new next".split(" ")
    ALLOWABLE_SITES = "master canonical all".split(" ")

    def __init__(self, url, auth_strategy=None, *args, **kwargs):
        self.base_url = url
        self.auth_strategy = auth_strategy

    def path(self, path, versionId=None, site=None, accept="json", **kwargs):
        """
        Retrieve a datacat object.
        :param path: Path of the object to retrieve.
        :param versionId: Version ID input for Dataset View.
        :param site: Site input for Dataset View.
        :return: A :class`requests.Response` object. The content is a representation of the newly created container.
        """
        endpoint = "path"
        return self._req("get", self._target(endpoint, path, versionId, site, accept), **kwargs)
    
    def children(self, path, versionId=None, site=None, offset=None, max_num=None, accept="json", **kwargs):
        """
        Retrieve the children of a container.
        :param path: Path of the container to retrieve objects from
        :param versionId: Version ID input for the Dataset View on the individual datasets.
        :param site: Site input for the Dataset View on the individual datasets.
        :return: A :class`requests.Response` object. The content is a representation of the newly created container.
        """
        endpoint = "path"
        param_list = "offset:offset max_num:max".split(" ")
        param_map = dict([i.split(":") for i in param_list])
        params = {param_map[k]:v for k,v in locals().items() if k in param_map and v is not None}
        target = self._target(endpoint, path, versionId, site, accept) + ";children"
        return self._req("get", target, params, **kwargs)

    def mkdir(self, path, payload=None, type="folder", **kwargs):
        """
        Make a new Container
        :param path: Container Target path
        :param type: Container type. Defaults to folder.
        :param parents: If true, will create intermediate Folders as required.
        :param metadata: Metadata to add to when creating folder
        :return: A :class`requests.Response` object. The content is a representation of the newly created container.
        """
        parentpath = os.path.dirname(path)
        endpoint = "folders"
        if type.lower() == "group":
            endpoint = "groups"
        return self._req("post", self._target(endpoint, parentpath), data=payload, **kwargs)

    def mkds(self, path, payload, versionId=None, **kwargs):
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
        :return: A :class`requests.Response` object. The content is a representation of the newly created Dataset.
        """
        endpoint = "datasets"
        return self._req("post", self._target(endpoint, path, versionId, None), data=payload, **kwargs)

    def rmdir(self, path, type="folder", **kwargs):
        """
        Remove a container.
        :param path: Path of container to remove.
        :param type: Type of container (Group or Folder). This will be removed in a future version.
        :return: A :class`requests.Response` object. A client can inspect the status code.
        """
        endpoint = "folders"
        if type.lower() == "group":
            endpoint = "groups"
        return self._req("delete", self._target(endpoint, path), **kwargs)

    def rmds(self, path, **kwargs):
        """
        Remove a dataset.
        :param path: Path of dataset to remove
        :param kwargs:
        :return: A :class`requests.Response` object.
        """
        endpoint = "datasets"
        return self._req("delete", self._target(endpoint, path), **kwargs)

    def patchdir(self, path, payload, type="folder", **kwargs):
        """
        Patch a container.
        :param path: Path of the dataset to patch.
        :param type: Container type. Defaults to folder.
        :param container: A dict object or a dataset.model.Group/Folder object representing the changes to be applied to the
        container.
        :param kwargs:
        :return: A :class`requests.Response` object. The content is a representation of the patched container
        """
        endpoint = "folders"
        if type.lower() == "group":
            endpoint = "groups"
        return self._req("patch", self._target(endpoint, path), data=payload, **kwargs)

    def patchds(self, path, payload, versionId="current", site=None, **kwargs):
        """
        Patch a dataset.
        :param path: Path of the dataset to patch.
        :param dataset: A dict object or a dataset.model.Dataset object representing the changes to be applied to the
        dataset
        :param versionId: If specified, identifies the version to patch. Otherwise, it's assumed to patch the current
        version, should it exist.
        :param site: If specified, identifies the specific location to be patched (i.e. SLAC, IN2P3)
        :param kwargs:
        :return: A :class`requests.Response` object. The content is a representation of the patched dataset
        """
        endpoint = "datasets"
        return self._req("patch", self._target(endpoint, path, versionId, site), data=payload, **kwargs)

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

    def _req(self, http_method, target, params=None, data=None, **kwargs):
        headers = kwargs["headers"] if "headers" in kwargs else None
        requests_method = getattr(requests, http_method)
        resp = requests_method(target, params=params, headers=headers, data=data, auth=self.auth_strategy)
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
        resp.raise_for_status()
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
            return "%s/%s" %(path, urllib.quote(part, safe="/*$"))
        url = resolve(self.base_url, resource(endpoint, accept))
        view = ";v=" + str(version) if version is not None else ""
        view += ";s=" + site if site is not None else ""
        return resolve(url, path) + view
