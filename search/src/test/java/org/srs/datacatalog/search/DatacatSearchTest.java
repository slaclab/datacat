/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacatalog.search;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.sql.DataSource;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.RequestView;
import org.srs.datacatalog.search.plugins.DatacatPlugin;
import org.srs.datacatalog.search.plugins.EXODatacatSearchPlugin;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.LogicalFolder;
import org.srs.datacat.test.HSqlDbHarness;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcFileSystemProvider;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcUriUtils;
import org.srs.datacat.vfs.DirectoryWalker.ContainerVisitor;
import org.srs.datacat.vfs.attribute.ContainerCreationAttribute;
import org.srs.datacat.vfs.attribute.DatasetOption;
import org.srs.datacat.vfs.attribute.DatasetViewProvider;
import org.zerorm.core.Select;

/**
 *
 * @author bvan
 */
public class DatacatSearchTest {
    
    static HSqlDbHarness harness;
    DatasetSearch datacatSearch;
    HashMap<String, DatacatPlugin> pluginMap;
    public DataSource ds = null;
    DcPath root;
    DcFileSystemProvider provider;
    
    String query1 = "\nSELECT ds.latestVersion, ds.dataset pk, ds.datasetName name, ds.datasetFileFormat, ds.datasetGroup, ds.datasetDataType, ds.datasetlogicalfolder, v.masterLocation, v.dataset, v.versionId, v.datasetVersion, l.runMin, l.datasetLocation, l.datasetSite, l.checkSum, l.path, l.numberEvents, l.runMax, l.scanstatus, l.fileSizeBytes, 'DATASET' type, CASE WHEN ds.datasetlogicalfolder IS NOT NULL  THEN ds.datasetlogicalfolder ELSE ds.datasetGroup END  parent, dsmv0.metaValue \"nRun\", eri.fullTypeName runType, eri.quality runQuality, eri.runIndex runId, dsmv1.metaValue \"nSegment\"\n" +
                "FROM VerDataset ds\n" +
                "LEFT OUTER JOIN DatasetVersion v ON ( ds.latestVersion = v.datasetVersion )\n" +
                "LEFT OUTER JOIN VerdatasetLocation l ON ( v.masterLocation = l.datasetLocation )\n" +
                "LEFT OUTER JOIN ( SELECT dsmv0.DatasetVersion, dsmv0.metaValue, dsmv0.metaName FROM VerDatasetMetanumber dsmv0 WHERE dsmv0.metaName = 'nRun' ) dsmv0 ON ( l.datasetVersion = dsmv0.DatasetVersion )\n" +
                "LEFT OUTER JOIN EXORunIndex eri ON ( dsmv0.metaValue = eri.runIndex )\n" +
                "LEFT OUTER JOIN ( SELECT dsmv1.DatasetVersion, dsmv1.metaValue, dsmv1.metaName FROM VerDatasetMetanumber dsmv1 WHERE dsmv1.metaName = 'nSegment' ) dsmv1 ON ( l.datasetVersion = dsmv1.DatasetVersion )\n" +
                "WHERE ( eri.runIndex > ? AND eri.quality = ? ) AND dsmv1.metaValue = ?";

    public DatacatSearchTest() throws SQLException, IOException {
        ds = harness.getDataSource();
        provider = new DcFileSystemProvider(ds);
        root = provider.getPath(DcUriUtils.toFsUri( "/", null, "SRS"));
        
        pluginMap = new HashMap<>();
        DatacatPlugin exoPlugin = new EXODatacatSearchPlugin();
        pluginMap.put( exoPlugin.getNamespace(), exoPlugin);
    }
        
    @BeforeClass
    public static void setUpDb() throws SQLException, IOException{
        harness = new HSqlDbHarness();
        DataSource d = harness.getDataSource();
    }

