

class DatacatObject(dict):
    def __init__(self, raw):
        self.type = raw["_type"]
        self["_type"] = self.type
        fields = [f for f in raw.keys() if "$" not in f]
        for k,v in self.fix_metadata(raw.copy()).items():
            if k in fields:
                self.__dict__[k] = v
                self[k] = v

    def fix_metadata(self, data_dict):
        if "metadata" in data_dict:
            data_dict["metadata"] = _unpack_metadata(data_dict["metadata"])
        return data_dict

class Container(DatacatObject):
    pass

class Folder(Container):
    pass

class Group(Container):
    pass

class Dataset(DatacatObject):
    REQ_JSON_ALLOWED = "name dataType fileFormat".split(" ")

    def __init__(self, name, dataType, fileFormat):
        self.name = name
        self.dataType = dataType
        self.fileFormat = fileFormat

    def fix_metadata(self, data_dict):
        super(Dataset, self).fix_metadata(data_dict)
        if "versionMetadata" in data_dict:
            data_dict["versionMetadata"] = _unpack_metadata(data_dict["versionMetadata"])
        if "locations" in data_dict:
            locations = [unpack(loc) for loc in data_dict["locations"]]
            data_dict["locations"] = locations
        return data_dict

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

    def pack(self):
        ret = {}
        for name in self.REQ_JSON_ALLOWED:
            if name in self:
                ret[name] = self[name]
        return ret

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

    def pack(self):
        ret = {}
        for name in self.REQ_JSON_ALLOWED:
            if name in self:

                ret[name] = self[name]
        return ret


class DatasetLocation(DatacatObject):
    REQ_JSON_ALLOWED = "site resource".split(" ")

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
        super(DatasetWithView, self).__init__(name, dataType,fileFormat, **kwargs)
        self.view = view

    def pack(self):
        ret = super(DatasetWithView, self).pack()

        if self.view.locations is not None:
            if len(self.view.locations) == 1:   # Flat
                loc = self.view.locations[0]
                for name in DatasetLocation.REQ_JSON_ALLOWED:
                    if name in loc:
                        ret[name] = loc[name]
            elif len(self.view.locations) > 1:
                ret['locations'] = self.view.locations # Full
        return ret

class DatasetView:
    def __init__(self, version=None, locations=None):
        self.version = version
        self.locations = locations

def unpack(raw):
    dctype = raw["_type"]
    if dctype.startswith("dataset"):
        return Dataset(raw)
    elif dctype.startswith("folder"):
        return Folder(raw)
    elif dctype.startswith("group"):
        return Group(raw)
    elif dctype.startswith("location"):
        return DatasetLocation(raw)
    elif dctype.startswith("version"):
        return DatasetVersion(raw)

def _pack_metadata(md_dict):
    return [{"key":k, "value":v} for k,v in md_dict.items()]

def _unpack_metadata(md_list):
    value_mapping = {"integer":long, "decimal":float, "string":str}
    def translate_value(val):
        return value_mapping[val["type"]](val["value"])
    return {item["key"]:translate_value(item) for item in md_list}
