
package org.srs.datacat.dao.sql;

import com.google.common.base.Optional;
import org.srs.datacat.dao.sql.DatasetDAO;
import org.srs.datacat.dao.sql.ContainerDAO;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import javax.sql.DataSource;

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
import org.srs.datacat.test.HSqlDbHarness;
import org.srs.vfs.PathUtils;

/**
 *
 * @author bvan
 */
public class DatasetDAOTest {
    
    static HSqlDbHarness harness;
    Connection conn;
    
    public DatasetDAOTest(){ }
    
    public static final String TEST_BASE_NAME = "junit";
    public static final String TEST_BASE_PATH = "/" + TEST_BASE_NAME;
    public static final String TEST_DATATYPE_01 = "JUNIT_TEST01";
    public static final String TEST_FILEFORMAT_01 = "junit.test";
    public static final String TEST_DATASET_SOURCE = "JUNIT";

    @BeforeClass
    public static void setUpDb() throws SQLException, IOException{
        harness = new HSqlDbHarness();
        DataSource d = harness.getDataSource();
        addRecords(d.getConnection());
    }
    
    @AfterClass
    public static void tearDownDb() throws Exception{
        System.out.println("Cleaning up...");
        DataSource d = harness.getDataSource();
        removeRecords(d.getConnection());
    }
    
    public static DatacatObject getDatacatObject(BaseDAO dao, String path) throws IOException, NoSuchFileException {
        if(!PathUtils.isAbsolute( path )){
            path = "/" + path;
        }
        path = PathUtils.normalize( path );
        DatacatObject next = dao.getDatacatObject(null, "/");
        int offsets[] = PathUtils.offsets(path);
        for(int i = 1; i <= offsets.length; i++){
            next = dao.getDatacatObject(next, PathUtils.absoluteSubpath(path, i, offsets));
        }
        return next;
    }

    public static void addRecords(Connection c) throws SQLException, IOException{
        ContainerDAO dao = new ContainerDAO(c);
        try {
            getDatacatObject(dao, TEST_BASE_PATH);
            c.close();
            return;
        } catch (NoSuchFileException x){ }
        
        DatacatObject container = new LogicalFolder.Builder().name( TEST_BASE_NAME ).build();
        dao.insertContainer( 0L, PathUtils.resolve("/",TEST_BASE_NAME), container );
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
        
    public static void removeRecords(Connection conn) throws Exception {
        ContainerDAO dao = new ContainerDAO(conn);
        DatacatObject folder = getDatacatObject(dao, TEST_BASE_PATH);
        dao.deleteFolder(folder.getPk());
        DatasetDAO dsDao = new DatasetDAO(conn);
        dsDao.deleteDatasetDataType(TEST_DATATYPE_01);
        dsDao.deleteDatasetFileFormat(TEST_FILEFORMAT_01);
        conn.commit();
        conn.close();
    }
    
    @Before
    public void connection() throws Exception {
        conn = harness.getDataSource().getConnection();
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
        Optional<DatasetVersion> versionOpt = Optional.absent();
        DatasetVersion newVer = dao.createOrMergeDatasetVersion(ds, req.getVersion(), versionOpt, false);
        System.out.println("Registered: " + newVer.toString());
        System.out.println(new Dataset.Builder(ds).version(newVer).build().toString());
        dao.deleteDatasetVersion(ds.getParentPk(), newVer);
    }
    
    private Dataset create(String path, Dataset ds) throws SQLException, IOException {
        DatasetDAO dao = new DatasetDAO(conn);
        System.out.println(path);
        DatacatObject folder = getDatacatObject(dao, path);
        return dao.insertDataset(folder.getPk(), DatacatObject.Type.FOLDER, PathUtils.resolve(path, ds.getName()), ds);
    }
    
    private FlatDataset.Builder getRequest(String dsName) throws SQLException, IOException{
        FlatDataset.Builder builder = new FlatDataset.Builder();
        builder.name(dsName);
        builder.dataType(TEST_DATATYPE_01);
        builder.fileFormat(TEST_FILEFORMAT_01);
        HashMap m = new HashMap();
        m.put( "fakeMetadata", "fakeString");
        m.put( "fakeMetadataNumber", 24);
        m.put( "fakeMetadataDecimal", 24.242);
        builder.versionMetadata(m);
        builder.versionId(DatasetView.NEW_VER);
        builder.dataType(TEST_DATATYPE_01);
        builder.datasetSource(TEST_DATASET_SOURCE);
        return builder;
    }


}
