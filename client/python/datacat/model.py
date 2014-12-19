
import collections
import json

class DatacatObject(object):
    def __init__(self, raw):
        for k,v in raw.items():
            if k != "_type":
                self.__dict__[k] = v

class Container(DatacatObject):
    pass

class Folder(Container):
    pass

class Group(Container):
    pass

class Dataset(DatacatObject):
    REQ_JSON_ALLOWED = "name dataType fileFormat".split(" ")

    def __init__(self, **kwargs):
        super(Dataset, self).__init__(kwargs)
        self.dataType = kwargs.get('dataType', None)
        self.fileFormat = kwargs.get('fileFormat', None)

    def flattened_by_status(self, status="OK"):
        from copy import copy
        ret = copy(self)
        location = ret._show_first_xxx(status)
        if hasattr(ret, 'locations'):
            delattr(ret, 'locations')
        if location is not None:
            for k,v in location.items():
                setattr(self, k, v)
        return ret

    def _show_first_xxx(self, status):
        #if hasattr(self, "location"):
        #if self.location.scanStatus is status:
        #        return self.location
        if hasattr(self, "locations"):
            for location in self.locations:
                if location.scanStatus is status:
                    return location
        return None


class DatasetVersion(DatacatObject):
    REQ_JSON_ALLOWED = "versionId processInstance taskName versionMetadata".split(" ")

    def __init__(self, versionId="new", versionMetadata=None, versionExtras=None, **kwargs):
        if versionId:
            self.versionId = versionId
        if versionMetadata:
            self.versionMetadata = versionMetadata
        if versionExtras:
            for k, v in versionExtras.items():
                setattr(k, v)

class DatasetLocation(DatacatObject):
    REQ_JSON_ALLOWED = "site resource checksum size".split(" ")

    def __init__(self, site=None, resource=None, locationExtras=None, **kwargs):
        if site:
            self.site = site
        if resource:
            self.resource = resource
        if locationExtras:
            for k, v in locationExtras.items():
                setattr(k, v)

class DatasetWithView(Dataset):
    def __init__(self, name, dataType, fileFormat, view, **kwargs):
        super(DatasetWithView, self).__init__(name=name, dataType=dataType, fileFormat=fileFormat, **kwargs)
        self.view = view

class DatasetView:
    def __init__(self, **kwargs):
        if "version" in kwargs:
            self.version = kwargs["version"]
        else:
            self.version = DatasetVersion(**kwargs)
        if 'locations' in kwargs:
            self.locations = [DatasetLocation(**raw_loc) for raw_loc in kwargs['locations']]


class Metadata(collections.MutableMapping):
    def __init__(self, seq=None):
        self.dct = dict(seq) if seq else dict()

    def __getitem__(self, key):
        return self.dct.__getitem__(key)

    def __setitem__(self, key, value):
        self.dct.__setitem__(key, value)

    def __delitem__(self, key):
        return self.dct.__delitem__(key)

    def __iter__(self):
        return self.dct.__iter__()

    def __len__(self):
        return self.dct.__len__()

    def __encode__(self):
        ret = []
        for k,v in self.dct.items():
            ret.append({"key":k, "value": v})
        return ret

def unpack(content):
    return json.loads(content, object_hook=datacat_hook)

def default(obj):
    try:
        if isinstance(obj, DatasetWithView):
            ret = {}
            if obj.version is not None:
                ret.update(default(obj.version))
            if obj.locations is not None:
                if len(obj.locations) == 1:   # Flat
                    ret.update(default(obj.locations[0]))
                elif len(obj.locations) > 1:
                    ret['locations'] = [default(obj.locations)] # Full
            return ret

        if isinstance(obj, DatacatObject):
            ret = {}
            for k,v in obj.__dict__.items():
                if v:
                    ret[k] = v
            return ret
        if isinstance(obj, Metadata):
            ret = obj.__encode__()
            return ret
        iterable = iter(obj)
    except TypeError:
        pass
    else:
        return list(iterable)
    return json.JSONEncoder.default(obj)


def datacat_hook(raw):
    def fix_md(mdname, d):
        if mdname in d:
            d[mdname] = Metadata(d[mdname])
    fix_md("metadata", raw)
    fix_md("versionMetadata", raw)
    if '_type' in raw:
        _type = raw['_type']
    if _type.startswith("dataset"):
        return Dataset(**raw)
    elif _type.startswith("folder"):
        return Folder(raw)
    elif _type.startswith("group"):
        return Group(raw)
    elif _type.startswith("location"):
        return DatasetLocation(raw)
    elif _type.startswith("version"):
        return DatasetVersion(raw)
    elif _type in ("integer","decimal","string"): # Must be metadata k:value pair
      value_mapping = {"integer":long, "decimal":float, "string":unicode}
      return (raw["key"], value_mapping[raw["_type"]](raw["value"]))

def _pack_metadata(md_dict):
    return [{"key":k, "value":v} for k,v in md_dict.items()]

