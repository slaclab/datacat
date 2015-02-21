__author__ = 'bvan'

import requests
import hmac
import base64
from urlparse import urlparse
from email.utils import formatdate
import hashlib


class HMACAuth(requests.auth.AuthBase):
    def __init__(self, key_id, secret_key, base_url=None):
        """key_id must not be base64"""
        if base_url:
            self.resource_base_url = base_url
        self.key_id = str(key_id)
        self.secret_key = str(secret_key)

    def __call__(self, r):
        # Create date header if it is not created yet.
        if not 'date' in r.headers:
            r.headers['date'] = formatdate(timeval=None, localtime=False, usegmt=True)
        r.headers['Authorization'] = 'SRS:%s:%s'%(self.key_id, self.get_signature(r))
        return r

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
            rpath = parsedurl.path.replace(self.resource_base_url,"")
        else:
            rpath = "/" + "".join(parsedurl.path.split("/r/")[1:])
        content_md5 = d_headers['content-md5'] if 'content-md5' in d_headers else ""
        content_type = d_headers['content-type'] if 'content-type' in d_headers else ""
        date = d_headers['date']
        hash_buf = "%s\n%s\n%s\n%s\n%s\n" %(method,rpath,content_md5, content_type, date)
        return hash_buf


