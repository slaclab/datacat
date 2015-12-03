CREATE OR REPLACE TRIGGER TRIG_VDSMS_METAINFO
   AFTER INSERT ON VerDatasetMetaString 
   FOR EACH ROW
BEGIN
   IF NOT EXISTS 
     (SELECT 1 FROM DatasetMetaInfo d WHERE d.MetaName = :new.MetaName and d.ValueType = 'S')
     THEN 
     INSERT INTO DatasetMetaInfo (MetaName, ValueType) VALUES (:new.MetaName, 'S');
   END IF;
END;

CREATE OR REPLACE TRIGGER TRIG_VDSMN_METAINFO
   AFTER INSERT ON VerDatasetMetaNumber
   FOR EACH ROW
BEGIN
   IF NOT EXISTS 
     (SELECT 1 FROM DatasetMetaInfo d WHERE d.MetaName = :new.MetaName and d.ValueType = 'N')
     THEN 
     INSERT INTO DatasetMetaInfo (MetaName, ValueType) VALUES (:new.MetaName, 'N');
   END IF;
END;

CREATE OR REPLACE TRIGGER TRIG_VDSMT_METAINFO
   AFTER INSERT ON VerDatasetMetaTimestamp
   FOR EACH ROW
BEGIN
   IF NOT EXISTS 
     (SELECT 1 FROM DatasetMetaInfo d WHERE d.MetaName = :new.MetaName and d.ValueType = 'T')
     THEN 
     INSERT INTO DatasetMetaInfo (MetaName, ValueType) VALUES (:new.MetaName, 'T');
   END IF;
END;
