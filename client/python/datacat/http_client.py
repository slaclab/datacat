
import logging
import os
import requests

try:
    from urllib.parse import quote
except ImportError:
    from urllib import quote

from .config import ENDPOINTS, DATATYPES

_logger = logging.getLogger(__name__)


# noinspection PyPep8Naming,PyShadowingBuiltins,PyUnusedLocal
class HttpClient:

    """
    HTTP-level abstraction over the RESTful Client.
    """

    ALLOWABLE_VERSIONS = "curr current latest new next".split(" ")
    ALLOWABLE_SITES = "master canonical all".split(" ")

    def __init__(self, url, auth_strategy=None, debug=False, **kwargs):
        self.base_url = url
        self.auth_strategy = auth_strategy
        self.debug = debug
        self.accept = kwargs.get("accept", "json")
        self.content_type = kwargs.get("content_type", "application/json")

    def path(self, path, versionId=None, site=None, stat=None, **kwargs):
        """
        Retrieve a datacat object.
        :param path: Path of the object to retrieve.
        :param versionId: Version ID input for Dataset View.
        :param site: Site input for Dataset View.
        :param stat: Type of stat to return with this object.
        :return: A :class`requests.Response` object. The content is a representation of the newly created container.
        """
        endpoint = "path"
        params = {}
        if stat:
            params["stat"] = stat
        return self._req("get", self._target(endpoint, path, versionId, site), params, **kwargs)
    
    def children(self, path, versionId=None, site=None, stat=None, offset=None, max_num=None, **kwargs):
        """
        Retrieve the children of a container.
        :param path: Path of the container to retrieve objects from
        :param versionId: Version ID input for the Dataset View on the individual datasets.
        :param site: Site input for the Dataset View on the individual datasets.
        :param stat: Optional stat parameter for child containers
        :param offset: Offset to start results for paging
        :param max_num: Maximum number of objects to return.
        :return: A :class`requests.Response` object. The content is a representation of the newly created container.
        """
        endpoint = "path"
        param_list = "offset:offset max_num:max stat:stat".split(" ")
        param_map = dict([tuple(i.split(":")) for i in param_list])
        params = {param_map[k]: v for k, v in locals().items() if k in param_map and v is not None}
        target = self._target(endpoint, path, versionId, site) + ";children"
        return self._req("get", target, params, **kwargs)

    def mkdir(self, path, payload=None, type="folder", **kwargs):
        """
        Make a new Container
        :param path: Container Target path
        :param payload: The serialized representation of the container to be created.
        :param type: Container type. Defaults to folder.
        :return: A :class`requests.Response` object. The content is a representation of the newly created container.
        """
        parentpath = os.path.dirname(path)
        endpoint = "folders"
        if type.lower() == "group":
            endpoint = "groups"
        headers = kwargs.setdefault("headers", {})
        headers["Content-Type"] = self.content_type
        return self._req("post", self._target(endpoint, parentpath), data=payload, **kwargs)

    def mkds(self, path, payload, versionId=None, **kwargs):
        """
        Make a dataset.
        :param path: Container Target path
        :param payload: The serialized representation of the dataset to be created.
        :param versionId: Desired versionId. By default, it is set to "new", which will result in a versionId of 0.
        :return: A :class`requests.Response` object. The content is a representation of the newly created Dataset.
        """
        endpoint = "datasets"
        headers = kwargs.setdefault("headers", {})
        headers["Content-Type"] = self.content_type
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
        :param payload: The serialized representation of the container to be patched.
        :param kwargs:
        :return: A :class`requests.Response` object. The content is a representation of the patched container
        """
        endpoint = "folders"
        if type.lower() == "group":
            endpoint = "groups"
        headers = kwargs.setdefault("headers", {})
        headers["Content-Type"] = self.content_type
        return self._req("patch", self._target(endpoint, path), data=payload, **kwargs)

    def patchds(self, path, payload, versionId="current", site=None, **kwargs):
        """
        Patch a dataset.
        :param path: Path of the dataset to patch.
        :param payload: The serialized representation of the dataset to be patched.
        :param versionId: If specified, identifies the version to patch. Otherwise, it's assumed to patch the current
        version, should it exist.
        :param site: If specified, identifies the specific location to be patched (i.e. SLAC, IN2P3)
        :param kwargs:
        :return: A :class`requests.Response` object. The content is a representation of the patched dataset
        """
        endpoint = "datasets"
        headers = kwargs.setdefault("headers", {})
        headers["Content-Type"] = self.content_type
        return self._req("patch", self._target(endpoint, path, versionId, site), data=payload, **kwargs)

    def search(self, target, versionId=None, site=None, query=None, sort=None, show=None, offset=None, max_num=None,
               **kwargs):
        """Search a target. A target is a Container of some sort. It may also be specified as a glob, as in:
         1. /path/to - target /path/to _only_
         2. /path/to/* - target is all containers directly in /path/to/
         3. /path/to/** - target is all containers, recursively, under /path/to/
         4. /path/to/*$ - target is only folders directly under /path/to/
         5. /path/to/**^ - target is only groups, recursively, under /path/to/

        :param target: The path (or glob-like path) of which to search
        :param versionId: Optional versionId filter
        :param site: Optional site filter
        :param query: The query
        :param sort: Fields and Metadata fields to sort on.
        :param show: Metadata fields to optionally return
        :param offset: Offset at which to start returning objects.
        :param max_num: Maximum number of objects to return.
        :return: A :class`requests.Response` object. A user can use Response.content to get the content.
        The object will be a collection.
        """
        endpoint = "search"
        param_list = "query:filter sort:sort show:show offset:offset max_num:max".split(" ")
        param_map = dict([tuple(i.split(":")) for i in param_list])
        params = {param_map[k]: v for k, v in locals().items() if k in param_map and v is not None}
        return self._req("get", self._target(endpoint, target, versionId, site), params, **kwargs)

    def permissions(self, path, group=None):
        """
        Retrieve the effective permissions.
        :param path: Path of the object to retrieve.
        :param group: If specified, this should be a group specification of the format "{name}@{project}"
        :return: A :class`requests.Response` object. The content is a representation of the newly created container.
        """
        endpoint = "permissions"
        params = dict(subject="user")
        if group:
            params["subject"] = "group"
            params["group"] = group
        return self._req("get", self._target(endpoint, path), params)

    def listacl(self, path):
        """
        Retrieve a datacat object.
        :param path: Path of the object to retrieve.
        :return: A :class`requests.Response` object. The content is a representation of the newly created container.
        """
        endpoint = "permissions"
        return self._req("get", self._target(endpoint, path))

    def patchacl(self, path, acl):
        """
        Patch an ACL.
        :param path: Path of the object to patch (must be container(
        :param acl: List of entries defining the group and
        :return: A complete representation of all ACLs
        """
        endpoint = "permissions"
        return self._req("patch", self._target(endpoint, path), data=acl)

    def _log_request(self, request):
        if not self.debug:
            return
        url = request.url
        http_method = request.method
        headers = getattr(request, "headers", None)
        data = getattr(request, "data", None)

        _logger.debug("Request: %s %s" % (http_method, url))
        if headers:
            for header in headers.items():
                _logger.debug("Request Header: %s" % ": ".join(header))

        if data:
            _logger.debug("Entity: %s\n" % data)

    def _log_response(self, response):
        headers = getattr(response, "headers", None)
        if not self.debug:
            return
        _logger.debug("Response: (%s)", response.status_code)

        if headers:
            for header in headers.items():
                _logger.debug("Response Header: %s" % ": ".join(header))

        if response.content:
            _logger.debug("Entity: %s\n", response.content)

    def _req(self, http_method, target, params=None, data=None, **kwargs):
        headers = kwargs.get("headers", {})
        requests_method = getattr(requests, http_method)
        response = requests_method(target, params=params, data=data, headers=headers, auth=self.auth_strategy)
        self._log_request(response.request)
        self._log_response(response)
        # response evaluates to false if it's a 4xx or 5xx
        if not response:
            _logger.debug("HTTP Request returned failure status: %s", response.status_code)
        response.raise_for_status()
        return response

    def _target(self, endpoint, path, version=None, site=None):
        if version is not None:
            try:
                version = int(version)
            except ValueError as e:
                if version.lower() in self.ALLOWABLE_VERSIONS:
                    version = version.lower()
                else:
                    raise e

        def resource(_endpoint, _accept):
            if _endpoint in ENDPOINTS and _accept in DATATYPES:
                return "%s.%s" % (_endpoint, _accept)
            raise ValueError("Unknown endpoint. Check client code")

        def resolve(_path, part):
            _path = _path if _path[-1] != '/' else _path[:-1]
            part = part if part[0] != '/' else (part[1:] if len(part) > 0 else "")
            return "%s/%s" % (_path, quote(part, safe="/*$"))

        url = resolve(self.base_url, resource(endpoint, self.accept))
        view = ";v=" + str(version) if version is not None else ""
        view += ";s=" + site if site is not None else ""
        return resolve(url, path) + view
