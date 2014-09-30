import requests
from config import ENDPOINTS, DATATYPES


class Client(object):
    """

    """

    def __init__(self, base_url, *args, **kwargs):
        self.base_url = base_url
    
    def children(self, path, offset=None, max_num=None, accept="json", **kwargs):
        endpoint = "children"
        param_list = "offset:offset max_num:max".split(" ")
        param_map = dict([i.split(":") for i in param_list])
        params = {param_map[k]:v for k,v in locals().items() if k in param_map and v is not None}
        return self._get(self._target(endpoint, path, accept), params)
    
    def path(self, path, accept="json", **kwargs):
        endpoint = "path"
        return self._get(self._target(endpoint, path, accept))
    
    def search(self, target, query=None, sort=None, show=None, offset=None, max_num=None, accept="json", **kwargs):
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
        :param max_num: Maxmimum number of objects to return.
        :param accept: Format of the response object which is returned.
        :param kwargs: All are ignored.
        :return: A :class`requests.Response` object. A user can use Response.content to get the content, and optionally
        response.json(), if :accept was json, and get a python dictionary back. The object will be a collection.
        """
        endpoint = "search"
        param_list = "query:filter sort:sort show:show offset:offset max_num:max".split(" ")
        param_map = dict([i.split(":") for i in param_list])
        params = {param_map[k]:v for k,v in locals().items() if k in param_map and v is not None}
        return self._get(self._target(endpoint, target, accept), params)
    
    def _get(self, target, params=None):
        return requests.get(target, params=params)
        
    def _target(self, endpoint, path, accept="json"):
        def rsrc(endpoint, accept="json"):
            if endpoint in ENDPOINTS and accept in DATATYPES:
                return "%s.%s" %(endpoint, accept)
        def rslv(path, part):
            path = path if path[-1] != '/' else path[:-1]
            part = part if part[0] != '/' else (part[1:] if len(part) > 0 else "")
            return "%s/%s" %(path, part)
        url = rslv(self.base_url, rsrc(endpoint, accept))
        return rslv(url, path)

