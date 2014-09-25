SET DATABASE SQL SYNTAX ORA true;

drop table      ContainerSearch;

drop table	VerDatasetMetaString ;
drop table	VerDatasetMetaNumber ;
drop table	VerDatasetMetaTimestamp ;
drop table   	VerDatasetMetaRoot ;
drop table	DatasetGroupMetaString ;
drop table	DatasetGroupMetaNumber ;
drop table	DatasetGroupMetaTimestamp ;
drop table	LogicalFolderMetaString ;
drop table	LogicalFolderMetaNumber ;
drop table	LogicalFolderMetaTimestamp ;
drop table      DatasetMetaname ;
drop table      DatasetGroupMetaname ;
drop table      LogicalFolderMetaname ;
drop table      DatasetLocationPurge ;

alter table DatasetVersion drop constraint FK_DSV_MasterLocation;
drop table  	VerDatasetLocation ;
drop table  	DatasetSite ;

alter table VerDataset drop constraint FK_VDS_LatestVersion;
drop table 	DatasetVersion ;
drop table  	VerDataset ;
drop table 	DatasetGroup ;
drop table 	DatasetLogicalFolder ;
drop table 	DatasetSource ;
drop table 	DatasetDataType ;
drop table 	DatasetFileFormat ;

DROP SEQUENCE Dataset_Seq;
CREATE SEQUENCE Dataset_Seq START WITH 1 INCREMENT BY 1;

create table DatasetFileFormat (
	DatasetFileFormat	varchar(20),
	Description		varchar(400),
	MimeType		varchar(50),
	constraint PK_DatasetFileFormat primary key (DatasetFileFormat)
);

create table DatasetSite (
	DatasetSite	varchar(20),
	constraint PK_DatasetSite primary key (DatasetSite)
);

create table DatasetDataType (
	DatasetDataType		varchar(32),
	Description		varchar(400),
	CrawlerPriority		number,
	constraint PK_DatasetDataType primary key (DatasetDataType)
);


create table DatasetSource (
	DatasetSource		varchar(20),
	constraint PK_DatasetSource primary key (DatasetSource)
);


create table DatasetLogicalFolder (
	DatasetLogicalFolder	integer GENERATED BY DEFAULT AS SEQUENCE dataset_seq,
	Name			varchar(50),
	Parent			integer,
	Description		varchar(400),
	constraint PK_DatasetLogicalFolder primary key (DatasetLogicalFolder),
	constraint FK_DatasetLogicalFolderParent foreign key (parent)
		references DatasetLogicalFolder (DatasetLogicalFolder)
		on delete cascade,
	constraint UNQ_DatasetLogicalFolder UNIQUE(Name, Parent)
);
create index IDX_DatasetLogicalFolderParent on DatasetLogicalFolder(Parent);


create table DatasetGroup (
	DatasetGroup		integer GENERATED BY DEFAULT AS SEQUENCE dataset_seq,
	Name			varchar(50),
	DatasetLogicalFolder	integer not null,
	CreateDate		timestamp,
	ModifyDate		timestamp,
	RunMin			number,
	RunMax			number,
	NumberEvents		number,
	DiskSizeBytes		number,
	Description		varchar(400),
	constraint PK_DatasetGroup primary key (DatasetGroup),
	constraint FK_DSG_DSLF foreign key (DatasetLogicalFolder)
		references DatasetLogicalFolder (DatasetLogicalFolder)
		on delete cascade,
	constraint UNQ_DatasetGroup UNIQUE(Name, DatasetLogicalFolder)
);
create index IDX_DSG_DSLF on DatasetGroup(DatasetLogicalFolder);