    public void testSomeMethod() throws Exception{
        Connection conn = ds.getConnection();        
        String query = "exo.runId > 5490 3242 and exo.runQuality = 'GOLDEN' \n"
                        + "and name eq 'newName' and \n"
                        + "(datasetGroup = 38288428 ) \n"
                        + "and ADO15 = d'2012-01-01' \n"
                        + "and ADO15 in (1.23, 2, 5) \n"
                        + "and ADO15 in ('hello','how','are','you') \n"
                        + "and ADO15 in (d'2012-03-23', d'2012-12-23T12:10Z')";
        query = "exo.runId > 5490 and exo.runQuality = 'GOLDEN' and nSegment = 1";
        
        datacatSearch = new DatasetSearch(provider, conn, pluginMap);
        /*datacatSearch.prepareSelection( query );
        
        DatasetVersions select = datacatSearch.getSelectStatement();

        String query1Actual = select.formatted();
        System.out.println( select.formatted());
        System.out.println( query1Actual );
        assertEquals("Checking query 1", query1, query1Actual );*/
        
    }
    
    @Test
    public void testSearchForDatasetsInParent() throws Exception{
        
        DcPath parent = root.resolve( "/testpath");
        LogicalFolder.Builder builder = new LogicalFolder.Builder();

        /*
            The following is faster:
            To create 100 folders, THEN create datasets in each of those folders
        */
        // Create 10 folders
        for(int i = 0; i < 20; i++){
            String name =String.format("folder%05d", i);
            DcPath newPath = parent.resolve(name);
            builder.name(name);
            provider.createDirectory( newPath, new ContainerCreationAttribute(builder.build()) );
        }
        
        String alphaName = "alpha";
        String numberName = "num";
        String alphaMdValues[] = {"abc","def","ghi","jkl","xyz"};
        Number numberMdValues[] = {0, 3.14159f, 4294967296L, -1.0000000001d, 1};

        List opts = Arrays.asList(DatasetOption.CREATE_NODE, DatasetOption.CREATE_VERSION, DatasetOption.SKIP_NODE_CHECK);
        HashSet<DatasetOption> options = new HashSet<>(opts);
        // Create 20k datasets
        for(int i = 0; i < 20; i++){
            String name =String.format("folder%05d", i);
            DcPath newPath = parent.resolve(name);
            System.out.println(newPath);
            for(int j = 0; j < 1000; j++){
                Dataset.Builder dsBuilder = new Dataset.Builder();
                name = String.format("dataset%05d", j);
                dsBuilder.name(name);
                dsBuilder.datasetDataType(HSqlDbHarness.JUNIT_DATASET_DATATYPE);
                dsBuilder.datasetSource(HSqlDbHarness.JUNIT_DATASET_DATASOURCE);
                dsBuilder.datasetFileFormat(HSqlDbHarness.JUNIT_DATASET_FILEFORMAT);
                dsBuilder.versionId( DatasetView.NEW_VER );
                HashMap<String, Object> metadata = new HashMap<>();
                metadata.put( numberName, numberMdValues[i % 4]);
                metadata.put( alphaName, alphaMdValues[j % 4]);
                dsBuilder.versionMetadata( metadata );
                provider.createDataset(newPath.resolve(name), dsBuilder.build(), options );
            }
        }
        

        Connection conn = ds.getConnection();
        conn.commit();
        conn.close();
        conn = ds.getConnection();

        String basePath;
        boolean recurseFolders;
        boolean searchFolders;
        boolean searchGroups;
        String queryString;
        String[] sites;
        String[] metaFieldsToRetrieve;
        String[] sortFields;
        int ii;
        
        ContainerVisitor visitor;
        Select statement;
        List<Dataset> datasets;
        DcPath searchPath;
        boolean keepAlive;
        
        datacatSearch = new DatasetSearch(provider, conn, pluginMap);
        
        searchFolders = true;
        searchGroups = true;
        queryString = "";
        sites = null;
        metaFieldsToRetrieve = null;
        sortFields = null;
        
        
        queryString = "alpha == 'def'";
        searchPath = root.resolve("/testpath/folder00001");
        visitor = new ContainerVisitor(root.getFileSystem(), searchPath.toString(), searchGroups, searchFolders);
        keepAlive = true;
        statement = datacatSearch.compileStatement( conn, searchPath, visitor, false, 100, queryString, sites, metaFieldsToRetrieve, sortFields,0,-1);
        System.out.println("ready");
        
        System.out.println(statement.formatted());
        datasets = datacatSearch.searchForDatasetsInParent( conn, statement, keepAlive);
        ii = 0;
        for(Dataset d: datasets){
            if(ii == 0){
                TestCase.assertEquals("First dataset found incorrect", "dataset00001", d.getName());
            }
            ii++;
        }
        TestCase.assertEquals("Should have found 250 datasets out of 1000", 250, ii);
        System.out.println("Found " + ii + " results");
        conn.commit(); // Remove from parents on commit
        
        
        queryString = "num == 0 and alpha == 'def'";
        searchPath = root.resolve("/testpath");
        visitor = new ContainerVisitor(root.getFileSystem(), searchPath.resolve("*").toString(), searchGroups, searchFolders);
        keepAlive = true;
        statement = datacatSearch.compileStatement( conn, searchPath, visitor, false, 100, queryString, sites, metaFieldsToRetrieve, sortFields,0,-1);
        System.out.println("ready");
        
        System.out.println(statement.formatted());
        datasets = datacatSearch.searchForDatasetsInParent( conn, statement, keepAlive);
        ii = 0;
        for(Dataset d: datasets){
            System.out.println(d.getPath());
            System.out.println(d.toString());
            if(ii == 0){
                TestCase.assertEquals("First dataset found incorrect", "dataset00001", d.getName());
            }
            ii++;
        }
        TestCase.assertEquals("Should have found 250 datasets out of 1000", 500*10, ii);
        System.out.println("Found " + ii + " results");
        conn.commit(); // Remove from parents on commit
        
        queryString = "runMin == 239557414";
        searchPath = root.resolve("/testpath");
        visitor = new ContainerVisitor(root.getFileSystem(), "/testpath", searchGroups, searchFolders);
        keepAlive = true;
        statement = datacatSearch.compileStatement( conn, searchPath, visitor, false, 100, queryString, sites, metaFieldsToRetrieve, sortFields,0,-1);
        System.out.println(statement.formatted());
        
        /*datasets = datacatSearch.searchForDatasetsInParent( conn, statement, keepAlive);
        for(Dataset d: datasets){
            System.out.println( d.toString() );
        }*/
        conn.commit(); // Remove from parents on commit
        
        queryString = "nRun == 239557414";
        searchPath = root.resolve("/testpath");
        visitor = new ContainerVisitor(root.getFileSystem(), "/testpath", searchGroups, searchFolders);
        keepAlive = true;
        statement = datacatSearch.compileStatement( conn, searchPath, visitor, false, 100, queryString, sites, metaFieldsToRetrieve, sortFields,0,-1);
        System.out.println(statement.formatted());
        
        /*datasets = datacatSearch.searchForDatasetsInParent( conn, statement, keepAlive);
        for(Dataset d: datasets){
            System.out.println( d.toString() );
        }*/
        conn.commit(); // Remove from parents on commit
        
        queryString = "sIntent == 'run'";
        searchPath = root.resolve("/testpath");
        visitor = new ContainerVisitor(root.getFileSystem(), "/testpath", searchGroups, searchFolders);
        keepAlive = true;
        statement = datacatSearch.compileStatement( conn, searchPath, visitor, false, 100, queryString, sites, metaFieldsToRetrieve, sortFields,0,-1);
        System.out.println(statement.formatted());
        
        /*datasets = datacatSearch.searchForDatasetsInParent( conn, statement, keepAlive);
        for(Dataset d: datasets){
            System.out.println( d.toString() );
        }*/
        conn.commit(); // Remove from parents on commit
        
        conn.close();
    }
}
