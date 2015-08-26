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
            s += "%s : %s\n" %(i, self.__dict__[i])
        return s


class DcRequestException(DcException):
    """
    Base exception class for all http exceptions.
    This class just wraps the thrown requests.exceptions.RequestException
    exception to something a bit simpler and friendly.
    """

    def __init__(self, httpError):
        self.url = httpError.request.url
        response = httpError.response or {}
        self.status_code = getattr(response, "status_code", None)
        self.headers = getattr(response, "headers", None)
        self.content = getattr(response, "content", None)
        super(DcRequestException, self).__init__(httpError.message)

    def __str__(self):
        formatted_string = "(HTTP Error: %s): %s" % (self.status_code, self.content)
        return formatted_string


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
        formatted_string = "datacat sent Exception %s with message: %s" % (self.type, self.message)
        return formatted_string


def checkedError(requestException):

    resp = requestException.response
    if resp and resp.headers["content-type"] == "application/json":
        err = resp.json()
        if "type" in err:
            return DcClientException(err["type"], message=err.get("message", None),
                                     cause=err.get("cause", None), code=err.get("code", None))
    return DcRequestException(requestException)