create table VerDataset (
	Dataset			integer GENERATED BY DEFAULT AS SEQUENCE dataset_seq constraint PK_VerDataset primary key,
	DatasetName		varchar(50) not null,
	DatasetFileFormat	varchar(20) not null,
	DatasetDataType		varchar(32) not null,
	DatasetLogicalFolder	number,
	DatasetGroup		number,
	LatestVersion		number,
	Registered		timestamp DEFAULT CURRENT_TIMESTAMP,
	constraint FK_VDS_DSDataType foreign key (DatasetDataType)
		references DatasetDataType (DatasetDataType),
	constraint FK_VDS_DSFileFormat foreign key (DatasetFileFormat)
		references DatasetFileFormat (DatasetFileFormat),
        constraint FK_VDS_DSLF foreign key (DatasetLogicalFolder)
		references DatasetLogicalFolder (DatasetLogicalFolder)
		on delete cascade,
        constraint FK_VDS_DSG foreign key (DatasetGroup)
		references DatasetGroup (DatasetGroup)
		on delete cascade,
	constraint VAL_VDS_XOR_NULL_DSLF_DSG check ((DatasetLogicalFolder IS NOT null OR DatasetGroup IS NOT null) AND (DatasetLogicalFolder IS null OR DatasetGroup IS null)),
	constraint UNQ_VDS_DataCatPath unique (DatasetName, DatasetLogicalFolder, DatasetGroup)
);
create index IDX_VDS_DSName on VerDataset(DatasetName);
create index IDX_FK_VDS_DSFileFormat on VerDataset(DatasetFileFormat);
create index IDX_FK_VDS_DSDataType on VerDataset(DatasetDataType);
create index IDX_FK_VDS_DSLF on VerDataset(DatasetLogicalFolder);
create index IDX_FK_VDS_DSG on VerDataset(DatasetGroup);
create index IDX_FK_VDS_LatestVersion on VerDataset(LatestVersion);

create table DatasetVersion (
	DatasetVersion		integer GENERATED BY DEFAULT AS SEQUENCE dataset_seq constraint PK_DatasetVersion primary key,
	Dataset			integer not null,
	VersionID		integer DEFAULT 0,
	DatasetSource		varchar(20) not null,
	ProcessInstance		integer,
	TaskName		varchar(30),
	MasterLocation		integer,
	Registered		timestamp DEFAULT CURRENT_TIMESTAMP,
	constraint FK_DSV_Dataset foreign key (Dataset) 
		references VerDataset (Dataset)
		on delete cascade,
	constraint FK_DSV_DSSource foreign key (DatasetSource)
		references DatasetSource (DatasetSource),
--	constraint FK_DSV_ProcessInstance foreign key (ProcessInstance)
--		references ProcessInstance (ProcessInstance)
--		on delete set null,
	constraint UNQ_DSV_VID_and_Dataset unique (Dataset, VersionID)
);
-- we had to create the DatasetVersion table before we could add the following foreign key to the Dataset table:
alter table VerDataset add constraint FK_VDS_LatestVersion foreign key (LatestVersion) 
	references DatasetVersion (DatasetVersion) 
	on delete set null;

create index IDX_DSV_VersionID on DatasetVersion(VersionID);
create index IDX_FK_DSV_Dataset on DatasetVersion(Dataset);
create index IDX_FK_DSV_DSSource on DatasetVersion(DatasetSource);
create index IDX_FK_DSV_ProcessInstance on DatasetVersion(ProcessInstance);
create index IDX_FK_DSV_MasterLocation on DatasetVersion(MasterLocation);

create index IDX_DSV_DS_and_MasterLoc on DatasetVersion(Dataset, MasterLocation);

create table VerDatasetLocation (
	DatasetLocation		integer GENERATED BY DEFAULT AS SEQUENCE dataset_seq constraint PK_VerDatasetLocation primary key,
	DatasetVersion		integer not null,
	DatasetSite		varchar(20) not null,
	Path			varchar(256) not null,
        RunMin			number,
	RunMax			number,
	NumberEvents		number,
	FileSizeBytes		number,
	CheckSum		number,
	LastModified		timestamp,
	LastScanned 		timestamp,
	ScanStatus 		varchar(20) DEFAULT 'UNSCANNED',
	Registered		timestamp DEFAULT CURRENT_TIMESTAMP,
	constraint FK_VDSL_DSVersion foreign key (DatasetVersion)
		references DatasetVersion (DatasetVersion)
		on delete cascade,
	constraint FK_VDSL_DSSite foreign key (DatasetSite)
		references DatasetSite (DatasetSite),
	constraint UNQ_VDSL_DSVersion_DSSite unique (DatasetVersion, DatasetSite),
	constraint UNQ_VDSL_DSSite_Path unique  (DatasetSite, Path)
);
-- we had to create the VerDatasetLocation table before we could add the following foreign key to the DatasetVersion table:
alter table DatasetVersion add constraint FK_DSV_MasterLocation foreign key (MasterLocation) references VerDatasetLocation (DatasetLocation) on delete set null;

