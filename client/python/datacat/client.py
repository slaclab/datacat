import requests
from config import ENDPOINTS, DATATYPES
from .model import *


class DcException(Exception):
    def __init__(self, resp):
        try:
            err = resp.json()
            for k, v in err.items():
                setattr(self, k, v)
        except Exception:
            self.content = resp.content


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
        :return: A :class`requests.Response` object. A user can use Response.content to get the content, and optionally
        response.json(), if :accept was json, and get a python dictionary back. The object will be a collection.
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
        ds = Dataset(name=name, dataType=dataType, fileFormat=fileFormat, locations=[location], **version)
        payload = pack(ds)
        headers = kwargs.get("headers", {})
        headers["content-type"] = "application/json"
        kwargs["headers"] = headers
        return self._req("post",self._target(endpoint, path), data=json.dumps(payload), **kwargs)

    def patch_dataset(self, path, versionId="current", site=None, versionMetadata=None, resource=None,
                       datasetExtras=None, versionExtras=None, locationExtras=None,
                       **kwargs):
        has_version = versionId is not None
        has_location = site is not None and resource is not None
        if not has_version and has_location:
            versionId = "new"
        version = None
        location = None
        if has_version:
            version = {"versionMetadata":versionMetadata}
            if versionExtras:
                version.update(versionExtras)
        if has_location:
            location = {"site":site, "resource":resource}
            if locationExtras:
                location.update(locationExtras)
        ds = Dataset(locations=[location], **version)
        return self.patch_dataset(path, ds, versionId, **kwargs)

    def patch_dataset(self, path, dataset, versionId="current", site=None, **kwargs):
        endpoint = "datasets"
        headers = kwargs.get("headers", {})
        headers["content-type"] = "application/json"
        kwargs["headers"] = headers
        return self._req("patch",self._target(endpoint, path, versionId, site),
                         data=pack(dataset), **kwargs)

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
            return "%s/%s" %(path, part)
        url = resolve(self.base_url, resource(endpoint, accept))
        view = ";v=" + str(version) if version is not None else ""
        view += ";s=" + site if site is not None else ""
        return resolve(url, path) + view

