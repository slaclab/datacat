/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacatalog.search;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import junit.framework.TestCase;
import org.srs.datacatalog.search.plugins.DatacatPlugin;
import org.srs.datacatalog.search.plugins.EXODatacatSearchPlugin;
import org.srs.datacatalog.search.tables.DatasetVersions;
import org.srs.datahandling.common.util.sql.ConnectionManager;
import org.srs.datahandling.common.util.sql.DataCatConnectionManager;
import org.srs.rest.datacat.shared.DatacatObject;
import org.srs.rest.datacat.shared.Dataset;
import org.srs.rest.datacat.shared.LogicalFolder;

/**
 *
 * @author bvan
 */
public class DatacatSearchTest extends TestCase {
    
    DatacatSearch datacatSearch;
    Connection conn = null;
    HashMap<String, DatacatPlugin> pluginMap;
    
    String query1 = "\nSELECT ds.latestVersion, ds.dataset pk, ds.datasetName name, ds.datasetFileFormat, ds.datasetGroup, ds.datasetDataType, ds.datasetlogicalfolder, v.masterLocation, v.dataset, v.versionId, v.datasetVersion, l.runMin, l.datasetLocation, l.datasetSite, l.checkSum, l.path, l.numberEvents, l.runMax, l.scanstatus, l.fileSizeBytes, 'DATASET' type, CASE WHEN ds.datasetlogicalfolder IS NOT NULL  THEN ds.datasetlogicalfolder ELSE ds.datasetGroup END  parent, dsmv0.metaValue \"nRun\", eri.fullTypeName runType, eri.quality runQuality, eri.runIndex runId, dsmv1.metaValue \"nSegment\"\n" +
                "FROM VerDataset ds\n" +
                "LEFT OUTER JOIN DatasetVersion v ON ( ds.latestVersion = v.datasetVersion )\n" +
                "LEFT OUTER JOIN VerdatasetLocation l ON ( v.masterLocation = l.datasetLocation )\n" +
                "LEFT OUTER JOIN ( SELECT dsmv0.DatasetVersion, dsmv0.metaValue, dsmv0.metaName FROM VerDatasetMetanumber dsmv0 WHERE dsmv0.metaName = 'nRun' ) dsmv0 ON ( l.datasetVersion = dsmv0.DatasetVersion )\n" +
                "LEFT OUTER JOIN EXORunIndex eri ON ( dsmv0.metaValue = eri.runIndex )\n" +
                "LEFT OUTER JOIN ( SELECT dsmv1.DatasetVersion, dsmv1.metaValue, dsmv1.metaName FROM VerDatasetMetanumber dsmv1 WHERE dsmv1.metaName = 'nSegment' ) dsmv1 ON ( l.datasetVersion = dsmv1.DatasetVersion )\n" +
                "WHERE ( eri.runIndex > ? AND eri.quality = ? ) AND dsmv1.metaValue = ?";

    public DatacatSearchTest(String testName) throws SQLException {
        super( testName );
        ConnectionManager c = DataCatConnectionManager.instance();
        conn = c.getConnection();
        pluginMap = new HashMap<>();
        DatacatPlugin exoPlugin = new EXODatacatSearchPlugin();
        pluginMap.put( exoPlugin.getNamespace(), exoPlugin);
    }
    
    @Override
    protected void setUp() throws Exception{
    }

    public void testSomeMethod() throws Exception{

        String query = "exo.runId > 5490 3242 and exo.runQuality = 'GOLDEN' \n"
                        + "and name eq 'newName' and \n"
                        + "(datasetGroup = 38288428 ) \n"
                        + "and ADO15 = d'2012-01-01' \n"
                        + "and ADO15 in (1.23, 2, 5) \n"
                        + "and ADO15 in ('hello','how','are','you') \n"
                        + "and ADO15 in (d'2012-03-23', d'2012-12-23T12:10Z')";
        query = "exo.runId > 5490 and exo.runQuality = 'GOLDEN' and nSegment = 1";
        
        datacatSearch = new DatacatSearch( conn, pluginMap );
        /*datacatSearch.prepareSelection( query );
        
        DatasetVersions select = datacatSearch.getSelectStatement();

        String query1Actual = select.formatted();
        System.out.println( select.formatted());
        System.out.println( query1Actual );
        assertEquals("Checking query 1", query1, query1Actual );*/
        
    }
    
    public void testSearchForDatasetsInParent() throws Exception{

        String basePath;
        boolean recurseFolders = false;
        boolean searchFolders = true;
        boolean searchGroups = true;
        String queryString = "";
        String[] sites = null;
        String[] metaFieldsToRetrieve = null;
        String[] sortFields = null;
        
        DatacatObject parent = new LogicalFolder.Builder().pk( 94L).build();
        datacatSearch = new DatacatSearch( conn, pluginMap );
        queryString = "sIntent == 'run'";
        /*datacatSearch.searchForDatasetsInParent(conn, parent, 
             recurseFolders,  searchFolders,  searchGroups, 
             queryString, sites, metaFieldsToRetrieve,  sortFields);*/
        queryString = "nRun == 239557414";

        List<Dataset> ds = datacatSearch.searchForDatasetsInParent( conn, true, Paths.get("/Data/Flight/Reprocess/P202"), 
                    recurseFolders, searchFolders, searchGroups, queryString, sites, metaFieldsToRetrieve, sortFields );
        System.out.println( ds.size() );
        for(Dataset d: ds){
            System.out.println( d.toString() );
        }
        ds = datacatSearch.searchForDatasetsInParent( conn, true, Paths.get("/Data/Flight/Reprocess/P202/FT2SECONDS"), 
                    recurseFolders, searchFolders, searchGroups, queryString, sites, metaFieldsToRetrieve, sortFields );
        for(Dataset d: ds){
            System.out.println( d.toString() );
        }
        System.out.println(ds.size());

    }
}