create index IDX_VDSL_Path on VerDatasetLocation(Path);
create index IDX_VDSL_RunMin on VerDatasetLocation(RunMin);
create index IDX_VDSL_RunMax on VerDatasetLocation(RunMax);
create index IDX_VDSL_LastScanned on VerDatasetLocation (LastScanned);
create index IDX_FK_VDSL_DatasetVersion on VerDatasetLocation (DatasetVersion);
create index IDX_FK_VDSL_DatasetSite on VerDatasetLocation (DatasetSite);
create index IDX_VDSL_ScanStatus_Site on VerDatasetLocation (ScanStatus,DatasetSite);
create index IDX_VDSL_ScanStatus_LastScan on VerDatasetLocation(ScanStatus, LastScanned);


create table VerDatasetMetaString (
	DatasetVersion		integer not null,
	MetaName		varchar(20) not null,
	MetaValue		varchar(256),
	constraint FK_VDSMS_DSVersion foreign key (DatasetVersion)
		references DatasetVersion (DatasetVersion)
		on delete cascade,
	constraint UNQ_VDSMS unique (DatasetVersion, MetaName)
);
create index IDX_FK_VDSMS_DSVersion on VerDatasetMetaString(DatasetVersion);
create index IDX_VDSMS_NameValue on VerDatasetMetaString(MetaName, MetaValue);

create table VerDatasetMetaNumber (
	DatasetVersion		integer not null,
	MetaName		varchar(20) not null,
	MetaValue		numeric,
	constraint FK_VDSMN_DSVersion foreign key (DatasetVersion)
		references DatasetVersion (DatasetVersion)
		on delete cascade,
	constraint UNQ_VDSMN unique (DatasetVersion, MetaName)
);
create index IDX_FK_VDSMN_DSVersion on VerDatasetMetaNumber(DatasetVersion);
create index IDX_VDSMN_NameValue on VerDatasetMetaNumber(MetaName, MetaValue);

create table VerDatasetMetaTimestamp (
	DatasetVersion		integer not null,
	MetaName		varchar(20) not null,
	MetaValue		timestamp,
	constraint FK_VDSMT_DSVersion foreign key (DatasetVersion)
		references DatasetVersion (DatasetVersion)
		on delete cascade,
	constraint UNQ_VDSMT unique (DatasetVersion, MetaName)
);
create index IDX_FK_VDSMT_DSVersion on VerDatasetMetaTimestamp(DatasetVersion);
create index IDX_VDSMT_NameValue on VerDatasetMetaTimestamp(MetaName, MetaValue);

create table DatasetMetaName (
     MetaName      varchar(20),
--      MetaType      varchar(1),
     constraint    UNQ_DatasetMetaName unique (MetaName)
);

create table DatasetMetaInfo (
     MetaName      varchar(20),
     ValueType      varchar(1),
     constraint    UNQ_DatasetMetaName unique (MetaName, ValueType)
);

create table VerDatasetMetaRoot (
	DatasetVersion		integer,
	RootVersion		varchar(10),
	SoLibVersion		varchar(50),
	TTreeName		varchar(50),
	constraint FK_VDSMRoot_DSVersion foreign key (DatasetVersion)
		references DatasetVersion (DatasetVersion)
		on delete cascade,
	constraint UNQ_VDSMRoot unique (DatasetVersion)
);
--create index IDX_FK_VDSMRoot_DSVersion on VerDatasetMetaRoot(DatasetVersion);
create index IDX_VDSMRoot_RootVersion on VerDatasetMetaRoot(RootVersion);
create index IDX_VDSMRoot_SoLibVersion on VerDatasetMetaRoot(SoLibVersion);
create index IDX_VDSMRoot_TTreeName on VerDatasetMetaRoot(TTreeName);


--REM DatasetGroup Meta Data Tables

