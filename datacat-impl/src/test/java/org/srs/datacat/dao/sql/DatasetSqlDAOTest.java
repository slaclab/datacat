
package org.srs.datacat.dao.sql;

import com.google.common.base.Optional;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.NoSuchFileException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.sql.DataSource;
import junit.framework.TestCase;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.srs.datacat.dao.BaseDAO;
import org.srs.datacat.dao.ContainerDAO;
import org.srs.datacat.dao.DAOFactory;
import org.srs.datacat.dao.sql.SqlDAOFactory.Locker;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.dataset.DatasetVersionModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.LogicalFolder;
import org.srs.datacat.shared.FlatDataset;
import org.srs.datacat.test.DbHarness;
//import org.srs.datacat.vfs.TestUtils;
import org.srs.vfs.PathUtils;

/**
 *
 * @author bvan
 */
public class DatasetSqlDAOTest {
    
    static DbHarness harness;
    Connection conn = null;
    
    public DatasetSqlDAOTest(){ }
    
    @BeforeClass
    public static void setUpDb() throws SQLException, IOException{
        harness = DbHarness.getDbHarness();
        DataSource d = harness.getDataSource();
        addRecords(new org.srs.datacat.dao.sql.mysql.DAOFactoryMySQL(d));
    }
    
    @AfterClass
    public static void tearDownDb() throws Exception{
        System.out.println("Cleaning up...");
        DataSource d = harness.getDataSource();
        removeRecords(d.getConnection());
    }
    
    public static DatacatNode getDatacatObject(BaseDAO dao, String path) throws IOException, NoSuchFileException {
        if(!PathUtils.isAbsolute( path )){
            path = "/" + path;
        }
        path = PathUtils.normalize( path );
        DatacatNode next = dao.getObjectInParent(null, "/");
        int offsets[] = PathUtils.offsets(path);
        for(int i = 1; i <= offsets.length; i++){
            next = dao.getObjectInParent(next, PathUtils.getFileName(PathUtils.absoluteSubpath(path, i, offsets)));
        }
        return next;
    }

    public static void addRecords(DAOFactory factory) throws SQLException, IOException{
        try (BaseDAO dao = factory.newBaseDAO()){
            getDatacatObject(dao, DbHarness.TEST_BASE_PATH);
            return;
        } catch (NoSuchFileException x){ }
        
        try(ContainerDAO dao = factory.newContainerDAO()) {
            DatasetContainer container = new LogicalFolder.Builder().name(DbHarness.TEST_BASE_NAME).build();
            DatasetContainer rootRecord = new LogicalFolder.Builder().pk(0L).path("/").build();
            dao.createNode(rootRecord, DbHarness.TEST_BASE_NAME, container);
            dao.commit();            
        }
    }
            
    public static void removeRecords(Connection conn) throws Exception {
        Locker locker = new Locker();
        SqlContainerDAO dao = new SqlContainerDAO(conn, locker);
        DatacatNode folder = getDatacatObject(dao, DbHarness.TEST_BASE_PATH);
        dao.deleteFolder(folder.getPk());
        DatacatNode group = getDatacatObject(dao, "/testpath/testgroup");
        dao.deleteGroup(group.getPk());
        SqlDatasetDAO dsDao = new SqlDatasetDAO(conn, locker);
        dsDao.deleteDatasetDataType(DbHarness.TEST_DATATYPE_01);
        dsDao.deleteDatasetFileFormat(DbHarness.TEST_FILEFORMAT_01);
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
        create(DbHarness.TEST_BASE_PATH, req);
    }
    
    @Test
    public void testDeleteDatasetVersion() throws SQLException, IOException {
        String dsName = "testCaseDataset002";
        Locker locker = new Locker();
        SqlDatasetDAO dao = new SqlDatasetDAO(conn, locker);
        
        FlatDataset req =(FlatDataset) getRequest(dsName)
                .versionId(DatasetView.NEW_VER)
                .datasetSource(DbHarness.TEST_DATASET_SOURCE)
                .build();
        Dataset ds = create(DbHarness.TEST_BASE_PATH, req);
        Optional<DatasetVersionModel> versionOpt = Optional.absent();
        DatasetVersion newVer = dao.createOrMergeDatasetVersion(ds, (DatasetVersion) req.getVersion(), versionOpt, false);
        System.out.println("Registered: " + newVer.toString());
        System.out.println(new Dataset.Builder(ds).version(newVer).build().toString());
        dao.deleteDatasetVersion(ds.getParentPk(), newVer);
    }
    
