CREATE OR REPLACE TRIGGER TRIG_VDSMS_METAINFO
   AFTER INSERT ON VerDatasetMetaString 
   FOR EACH ROW
BEGIN
  INSERT INTO DatasetMetaInfo (MetaName, ValueType) 
  SELECT :new.MetaName, 'S' FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM DatasetMetaInfo d WHERE d.MetaName = :new.MetaName and d.ValueType = 'S');
END;

CREATE OR REPLACE TRIGGER TRIG_VDSMN_METAINFO
   AFTER INSERT ON VerDatasetMetaNumber
   FOR EACH ROW
BEGIN
  INSERT INTO DatasetMetaInfo (MetaName, ValueType) 
  SELECT :new.MetaName, 'N' FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM DatasetMetaInfo d WHERE d.MetaName = :new.MetaName and d.ValueType = 'N');
END;

CREATE OR REPLACE TRIGGER TRIG_VDSMT_METAINFO
   AFTER INSERT ON VerDatasetMetaTimestamp
   FOR EACH ROW
BEGIN
  INSERT INTO DatasetMetaInfo (MetaName, ValueType) 
  SELECT :new.MetaName, 'T' FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM DatasetMetaInfo d WHERE d.MetaName = :new.MetaName and d.ValueType = 'T');
END;

CREATE OR REPLACE TRIGGER TRIG_DSCMS_METAINFO
   AFTER INSERT ON LogicalFolderMetaString 
   FOR EACH ROW
BEGIN
  INSERT INTO ContainerMetaInfo (MetaName, ValueType) 
  SELECT :new.MetaName, 'S' FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM ContainerMetaInfo d WHERE d.MetaName = :new.MetaName and d.ValueType = 'S');
END;

CREATE OR REPLACE TRIGGER TRIG_DSCMN_METAINFO
   AFTER INSERT ON LogicalFolderMetaNumber
   FOR EACH ROW
BEGIN
  INSERT INTO ContainerMetaInfo (MetaName, ValueType) 
  SELECT :new.MetaName, 'N' FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM ContainerMetaInfo d WHERE d.MetaName = :new.MetaName and d.ValueType = 'N');
END;

CREATE OR REPLACE TRIGGER TRIG_DSCMT_METAINFO
   AFTER INSERT ON LogicalFolderMetaTimestamp
   FOR EACH ROW
BEGIN
  INSERT INTO ContainerMetaInfo (MetaName, ValueType) 
  SELECT :new.MetaName, 'T' FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM ContainerMetaInfo d WHERE d.MetaName = :new.MetaName and d.ValueType = 'T');
END;