create table DatasetGroupMetaString (
	DatasetGroup		integer not null,
	MetaName		varchar(20) not null,
	MetaValue		varchar(256),
	constraint FK_DSGMS_DatasetGroup foreign key (DatasetGroup)
		references DatasetGroup (DatasetGroup)
		on delete cascade,
	constraint UNQ_DSGMS unique (DatasetGroup, MetaName)
);
create index IDX_DSGMS_DatasetGroup on DatasetGroupMetaString (DatasetGroup);
create index IDX_DSGMS_MetaName on DatasetGroupMetaString (MetaName);


create table DatasetGroupMetaNumber (
	DatasetGroup		integer not null,
	MetaName		varchar(20) not null,
	MetaValue		numeric,
	constraint FK_DSGMN_DatasetGroup foreign key (DatasetGroup)
		references DatasetGroup (DatasetGroup)
		on delete cascade,
	constraint UNQ_DSGMN unique (DatasetGroup, MetaName)
);
create index IDX_DSGMN_DatasetGroup on DatasetGroupMetaNumber (DatasetGroup);
create index IDX_DSGMN_MetaName on DatasetGroupMetaNumber (MetaName);


create table DatasetGroupMetaTimestamp (
	DatasetGroup		integer not null,
	MetaName		varchar(20) not null,
	MetaValue		timestamp,
	constraint FK_DSGMT_DatasetGroup foreign key (DatasetGroup)
		references DatasetGroup (DatasetGroup)
		on delete cascade,
	constraint UNQ_DSGMT unique (DatasetGroup, MetaName)
);
create index IDX_DSGMT_DatasetGroup on DatasetGroupMetaTimestamp (DatasetGroup);
create index IDX_DSGMT_MetaName on DatasetGroupMetaTimestamp (MetaName);


create table DatasetGroupMetaName (
     MetaName      varchar(20),
     constraint    UNQ_DatasetGroupMetaName unique (MetaName)
);



--REM LogicalFolder Meta Data Tables

create table LogicalFolderMetaString (
	LogicalFolder		integer not null,
	MetaName		varchar(20) not null,
	MetaValue		varchar(256),
	constraint FK_LFMS_LogicalFolder foreign key (LogicalFolder)
		references DatasetLogicalFolder (DatasetLogicalFolder)
		on delete cascade,
	constraint UNQ_LFMS unique (LogicalFolder, MetaName)
);
create index IDX_LFMS_LogicalFolder on LogicalFolderMetaString (LogicalFolder);
create index IDX_LFMS_MetaName on LogicalFolderMetaString (MetaName);


create table LogicalFolderMetaNumber (
	LogicalFolder		integer not null,
	MetaName		varchar(20) not null,
	MetaValue		numeric,
	constraint FK_LFMN_LogicalFolder foreign key (LogicalFolder)
		references DatasetLogicalFolder (DatasetLogicalFolder)
		on delete cascade,
	constraint UNQ_LFMN unique (LogicalFolder, MetaName)
);
create index IDX_LFMN_LogicalFolder on LogicalFolderMetaNumber (LogicalFolder);
create index IDX_LFMN_MetaName on LogicalFolderMetaNumber (MetaName);


create table LogicalFolderMetaTimestamp (
	LogicalFolder		integer not null,
	MetaName		varchar(20) not null,
	MetaValue		timestamp,
	constraint FK_LFMT_LogicalFolder foreign key (LogicalFolder)
		references DatasetLogicalFolder (DatasetLogicalFolder)
		on delete cascade,
	constraint UNQ_LFMT unique (LogicalFolder, MetaName)
);
create index IDX_LFMT_LogicalFolder on LogicalFolderMetaTimestamp (LogicalFolder);
create index IDX_LFMT_MetaName on LogicalFolderMetaTimestamp (MetaName);

create table LogicalFolderMetaName (
     MetaName      varchar(20),
     constraint    UNQ_LogicalFolderMetaName unique (MetaName)
);


