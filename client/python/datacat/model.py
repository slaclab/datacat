

class DatacatObject(dict):
    def __init__(self, raw):
        self.type = raw["$type"]
        self["$type"] = self.type
        fields = [f for f in raw.keys() if "$" not in f]
        for k,v in self.fix_metadata(raw.copy()).items():
            if k in fields:
                self.__dict__[k] = v
                self[k] = v

    def fix_metadata(self, data_dict):
        if "metadata" in data_dict:
            data_dict["metadata"] = _translate_metadata(data_dict["metadata"])
        return data_dict

class Container(DatacatObject):
    pass

class Folder(Container):
    pass

class Group(Container):
    pass

class Dataset(DatacatObject):
    def fix_metadata(self, data_dict):
        super(Dataset, self).fix_metadata(data_dict)
        if "versionMetadata" in data_dict:
            data_dict["versionMetadata"] = _translate_metadata(data_dict["versionMetadata"])
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

class DatasetVersion(DatacatObject):
    pass

class DatasetLocation(DatacatObject):
    pass

def unpack(raw):
    dctype = raw["$type"]
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

def _translate_metadata(md_list):
    value_mapping = {"integer":long, "decimal":float, "string":str}
    def translate_value(val):
        return value_mapping[val["$type"]](val["value"])
    return {item["key"]:translate_value(item) for item in md_list}
