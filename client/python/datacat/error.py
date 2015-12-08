from functools import wraps
from requests import RequestException

__author__ = 'bvan'

EXCEPTION_TYPES = "AccessDenied DirectoryNotEmpty FileAlreadyExists " \
    "IllegalArgument NotDirectory NoSuchFile".split(" ")


class DcException(Exception):
    """
    The base exception class for the datacat client (this module)
    when there is a general error interacting with the API.
    """

    def __repr__(self):
        s = ""
        for i in self.__dict__.keys():
            s += "%s : %s\n" % (i, self.__dict__[i])
        return s


class DcRequestException(DcException):
    """
    Base exception class for all http exceptions.
    This class just wraps the thrown requests.exceptions.RequestException
    exception to something a bit simpler and friendly.
    """

    def __init__(self, http_error):
        self.url = http_error.request.url
        response = http_error.response if http_error.response is not None else {}
        self.status_code = getattr(response, "status_code", None)
        self.headers = getattr(response, "headers", None)
        self.content = getattr(response, "content", None)
        super(DcRequestException, self).__init__(http_error.message)

    def __str__(self):
        formatted_string = "(HTTP Error: %s): %s" % (self.status_code, self.content)
        return formatted_string


# noinspection PyShadowingBuiltins
class DcClientException(DcException):
    """
    The base exception class for all datacat returned client exceptions.
    """
    def __init__(self, type, message=None, cause=None, code=None):
        self.type = type
        self.message = message
        self.cause = cause
        self.code = code
        super(DcClientException, self).__init__(str(self))

    def __str__(self):
        formatted_string = "Datacat Exception: (%s): %s" % (self.type, self.message)
        return formatted_string


def checked_error(method):
    @wraps(method)
    def wrapped(*args, **kwargs):
        try:
            return method(*args, accept="json", **kwargs)
        except RequestException as e:
            raise _check(e)
    return wrapped


def _check(request_exception):
    resp = request_exception.response
    if resp is not None and resp.headers.get("content-type", None) == "application/json":
        err = resp.json()
        if "type" in err:
            return DcClientException(err["type"], message=err.get("message", None),
                                     cause=err.get("cause", None), code=err.get("code", None))
    return DcRequestException(request_exception)
