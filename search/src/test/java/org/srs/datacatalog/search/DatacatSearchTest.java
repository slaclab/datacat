/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacatalog.search;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.sql.DataSource;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.srs.datacat.model.DatasetView;
import org.srs.datacatalog.search.plugins.DatacatPlugin;
import org.srs.datacatalog.search.plugins.EXODatacatSearchPlugin;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.LogicalFolder;
import org.srs.datacat.shared.dataset.FlatDataset;
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
import org.srs.datacat.vfs.TestUtils;
import org.srs.vfs.GlobToRegex;
import org.srs.vfs.PathUtils;

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
        
        TestUtils.generateDatasets(root, provider, 20, 1000);

        Connection conn = ds.getConnection();
        conn.commit();
        conn.close();
        conn = ds.getConnection();

        String basePath;
        boolean recurseFolders;
        Boolean searchFolders;
        Boolean searchGroups;
        String queryString;
        String[] sites;
        String[] metaFieldsToRetrieve;
        String[] sortFields;
        int ii;
        
        ContainerVisitor visitor;
        Select statement;
        List<Dataset> datasets;
        DcPath searchPath;
        String pathPattern;
        
        datacatSearch = new DatasetSearch(provider, conn, pluginMap);
        
        searchFolders = true;
        searchGroups = true;
        queryString = "";
        sites = null;
        metaFieldsToRetrieve = null;
        sortFields = null;
        
        
        queryString = "alpha == 'def'";
        pathPattern = "/testpath/folder00001";
        datasets = doSearch( conn, root, pathPattern, searchFolders, searchGroups, queryString, 250 );
        TestCase.assertEquals("First dataset found incorrect", "dataset00001", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "alpha == 'def'";
        pathPattern = "/testpath/folder0000*";
        datasets = doSearch( conn, root, pathPattern, searchFolders, searchGroups, queryString, 10*250 );
        TestCase.assertEquals("First dataset found incorrect", "dataset00001", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "alpha == 'def'";
        pathPattern = "/testpath/folder0000[1]";
        datasets = doSearch( conn, root, pathPattern, searchFolders, searchGroups, queryString, 250 );
        TestCase.assertEquals("First dataset found incorrect", "dataset00001", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "num == false";
        pathPattern = "/testpath/folder00000";
        datasets = doSearch( conn, root, pathPattern, searchFolders, searchGroups, queryString, 1000);
        TestCase.assertEquals("First dataset found incorrect", "dataset00000", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "num == true";
        pathPattern = "/testpath/folder00001";
        datasets = doSearch( conn, root, pathPattern, searchFolders, searchGroups, queryString, 0);
        conn.commit(); // Remove from parents on commit
        
        queryString = "num == true";
        pathPattern = "/testpath/folder00003";
        datasets = doSearch( conn, root, pathPattern, searchFolders, searchGroups, queryString, 0);
        conn.commit(); // Remove from parents on commit
        
        try {
            queryString = "num is not null";
            pathPattern = "/testpath/folder00001";
            datasets = doSearch( conn, root, pathPattern, searchFolders, searchGroups, queryString, 1000);
            TestCase.fail( "Should fail on null queries");
        } catch (IllegalArgumentException ex){
            conn.commit(); // Remove from parents on commit
        }
        
        searchFolders = null;
        searchGroups = null;
        queryString = "alpha == 'def'";
        pathPattern = "/testpath/folder0000*$";
        datasets = doSearch( conn, root, pathPattern, searchFolders, searchGroups, queryString,2500);
        TestCase.assertEquals("First dataset found incorrect", "dataset00001", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "alpha == 'def'";
        pathPattern = "/testpath/folder0000*^";
        doSearch( conn, root, pathPattern, searchFolders, searchGroups, queryString,0);
        conn.commit(); // Remove from parents on commit
        
        searchFolders = true;
        searchGroups = true;
        
        queryString = "alpha =~ 'de%'";
        pathPattern ="/testpath/folder00001";
        datasets = doSearch( conn, root, pathPattern, searchFolders, searchGroups, queryString, 250);
        TestCase.assertEquals("First dataset found incorrect", "dataset00001", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "alpha =~ 'de_'";
        pathPattern ="/testpath/folder00001";
        datasets = doSearch( conn, root, pathPattern, searchFolders, searchGroups, queryString, 250);
        TestCase.assertEquals("First dataset found incorrect", "dataset00001", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit
        
        int expected;
        searchFolders = null;
        searchGroups = null;
        queryString = "alpha == 'def' or num == 3.14159f";
        pathPattern = "/testpath/folder0000*$";
        expected = (250*10) + (750*3); // choose 1 mod 4 folders under 10, so 3 will include all
        datasets = doSearch( conn, root, pathPattern, searchFolders, searchGroups, queryString, expected);
        TestCase.assertEquals("First dataset found incorrect", "dataset00001", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit

        queryString = "alpha == 'def' or num in (0,3.14159f)";
        pathPattern = "/testpath/folder0000*$";
        expected = 250*10 + 750*3 + 750*3;
        datasets = doSearch( conn, root, pathPattern, searchFolders, searchGroups, queryString, expected);
        TestCase.assertEquals("First dataset found incorrect", "dataset00000", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "alpha == 'def' or num in (3.1414:3.1416)";
        pathPattern = "/testpath/folder0000*$";
        expected = (250*10) + (750*3); // choose 1 mod 4 folders under 10, so 3 will include all
        doSearch( conn, root, pathPattern, searchFolders, searchGroups, queryString, expected);
        conn.commit(); // Remove from parents on commit
        
        queryString = "num == 0 and alpha == 'def'";
        pathPattern ="/testpath/*";
        doSearch( conn, root, pathPattern, searchFolders, searchGroups, queryString, 1250);
        conn.commit(); // Remove from parents on commit
        
        // Test sortFields
        sortFields = new String[]{"num", "alpha"};
        queryString = "num == 0 and alpha == 'def'";
        pathPattern = "/testpath/*";
        doSearch( conn, root, pathPattern, searchFolders, searchGroups, queryString, sortFields, 1250);
        conn.commit(); // Remove from parents on commit
        
        /*datasets = datacatSearch.searchForDatasetsInParent( conn, statement, keepAlive);
        for(Dataset d: datasets){
            System.out.println( d.toString() );
        }*/
        conn.commit(); // Remove from parents on commit
        
        
        // TODO: Finish More tests
        queryString = "nRun == 239557414";
        searchPath = root.resolve("/testpath");
        System.out.println(datacatSearch.dmc.toString());
        visitor = new ContainerVisitor(root.getFileSystem(), "/testpath", searchGroups, searchFolders);
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
        statement = datacatSearch.compileStatement( conn, searchPath, visitor, false, 100, queryString, sites, metaFieldsToRetrieve, sortFields,0,-1);
        System.out.println(statement.formatted());
        
        /*datasets = datacatSearch.searchForDatasetsInParent( conn, statement, keepAlive);
        for(Dataset d: datasets){
            System.out.println( d.toString() );
        }*/
        conn.commit(); // Remove from parents on commit
        
        conn.close();
    }
    
    private List<Dataset> doSearch(Connection conn, DcPath root, String pathPattern, Boolean searchFolders, Boolean searchGroups, String queryString, int expected) throws SQLException, ParseException, IOException{
        String searchBase = PathUtils.normalizeRegex(GlobToRegex.toRegex(pathPattern,"/"));
        DcPath searchPath = root.resolve(searchBase);
        String[] metaFieldsToRetrieve = null;
        String[] sortFields = null;
        //String[] sortFields = new String[]{"num","alpha"};
        String[] sites = null;
        ContainerVisitor visitor = new ContainerVisitor(root.getFileSystem(), pathPattern, searchGroups, searchFolders);
        System.out.println("With visitor: " + visitor.toString());
        Select statement = datacatSearch.compileStatement( conn, searchPath, visitor, false, 100, queryString, sites, metaFieldsToRetrieve, sortFields,0,-1);
        List<Dataset> datasets = datacatSearch.searchForDatasetsInParent( conn, statement);
        int ii = 0;
        for(Dataset d: datasets){
            ii++;
        }
        TestCase.assertEquals("Should have found "+ expected + " datasets out of 20000",expected, ii);
        return datasets;
    }
    
    private List<Dataset> doSearch(Connection conn, DcPath root, String pathPattern, Boolean searchFolders, Boolean searchGroups, String queryString, String[] sortFields, int expected) throws SQLException, ParseException, IOException{
        String searchBase = PathUtils.normalizeRegex(GlobToRegex.toRegex(pathPattern,"/"));
        DcPath searchPath = root.resolve(searchBase);
        String[] metaFieldsToRetrieve = null;
        String[] sites = null;
        ContainerVisitor visitor = new ContainerVisitor(root.getFileSystem(), pathPattern, searchGroups, searchFolders);
        System.out.println("With visitor: " + visitor.toString());
        Select statement = datacatSearch.compileStatement( conn, searchPath, visitor, false, 100, queryString, sites, metaFieldsToRetrieve, sortFields,0,-1);
        System.out.println("With statement" + statement.formatted());
        List<Dataset> datasets = datacatSearch.searchForDatasetsInParent( conn, statement);
        int ii = 0;
        for(Dataset d: datasets){
            ii++;
        }
        TestCase.assertEquals("Should have found "+ expected + " datasets out of 20000",expected, ii);
        return datasets;
    }
}
