/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.srs.datacat.sql;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.LogicalFolder;
import org.srs.datacat.shared.dataset.FlatDataset;

/**
 *
 * @author bvan
 */
public class DatasetDAOTest {
    
    Connection conn;
    
    public DatasetDAOTest(){ }
    
    public static final String TEST_BASE_NAME = "junit";
    public static final String TEST_BASE_PATH = "/" + TEST_BASE_NAME;
    public static final String TEST_DATATYPE_01 = "JUNIT_TEST01";
    public static final String TEST_FILEFORMAT_01 = "junit.test";
    public static final String TEST_DATASET_SOURCE = "JUNIT";

    
    @BeforeClass
    public static void setUpDb() throws SQLException, IOException{
        System.out.println("working...");
        Connection c = Utils.getConnection();
        ContainerDAO dao = new ContainerDAO(c);
        try {
            dao.getDatacatObject(TEST_BASE_PATH);
            c.close();
            return;
        } catch (FileNotFoundException x){ }
            
        DatacatObject container = new LogicalFolder.Builder().name( TEST_BASE_NAME ).build();
        dao.insertContainer( 0L, "/", container );
        System.out.println( "creating file" );
        DatasetDAO dsDao = new DatasetDAO( c );
        try {
            dsDao.insertDatasetSource(TEST_DATASET_SOURCE);
        } catch (SQLException ex){}
        try {
            dsDao.insertDatasetDataType( TEST_DATATYPE_01, null, null );
        } catch (SQLException ex){}
        try {
            dsDao.insertDatasetFileFormat( TEST_FILEFORMAT_01, null, null );
        } catch (SQLException ex){}
        
        c.commit();
        c.close();
    }

    @AfterClass
    public static void tearDownDb() throws Exception{
        System.out.println("Cleaning up...");
        Connection c = Utils.getConnection();
        ContainerDAO dao = new ContainerDAO(c);
        DatacatObject folder = dao.getDatacatObject(TEST_BASE_PATH);
        dao.deleteFolder(folder.getPk());
        DatasetDAO dsDao = new DatasetDAO(c);
        dsDao.deleteDatasetDataType(TEST_DATATYPE_01);
        dsDao.deleteDatasetFileFormat(TEST_FILEFORMAT_01);
        c.commit();
        c.close();
    }
    
    @Before
    public void connection() throws Exception {
        conn = Utils.getConnection();
    }

    @After
    public void tearDown() throws Exception{
        conn.rollback();
        conn.close();
    }
    
    @Test
    public void testCreateNode() throws IOException, SQLException{
        String dsName = "testCaseDataset001";

        FlatDataset req =getRequest( dsName ).build();
        create(TEST_BASE_PATH, req);
    }
    
    @Test
    public void testDeleteDatasetVersion() throws SQLException, IOException {
        String dsName = "testCaseDataset002";
        DatasetDAO dao = new DatasetDAO(conn);
        
        FlatDataset req =(FlatDataset) getRequest(dsName)
                .versionId(DatasetView.NEW_VER)
                .datasetSource(TEST_DATASET_SOURCE)
                .build();
        Dataset ds = create(TEST_BASE_PATH, req);
        
        
        DatasetVersion newVer = dao.createOrMergeDatasetVersion(ds.getPk(), req.getPath(), null, req.getVersion(), false );
        System.out.println("Registered: " + newVer.toString());
        System.out.println(Dataset.Builder.create(ds).version(newVer).build().toString());
        dao.deleteDatasetVersion(ds.getParentPk(), newVer);
    }
    
    private Dataset create(String path, Dataset ds) throws SQLException, IOException {
        DatasetDAO dao = new DatasetDAO(conn);
        System.out.println(path);
        DatacatObject folder = dao.getDatacatObject(path);
        return dao.insertDataset(folder.getPk(), DatacatObject.Type.FOLDER, "/", ds);
    }
    
    private FlatDataset.Builder getRequest(String dsName) throws SQLException, IOException{
        FlatDataset.Builder builder = new FlatDataset.Builder();
        builder.name(dsName);
        builder.datasetDataType(TEST_DATATYPE_01);
        builder.datasetFileFormat(TEST_FILEFORMAT_01);
        HashMap m = new HashMap();
        m.put( "fakeMetadata", "fakeString");
        m.put( "fakeMetadataNumber", 24);
        m.put( "fakeMetadataDecimal", 24.242);
        builder.versionMetadata(m);
        builder.versionId(DatasetView.NEW_VER);
        builder.datasetDataType(TEST_DATATYPE_01);
        builder.datasetSource(TEST_DATASET_SOURCE);
        return builder;
    }


}
