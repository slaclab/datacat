
package org.srs.datacat.dao.sql.search;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.sql.DataSource;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.srs.datacat.dao.DAOFactory;
import org.srs.datacat.dao.DAOTestUtils;
import org.srs.datacat.dao.sql.mysql.DAOFactoryMySQL;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.dao.sql.search.plugins.DatacatPlugin;
import org.srs.datacat.shared.Provider;
import org.srs.datacat.test.DbHarness;

/**
 *
 * @author bvan
 */
public class DatacatSearchTest {
    
    static DbHarness harness;
    static DataSource ds = null;
    static DAOFactory factory;
    DatasetSearch datacatSearch;
    Class<? extends DatacatPlugin>[] plugins;
    
    
    public DatacatSearchTest() throws SQLException, IOException {
        plugins = new Class[]{};
    }
        
    @BeforeClass
    public static void setUpDb() throws SQLException, IOException{        
        harness = DbHarness.getDbHarness();
        harness.getDataSource();
        ds = harness.getDataSource();
        factory = new DAOFactoryMySQL(ds);
        DAOTestUtils.generateDatasets(factory, 20, 1000);
    }
    
    @Test
    public void testSearchForDatasetsInParent() throws Exception{

        List<DatacatNode> folders = DAOTestUtils.getFolders(factory, 20);
        Connection conn = ds.getConnection();
        conn.commit();
        conn.close();
        conn = ds.getConnection();
        
        datacatSearch = new DatasetSearch(conn, new Provider(), plugins);        
        
        LinkedList<DatacatNode> folder00000 = new LinkedList(Arrays.asList(folders.get(0)));
        LinkedList<DatacatNode> folder00001 = new LinkedList(Arrays.asList(folders.get(1)));
        LinkedList<DatacatNode> folder00002 = new LinkedList(Arrays.asList(folders.get(2)));
        LinkedList<DatacatNode> folder00003 = new LinkedList(Arrays.asList(folders.get(3)));
        LinkedList<DatacatNode> firstTen = new LinkedList(folders.subList(0, 10));
        
        List<DatasetModel> datasets;
        String queryString = "alpha == 'def'";
        // Search pattern "/testpath/folder00001"
        datasets = doSearch(Lists.newLinkedList(folder00001), queryString, null, 250 );
        TestCase.assertEquals("First dataset found incorrect", "dataset00001", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "alpha == 'def'";
        // Search pattern "/testpath/folder0000*"
        datasets = doSearch(Lists.newLinkedList(firstTen), queryString, null, 10*250 );
        TestCase.assertEquals("First dataset found incorrect", "dataset00001", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "alpha == 'def'";
        // Search pattern "/testpath/folder0000[1]"
        datasets = doSearch(Lists.newLinkedList(folder00001), queryString, null, 250 );
        TestCase.assertEquals("First dataset found incorrect", "dataset00001", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "num == false";
        // Search pattern "/testpath/folder00000"
        datasets = doSearch(Lists.newLinkedList(folder00000), queryString, null, 1000);
        TestCase.assertEquals("First dataset found incorrect", "dataset00000", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "num is null";
        // Search pattern "/testpath/folder00000"
        doSearch(Lists.newLinkedList(folder00000), queryString, null, 0);
        conn.commit(); // Remove from parents on commit
        
        queryString = "num is not null";
        // Search pattern "/testpath/folder00000"
        datasets = doSearch(Lists.newLinkedList(folder00000), queryString, null, 1000);
        TestCase.assertEquals("First dataset found incorrect", "dataset00000", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "num == true";
        // Search pattern "/testpath/folder00001"
        doSearch(Lists.newLinkedList(folder00001), queryString, null, 0);
        conn.commit(); // Remove from parents on commit
        
        queryString = "num == true";
        // Search pattern "/testpath/folder00003"
        doSearch(Lists.newLinkedList(folder00003), queryString, null, 0);
        conn.commit(); // Remove from parents on commit
        
        queryString = "num == 0";
        // Search pattern "/testpath/folder00000"
        doSearch(Lists.newLinkedList(folder00000), queryString, null, 1000);
        conn.commit(); // Remove from parents on commit

        queryString = "num == 0";
        // Search pattern "/testpath/folder00001"
        doSearch(Lists.newLinkedList(folder00001), queryString, null, 0);
        conn.commit(); // Remove from parents on commit
                
        queryString = "num == 3.14159F";
        // Search pattern "/testpath/folder00001"
        doSearch(Lists.newLinkedList(folder00001), queryString, null, 1000);
        conn.commit(); // Remove from parents on commit
        
        queryString = "num == 3.14159F";
        // Search pattern "/testpath/folder00000"
        doSearch(Lists.newLinkedList(folder00000), queryString, null, 0);
        conn.commit(); // Remove from parents on commit
        
        queryString = "num == 4294967296";
        // Search pattern "/testpath/folder00002"
        doSearch(Lists.newLinkedList(folder00002), queryString, null, 1000);
        conn.commit(); // Remove from parents on commit

        queryString = "num is not null";
        // Search pattern "/testpath/folder00001"
        doSearch(Lists.newLinkedList(folder00001), queryString, null, 1000);
        //TestCase.fail( "Should fail on null queries");
        conn.commit(); // Remove from parents on commit
        
        queryString = "alpha == 'def'";
        // Search pattern "/testpath/folder0000*$"
        datasets = doSearch(Lists.newLinkedList(firstTen), queryString, null,2500);
        TestCase.assertEquals("First dataset found incorrect", "dataset00001", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "alpha == 'def'";
        // Search pattern "/testpath/folder0000*^"
        doSearch(Lists.<DatacatNode>newLinkedList(), queryString, null,0);
        conn.commit(); // Remove from parents on commit
               
        queryString = "alpha =~ 'de?'";
        // Search pattern "/testpath/folder00001"
        datasets = doSearch(Lists.newLinkedList(folder00001), queryString, null, 250);
        TestCase.assertEquals("First dataset found incorrect", "dataset00001", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "alpha =~ 'de\\?'";
        // Search pattern "/testpath/folder00001"
        doSearch(Lists.newLinkedList(folder00001), queryString, null, 0);
        conn.commit(); // Remove from parents on commit
        
        queryString = "alpha !~ 'd*'";
        // Search pattern "/testpath/folder00001"
        datasets = doSearch(Lists.newLinkedList(folder00001), queryString, null, 750);
        TestCase.assertEquals("First dataset found incorrect", "dataset00002", datasets.get(1).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "alpha !~ 'de?'";
        // Search pattern "/testpath/folder00001"
        datasets = doSearch(Lists.newLinkedList(folder00001), queryString, null, 750);
        TestCase.assertEquals("First dataset found incorrect", "dataset00002", datasets.get(1).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "alpha =~ 'de_'";
        // Search pattern "/testpath/folder00001"
        doSearch(Lists.newLinkedList(folder00001), queryString, null, 0);
        conn.commit(); // Remove from parents on commit
        
        int expected;
        queryString = "alpha == 'def' or num == 3.14159f";
        // Search pattern "/testpath/folder0000*$"
        expected = (250*10) + (750*3); // choose 1 mod 4 folders under 10, so 3 will include all
        datasets = doSearch(Lists.newLinkedList(firstTen), queryString, null, expected);
        TestCase.assertEquals("First dataset found incorrect", "dataset00001", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "alpha == 'def' or num in (0,3.14159f)";
        // Search pattern "/testpath/folder0000*$"
        expected = 250*10 + 750*3 + 750*3;
        datasets = doSearch(Lists.newLinkedList(firstTen), queryString, null, expected);
        TestCase.assertEquals("First dataset found incorrect", "dataset00000", datasets.get(0).getName());
        conn.commit(); // Remove from parents on commit
        
        queryString = "alpha == 'def' or num in (3.1414:3.1416)";
        // Search pattern "/testpath/folder0000*$"
        expected = (250*10) + (750*3); // choose 1 mod 4 folders under 10, so 3 will include all
        doSearch(Lists.newLinkedList(firstTen), queryString, null, expected);
        conn.commit(); // Remove from parents on commit
        
        queryString = "num == 0 and alpha == 'def'";
        // Search pattern "/testpath/*"
        
        doSearch(Lists.newLinkedList(DAOTestUtils.getContainers(factory)), queryString, null, 1250);
        conn.commit(); // Remove from parents on commit
        conn.close();
    }

    @Test
    public void testWithSortFields() throws IOException, SQLException, ParseException{
            
        Connection conn = ds.getConnection();
        conn.commit();
        conn.close();
        conn = ds.getConnection();
        String queryString = null;
        List<DatasetModel> datasets;
        
        datacatSearch = new DatasetSearch(conn, new Provider(), plugins);
            
        String[] metaFieldsToRetrieve = null;
        String[] sortFields = null;
        String[] sites = null;
        
        // Test sortFields
        sortFields = new String[]{"num", "alpha"};
        queryString = "num == 0 and alpha == 'def'";
        // Search pattern "/testpath/*"
        doSearch(Lists.newLinkedList(DAOTestUtils.getContainers(factory)), queryString, sortFields, 1250);
        conn.commit(); // Remove from parents on commit
        
        DatasetView dsView = DatasetView.CURRENT_ALL;
        queryString = "nRun == 239557414";
        
        
        datacatSearch.compileStatement(Lists.newLinkedList(DAOTestUtils.getContainers(factory)), 
                dsView,
                Optional.fromNullable(queryString),
                Optional.fromNullable(metaFieldsToRetrieve),
                Optional.fromNullable(sortFields));
        
        conn.commit(); // Remove from parents on commit
        
        queryString = "sIntent == 'run'";
        
        datacatSearch.compileStatement(Lists.newLinkedList(DAOTestUtils.getContainers(factory)), 
                dsView,
                Optional.fromNullable(queryString),
                Optional.fromNullable(metaFieldsToRetrieve),
                Optional.fromNullable(sortFields));
        
        conn.commit(); // Remove from parents on commit
        
        conn.close();
    }
    
    private List<DatasetModel> doSearch(LinkedList<DatacatNode> searchContainers, String queryString, 
            String[] sortFields, int expected) throws SQLException, ParseException, IOException{
        String[] metaFieldsToRetrieve = null;
        String[] sites = null;
        DatasetView dsView = DatasetView.CURRENT_ALL;
        
        datacatSearch.compileStatement(searchContainers, dsView, 
                Optional.fromNullable(queryString), 
                Optional.fromNullable(metaFieldsToRetrieve), 
                Optional.fromNullable(sortFields));
        try (DirectoryStream<DatasetModel> stream = datacatSearch.retrieveDatasets()){
            Iterator<DatasetModel> iter = stream.iterator();
            List<DatasetModel> datasets = new ArrayList<>();
            while(iter.hasNext()){
                datasets.add(iter.next());
            }
            int ii = 0;
            for(DatasetModel d: datasets){
                ii++;
            }
            TestCase.assertEquals("Should have found "+ expected + " datasets out of 20000",expected, ii);
            return datasets;
        }
    }

    @Test
    public void testErrorString() throws Exception {
        List<DatacatNode> folders = DAOTestUtils.getFolders(factory, 20);
        Connection conn = ds.getConnection();
        conn.commit();
        conn.close();
        conn = ds.getConnection();
        
        datacatSearch = new DatasetSearch(conn, new Provider(), plugins);        
        
        LinkedList<DatacatNode> folder00001 = new LinkedList(Arrays.asList(folders.get(1)));
        
        
        String queryString = "x == 'de'";
        try {
            doSearch(Lists.newLinkedList(folder00001), queryString, null, 250 );
            TestCase.fail("should have produced an error");
        } catch (IllegalArgumentException ex){
            TestCase.assertTrue("Error string is incorrect", 
                    ex.getMessage().contains("Unable to resolve 'x'"));
        } finally {
            conn.commit(); // Remove from parents on commit
        }
        
    }
    
}
