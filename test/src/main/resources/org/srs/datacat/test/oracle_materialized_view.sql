DROP MATERIALIZED VIEW log on DatasetLogicalFolder;
DROP MATERIALIZED VIEW LogicalFolders;

CREATE OR REPLACE FUNCTION dc_acl_of (folder_in NUMBER)
    RETURN varchar2 DETERMINISTIC
IS
    acl varchar2(1024);
    parent number;
BEGIN
select parent, acl into parent, acl from DatasetLogicalFolder where DatasetLogicalFolder = folder_in;
  if parent != 0 and acl is null
    then
      return dc_acl_of(parent);
  end if;
  RETURN acl;
END;

CREATE OR REPLACE FUNCTION dc_path_of (folder_in NUMBER, path_in VARCHAR2)
    RETURN varchar2 DETERMINISTIC
IS
    path varchar2(512);
    parent number;
BEGIN
select parent, '/' || case when folder_in = 0 then '' else name end || path_in into parent, path from DatasetLogicalFolder where DatasetLogicalFolder = folder_in;
  if parent != 0
    then
      return dc_path_of(parent, path);
  end if;
  RETURN path;
END;


CREATE MATERIALIZED VIEW LOG ON DatasetLogicalFolder
   WITH PRIMARY KEY;


CREATE MATERIALIZED VIEW LogicalFolders
   BUILD IMMEDIATE
   REFRESH FAST ON COMMIT
  AS
  SELECT datasetlogicalfolder, 
      name, 
      dc_path_of(datasetlogicalfolder, '') path, 
      dc_acl_of(datasetlogicalfolder) acl 
      FROM DatasetLogicalFolder;

CREATE INDEX MV_LogicalFolders_path_idx on LogicalFolders(path);
