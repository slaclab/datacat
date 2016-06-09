/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.srs.datacat.rest.resources;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import junit.framework.TestCase;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.srs.datacat.dao.DAOFactory;
import org.srs.datacat.dao.DAOTestUtils;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetResultSetModel;
import org.srs.datacat.rest.App;
import org.srs.datacat.shared.FlatDataset;
import org.srs.datacat.test.DbHarness;
import org.srs.datacat.test.HSqlDbHarness;
import org.srs.datacat.vfs.TestUtils;

/**
 *
 * @author bvan
 */
public class SearchResourceTest extends JerseyTest{
    
    App app;
    HSqlDbHarness harness = null;
    
    public SearchResourceTest() throws SQLException{

    }
    
    @Override
    protected Application configure(){
        
        DbHarness harness = null;
        try {
            harness = DbHarness.getDbHarness();
        } catch(SQLException ex) {
            System.out.println(ex);

        }
        app = new App(harness.getDataSource(), TestUtils.getLookupService());
        ResourceConfig newApp = app.register(SearchResource.class)
                .register(PathResource.class)
                .register(TestSecurityFilter.class);
        return newApp;
    }

    @Test
    public void testBasicSearch() throws IOException {
        DAOFactory factory = app.getFsProvider().getDaoFactory();
        DAOTestUtils.generateDatasets(factory, 10, 100);
        
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        AnnotationIntrospector pair = new AnnotationIntrospectorPair(primary, secondary);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        for(Map.Entry<Class, Class> e : app.fsProvider.getModelProvider().modelProviders().entrySet()){
            mapper.addMixIn(e.getKey(), e.getValue());
        }
        
        mapper.setAnnotationIntrospector( pair );
        client().register( new JacksonJsonProvider( mapper ) );
        
        Response resp;
        String filter;
        String pathPattern;
        int expected;
        
        List<? super FlatDataset> datasets;
        
        filter = "alpha == 'def'";
        pathPattern = "/testpath/folder00001";
        expected = 25;
        datasets = doSearch(pathPattern, filter, 200);
        TestCase.assertEquals("Expected 25 datasets", 25, datasets.size());
        
        pathPattern = "/testpath/folder0000*";
        filter = "alpha == 'def'";
        expected = 250;
        datasets = doSearch(pathPattern, filter, 200);
        TestCase.assertEquals("Expected 250 datasets", 250, datasets.size());
        
        pathPattern = "/testpath/folder0000*";
        filter = "alpha =~ 'de*'";
        expected = 250;
        datasets = doSearch( pathPattern, filter, 200 );
        TestCase.assertEquals( "Expected 250 datasets", 250, datasets.size() );

        pathPattern = "/testpath/folder0000[1]";
        filter = "alpha == 'def'";
        expected = 25;
        datasets = doSearch( pathPattern, filter, 200 );
        TestCase.assertEquals( "Expected 25 datasets", 25, datasets.size() );

        pathPattern = "/testpath/folder0000*$";
        filter = "alpha == 'def'";
        expected = 250;
        datasets = doSearch( pathPattern, filter, 200 );
        TestCase.assertEquals( "Expected 25 datasets", 250, datasets.size() );

        pathPattern = "/testpath/folder0000[12]$";
        filter = "alpha == 'def'";
        expected = 50;
        datasets = doSearch( pathPattern, filter, 200 );
        TestCase.assertEquals( "Expected 25 datasets", 50, datasets.size() );

        pathPattern = "/testpath/folder0000*^";
        filter = "alpha == 'def'";
        expected = 0;
        datasets = doSearch( pathPattern, filter, 200 );
        TestCase.assertEquals( "Expected 0 datasets, no groups in search path", 0, datasets.size() );

        pathPattern = "/testpath/folder0000*$";
        filter = "alpha == 'def' or num == 3.14159f";
        expected = (25 * 10) + (75 * 3);
        datasets = doSearch( pathPattern, filter, 200 );
        TestCase.assertEquals( "Expected 0 datasets, no groups in search path", expected, datasets.size() );
        

        filter = "alpha == 'def' or num in (0,3.14159f)";
        pathPattern = "/testpath/folder0000*$";
        expected = 25*10;
        for(int j = 0; j < 10; j++){
            Number num = DbHarness.numberMdValues[j%4];
            if(num.equals( new BigDecimal("3.14159")) || num.equals(0)){
                expected += 75;
            }
        }
        datasets = doSearch(pathPattern, filter, 200);
        TestCase.assertEquals(expected, datasets.size());
        
        pathPattern = "/testpath/folder0000*$";
        filter = "alpha == 'def' or num in (3.1414:3.1416)";
        expected = (25*10) + (75*3); // choose 1 mod 4 folders under 10, so 3 will include all
        datasets = doSearch(pathPattern, filter, 200);
        TestCase.assertEquals("Expected 0 datasets, no groups in search path", expected, datasets.size());
        
        pathPattern ="/testpath/*";
        filter = "num == 0 and alpha == 'def'";
        expected = 25 * 3;
        datasets = doSearch(pathPattern, filter, 200);
        for(Object d: datasets){
            System.out.println(d.toString());
        }
        TestCase.assertEquals("Expected 0 datasets, no groups in search path", expected, datasets.size());

    }
    
    private List<DatasetModel> doSearch(String pathPattern, String filter, int status){
        Response resp = target("/search" + pathPattern)
                .queryParam( "filter", filter )
                .request( MediaType.APPLICATION_JSON )
                .get();
        if(status != resp.getStatus()){
            System.out.println(resp.readEntity(String.class));
        }
        
        TestCase.assertEquals(status, resp.getStatus());
        return resp.readEntity(new GenericType<DatasetResultSetModel>(){}).getResults();
    }
    
}
