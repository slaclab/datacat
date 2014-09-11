

delete from datasetversion where dataset = (select dataset from verdataset where datasetname = 'testds1');
delete from verdataset where datasetname = 'testds1';
delete from datasetgroup where name = 'testgroup';
delete from datasetlogicalfolder where name = 'testfolder';
delete from datasetlogicalfolder where name = 'testpath';

insert into datasetdatatype (datasetdatatype) values ('JUNIT_TEST');
insert into datasetfileformat (datasetfileformat) values ('junit.test');
insert into datasetsource (datasetsource) values ('JUNIT_SOURCE');

insert into datasetlogicalfolder (name, parent) values  ('testpath',0);
insert into datasetlogicalfolder (name, parent) values ('testfolder',(select datasetlogicalfolder from datasetlogicalfolder where name = 'testpath'));
insert into datasetgroup (name, datasetlogicalfolder) values ('testgroup',(select datasetlogicalfolder from datasetlogicalfolder where name = 'testpath'));

insert into VerDataset (DatasetName, DataSetFileFormat, DataSetDataType, DatasetLogicalFolder, DatasetGroup) 
        values ('testds1', 'junit.test', 'JUNIT_TEST', null, (select datasetgroup from datasetgroup where name = 'testgroup'));

insert into DatasetVersion (Dataset, VersionID, DataSetSource, ProcessInstance, TaskName) 
        values ((select dataset from verdataset where datasetname = 'testds1'), -4, 'JUNIT_SOURCE', null, null);
               
insert into DatasetVersion (Dataset, DataSetSource, ProcessInstance, TaskName) 
        values ((select dataset from verdataset where datasetname = 'testds1'), 'JUNIT_SOURCE', null, null);
        
insert into DatasetVersion (Dataset, DataSetSource, ProcessInstance, TaskName) 
        values ((select dataset from verdataset where datasetname = 'testds1'), 'JUNIT_SOURCE', null, null);        

insert into DatasetVersion (Dataset, DataSetSource, ProcessInstance, TaskName) 
        values ((select dataset from verdataset where datasetname = 'testds1'), 'JUNIT_SOURCE', null, null);

insert into DatasetVersion (Dataset, VersionID, DataSetSource, ProcessInstance, TaskName) 
        values ((select dataset from verdataset where datasetname = 'testds1'), 24, 'JUNIT_SOURCE', null, null);
      
insert into DatasetVersion (Dataset, VersionID, DataSetSource, ProcessInstance, TaskName) 
        values ((select dataset from verdataset where datasetname = 'testds1'), 0, 'JUNIT_SOURCE', null, null);
        


insert into datasetlogicalfolder (name, parent) values ('a',(select datasetlogicalfolder from datasetlogicalfolder where name = 'testpath'));
insert into datasetlogicalfolder (name, parent) values ('b',(select datasetlogicalfolder from datasetlogicalfolder where name = 'a'));
insert into datasetlogicalfolder (name, parent) values ('c',(select datasetlogicalfolder from datasetlogicalfolder where name = 'b'));

insert into datasetlogicalfolder (name, parent) values ('abc',(select datasetlogicalfolder from datasetlogicalfolder where name = 'testpath'));
insert into datasetlogicalfolder (name, parent) values ('def',(select datasetlogicalfolder from datasetlogicalfolder where name = 'abc'));
insert into datasetlogicalfolder (name, parent) values ('xyz',(select datasetlogicalfolder from datasetlogicalfolder where name = 'def'));

insert into datasetgroup (name, datasetlogicalfolder) values ('fed',(select datasetlogicalfolder from datasetlogicalfolder where name = 'abc'));
insert into datasetgroup (name, datasetlogicalfolder) values ('zyx',(select datasetlogicalfolder from datasetlogicalfolder where name = 'def'));

insert into datasetmetaname (metaname) VALUES ('nRun');
insert into datasetmetaname (metaname) VALUES ('alpha');
insert into datasetmetaname (metaname) VALUES ('num');
insert into datasetmetaname (metaname) VALUES ('sIntent');