create table DatasetLocationPurge (
	Purged			timestamp default CURRENT_TIMESTAMP,
	Dataset			number,
	DatasetName		varchar(50) NOT NULL,
	DatasetFileFormat	varchar(20) NOT NULL,
	DatasetDataType		varchar(32) NOT NULL,
	DataCatFolderPath	varchar(2000) NOT NULL,
	DataCatGroupName	varchar(32),
	DatasetVersion		number,
	VersionID		number,
	DatasetSource		varchar(20) NOT NULL,
	ProcessInstance		number,
	TaskName		varchar(30),
	WasLatestVersion	integer NOT NULL,
	DatasetLocation		number PRIMARY KEY,
	DatasetSite		varchar(20) NOT NULL,
	Path			varchar(2000) NOT NULL,
	WasMasterLocation	integer NOT NULL
);
create index idx_DSLP_PurgeTime on DatasetLocationPurge (Purged);

insert
  into DatasetLogicalFolder (DatasetLogicalFolder, Name, Parent)
  values(0, 'ROOT', NULL);

create global temporary table ContainerSearch (
    DatasetLogicalFolder	number,
    DatasetGroup		number,
    ContainerPath varchar(500)
) on commit delete rows;

-- The block keyword should be removed, it's mostly used to tell the scanner
-- that this is one statement.

BLOCK
CREATE TRIGGER TRIG_VDSMS_METANAME AFTER INSERT ON VerDatasetMetaString
   REFERENCING NEW ROW AS newrow
   FOR EACH ROW 
   IF NOT EXISTS 
     (SELECT 1 FROM DatasetMetaName d WHERE d.MetaName = newrow.MetaName)
     THEN 
     INSERT INTO DatasetMetaname (MetaName) VALUES (newrow.MetaName);
   END IF;
END BLOCK;

BLOCK
CREATE TRIGGER TRIG_VDSMN_METANAME AFTER INSERT ON VerDatasetMetaNumber
   REFERENCING NEW ROW AS newrow
   FOR EACH ROW 
   IF NOT EXISTS 
     (SELECT 1 FROM DatasetMetaName d WHERE d.MetaName = newrow.MetaName )
     THEN 
     INSERT INTO DatasetMetaname (MetaName) VALUES (newrow.MetaName);
   END IF;
END BLOCK;

BLOCK
CREATE TRIGGER TRIG_VDSMTS_METANAME AFTER INSERT ON VerDatasetMetaTimeStamp
   REFERENCING NEW ROW AS newrow
   FOR EACH ROW 
   IF NOT EXISTS 
     (SELECT 1 FROM DatasetMetaName d WHERE d.MetaName = newrow.MetaName )
     THEN 
     INSERT INTO DatasetMetaname (MetaName) VALUES (newrow.MetaName);
   END IF;
END BLOCK;

BLOCK
CREATE TRIGGER TRIG_VDSMS_METAINFO AFTER INSERT ON VerDatasetMetaString
   REFERENCING NEW ROW AS newrow
   FOR EACH ROW 
   IF NOT EXISTS 
     (SELECT 1 FROM DatasetMetaInfo d WHERE d.MetaName = newrow.MetaName and d.ValueType = 'S')
     THEN 
     INSERT INTO DatasetMetaInfo (MetaName, ValueType) VALUES (newrow.MetaName, 'S');
   END IF;
END BLOCK;

BLOCK
CREATE TRIGGER TRIG_VDSMN_METAINFO AFTER INSERT ON VerDatasetMetaNumber
   REFERENCING NEW ROW AS newrow
   FOR EACH ROW 
   IF NOT EXISTS 
     (SELECT 1 FROM DatasetMetaInfo d WHERE d.MetaName = newrow.MetaName and d.ValueType = 'N')
     THEN 
     INSERT INTO DatasetMetaInfo (MetaName, ValueType) VALUES (newrow.MetaName, 'N');
   END IF;
END BLOCK;

BLOCK
CREATE TRIGGER TRIG_VDSMTS_METAINFO AFTER INSERT ON VerDatasetMetaTimeStamp
   REFERENCING NEW ROW AS newrow
   FOR EACH ROW 
   IF NOT EXISTS 
     (SELECT 1 FROM DatasetMetaInfo d WHERE d.MetaName = newrow.MetaName and d.ValueType = 'T')
     THEN 
     INSERT INTO DatasetMetaInfo (MetaName, ValueType) VALUES (newrow.MetaName, 'T');
   END IF;
END BLOCK;

-- Need newline at end