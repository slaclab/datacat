import requests
from config import ENDPOINTS, DATATYPES

class Client(object):
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
        endpoint = "search"
        param_list = "query:filter sort:sort show:show offset:offset max_num:max".split(" ")
        param_map = dict([i.split(":") for i in param_list])
        params = {param_map[k]:v for k,v in locals().items() if k in param_map and v is not None}
        return self._get(self._target(endpoint, path, accept), params)
    
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

