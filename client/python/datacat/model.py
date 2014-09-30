

class DatacatObject(object):
    def __init__(self, raw):
        self.raw = self.fix_metadata(raw.copy())
        self.fields = [f for f in raw.keys() if "$" not in f]
        for k,v in self.raw.items():
            if k in self.fields:
                self.__dict__[k] = v

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
        return data_dict

def unpack(raw):
    dctype = raw["$type"]
    if dctype.startswith("dataset"):
        return Dataset(raw)
    elif dctype.startswith("folder"):
        return Folder(raw)
    elif dctype.startswith("group"):
        return Group(raw)

def _translate_metadata(md_list):
    value_mapping = {"integer":long, "decimal":float, "string":str}
    def translate_value(val):
        return value_mapping[val["$type"]](val["value"])
    return {item["key"]:translate_value(item) for item in md_list}
