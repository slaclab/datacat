__author__ = 'bvan'

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