    @Test
    public void testMetadata() throws SQLException, IOException {
        String dsName = "testCaseDataset002";
        Locker locker = new Locker();
        SqlDatasetDAO dao = new SqlDatasetDAO(conn, locker);
        
        FlatDataset req =(FlatDataset) getRequest(dsName)
                .versionId(DatasetView.NEW_VER)
                .datasetSource(DbHarness.TEST_DATASET_SOURCE)
                .build();
        Dataset ds = create(DbHarness.TEST_BASE_PATH, req);
        Optional<DatasetVersionModel> versionOpt = Optional.absent();
        DatasetVersionModel newVer = dao.createOrMergeDatasetVersion(ds, (DatasetVersion) req.getVersion(), versionOpt, false);
        HashMap<String, Object> md = new HashMap();
        md.put("stringObject", "mdvalue");
        md.put("intObject", 1234);
        md.put("doubleObject", 1234.4321);
        md.put("tsObject", new java.sql.Timestamp(1234L));
        System.out.println(newVer);
        dao.mergeMetadata(newVer, md);

        md.put( "fakeMetadata", "fakeString");
        md.put( "fakeMetadataNumber", 24);
        md.put( "fakeMetadataDecimal", 24.242);
        
        newVer = dao.getDatasetViewInfo(ds, DatasetView.CURRENT_ANY).getVersion();
        for(Map.Entry<String, Object> e: md.entrySet()){
            Object expected = e.getValue();
            Object actual = newVer.getMetadataMap().get(e.getKey());
            if(expected instanceof Number){
                if(expected instanceof Double | expected instanceof Float){
                    expected = BigDecimal.valueOf(((Number) expected).doubleValue());
                    actual = BigDecimal.valueOf(((Number) actual).doubleValue());
                }
                if(expected instanceof Integer | expected instanceof Long){
                    expected = BigInteger.valueOf(((Number) expected).longValue()).longValue();
                    actual = ((Number) actual).longValue();
                }
            }
            TestCase.assertEquals(String.format("%s should have been %s", actual.toString(), expected.toString()), expected, actual);
        }
        
        HashMap<String, Object> merge = new HashMap<>();
        merge.put("stringObject", "mdvalue1");
        merge.put("intObject", 4321);
        merge.put("doubleObject", 4321.1234);
        md.put("stringObject", "mdvalue1");
        md.put("intObject", 4321);
        md.put("doubleObject", 4321.1234);
        dao.mergeMetadata(newVer, merge);
        
        newVer = dao.getDatasetViewInfo(ds, DatasetView.CURRENT_ANY).getVersion();
        for(Map.Entry<String, Object> e: md.entrySet()){
            Object expected = e.getValue();
            Object actual = newVer.getMetadataMap().get(e.getKey());
            if(expected instanceof Number){
                if(expected instanceof Double | expected instanceof Float){
                    expected = BigDecimal.valueOf(((Number) expected).doubleValue());
                    actual = BigDecimal.valueOf(((Number) actual).doubleValue());
                }
                if(expected instanceof Integer | expected instanceof Long){
                    expected = BigInteger.valueOf(((Number) expected).longValue()).longValue();
                    actual = ((Number) actual).longValue();
                }
            }
            TestCase.assertEquals(String.format("%s should have been %s", actual.toString(), expected.toString()), expected, actual);
        }
        
        HashMap<String, Object> deletes = new HashMap<>();
        deletes.put("stringObject", null);
        deletes.put("intObject", null);
        deletes.put("doubleObject", null);
        md.remove("stringObject", "mdvalue1");
        md.remove("intObject", 4321);
        md.remove("doubleObject", 4321.1234);
        dao.mergeMetadata(newVer, deletes);
        
        newVer = dao.getDatasetViewInfo(ds, DatasetView.CURRENT_ANY).getVersion();
        for(Map.Entry<String, Object> e: md.entrySet()){
            Object expected = e.getValue();
            Object actual = newVer.getMetadataMap().get(e.getKey());
            if(expected instanceof Number){
                if(expected instanceof Double | expected instanceof Float){
                    expected = BigDecimal.valueOf(((Number) expected).doubleValue());
                    actual = BigDecimal.valueOf(((Number) actual).doubleValue());
                }
                if(expected instanceof Integer | expected instanceof Long){
                    expected = BigInteger.valueOf(((Number) expected).longValue()).longValue();
                    actual = ((Number) actual).longValue();
                }
            }
            TestCase.assertEquals(String.format("%s should have been %s", actual.toString(), expected.toString()), expected, actual);
        }
        
    }
    
    private Dataset create(String path, Dataset ds) throws SQLException, IOException {
        Locker locker = new Locker();
        SqlDatasetDAO dao = new SqlDatasetDAO(conn, locker);
        System.out.println(path);
        DatacatNode folder = getDatacatObject(dao, path);
        return dao.insertDataset(folder, PathUtils.resolve(path, ds.getName()), ds);
    }
    
    private FlatDataset.Builder getRequest(String dsName) throws SQLException, IOException{
        FlatDataset.Builder builder = new FlatDataset.Builder();
        builder.name(dsName);
        builder.dataType(DbHarness.TEST_DATATYPE_01);
        builder.fileFormat(DbHarness.TEST_FILEFORMAT_01);
        HashMap m = new HashMap();
        m.put( "fakeMetadata", "fakeString");
        m.put( "fakeMetadataNumber", 24);
        m.put( "fakeMetadataDecimal", 24.242);
        builder.versionMetadata(m);
        builder.versionId(DatasetView.NEW_VER);
        builder.dataType(DbHarness.TEST_DATATYPE_01);
        builder.datasetSource(DbHarness.TEST_DATASET_SOURCE);
        return builder;
    }


}
