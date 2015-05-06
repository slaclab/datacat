__author__ = 'bvan'

import base64
from email.utils import formatdate
import hashlib
import hmac
import requests
from urlparse import urlparse


class HMACAuth(requests.auth.AuthBase):
    def __init__(self, key_id, secret_key, header_name, signature_format, url=None):
        """key_id must be base64"""
        if url:
            self.resource_base_url = url
        self.key_id = str(key_id)
        self.secret_key = base64.b64decode(str(secret_key))
        self.header_name = header_name
        self.sig_fmt = signature_format

    def __call__(self, request):
        # Create date header if it is not created yet.
        if 'date' not in request.headers:
            request.headers['date'] = formatdate(timeval=None, localtime=False, usegmt=True)
        request.headers[self.header_name] = self.sig_fmt.format(self.key_id, self.get_signature(request))
        return request

    def get_signature(self, r):
        canonical_string = self.get_canonical_string(r.url, r.headers, r.method)
        h = hmac.new(self.secret_key, canonical_string, digestmod=hashlib.sha1)
        return base64.encodestring(h.digest()).strip()

    def get_canonical_string(self, url, headers, method):
        parsedurl = urlparse(url)
        d_headers = {}
        for key in headers:
            lk = key.lower()
            d_headers[lk] = headers[key]
        # hacky way of doing this...
        if self.resource_base_url:
            rpath = parsedurl.path.replace(urlparse(self.resource_base_url).path,"")
        else:
            rpath = "/" + "".join(parsedurl.path.split("/r/")[1:])
        content_md5 = d_headers['content-md5'] if 'content-md5' in d_headers else ""
        content_type = d_headers['content-type'] if 'content-type' in d_headers else ""
        date = d_headers['date']
        hash_buf = "%s\n%s\n%s\n%s\n%s\n" %(method, rpath, content_md5, content_type, date)
        return hash_buf


"""
TODO: Write tests
class MockRequest(object):
    pass

r = MockRequest()
r.url =  "http://srs.slac.stanford.edu/datacat-v0.2/r" + "/path.json/LSST"
r.headers = {}
r.method = "post"

auth_strategy = HMACAuthSRS(key, secret, "http://srs.slac.stanford.edu/datacat-v0.2/r")
auth_strategy(r)
"""


class HMACAuthSRS(HMACAuth):
    def __init__(self, key_id, secret_key, url=None):
        super(HMACAuthSRS, self).__init__(key_id, secret_key, u"Authorization", u"SRS:{0}:{1}", url)
