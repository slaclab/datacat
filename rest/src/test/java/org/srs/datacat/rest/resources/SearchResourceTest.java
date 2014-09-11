/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.srs.datacat.rest.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import junit.framework.TestCase;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.srs.datacat.rest.App;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.dataset.FlatDataset;
import org.srs.datacat.test.HSqlDbHarness;
import org.srs.datacat.vfs.DcFileSystemProvider;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcUriUtils;
import org.srs.datacat.vfs.TestUtils;

/**
 *
 * @author Brian Van Klaveren<bvan@slac.stanford.edu>
 */
public class SearchResourceTest extends JerseyTest{
    
    App app;
    HSqlDbHarness harness = null;
    
    public SearchResourceTest() throws SQLException{

    }
    
    @Override
    protected Application configure(){
        
        HSqlDbHarness harness = null;
        try {
            harness = new HSqlDbHarness();
        } catch(SQLException ex) {
            System.out.println(ex);

        }
        app = new App(harness.getDataSource());
        ResourceConfig newApp = app.register(SearchResource.class).register( PathResource.class);
        return newApp;
    }

    @Test
    public void testBasicSearch() throws IOException {
        DcFileSystemProvider provider = app.getFsProvider();
        DcPath root = provider.getPath(DcUriUtils.toFsUri( "/", null, "SRS"));
        TestUtils.generateDatasets( root, provider, 10, 100 );
        
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        AnnotationIntrospector pair = new AnnotationIntrospectorPair(primary, secondary);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector( pair );
        client().register( new JacksonJsonProvider( mapper ) );
        
        Response resp;
        List<? super FlatDataset> datasets;
        resp = target("/search/testpath/folder00001")
                .queryParam( "filter", "alpha == 'def'" )
                .request( MediaType.APPLICATION_JSON )
                .get();
        
        datasets = mapper.readValue( resp.readEntity(String.class), new TypeReference<List<FlatDataset>>(){});
        TestCase.assertEquals("Expected 25 datasets", 25, datasets.size());
        
        resp = target("/search/testpath/folder0000*")
                .queryParam( "filter", "alpha == 'def'" )
                .request( MediaType.APPLICATION_JSON )
                .get();
        datasets = mapper.readValue( resp.readEntity(String.class), new TypeReference<List<FlatDataset>>(){});
        TestCase.assertEquals("Expected 250 datasets", 250, datasets.size());
        
        resp = target("/search/testpath/folder0000*")
                .queryParam( "filter", "alpha =~ 'de%'" )
                .request( MediaType.APPLICATION_JSON )
                .get();
        datasets = mapper.readValue( resp.readEntity(String.class), new TypeReference<List<FlatDataset>>(){});
        TestCase.assertEquals("Expected 250 datasets", 250, datasets.size());
        
        /*resp = target(URLEncoder.encode("/search/testpath/folder0000[1]", "UTF-8"))
                .queryParam( "filter", "alpha == 'def'" )
                .request( MediaType.APPLICATION_JSON )
                .get();
        datasets = mapper.readValue( resp.readEntity(String.class), new TypeReference<List<FlatDataset>>(){});
        TestCase.assertEquals("Expected 25 datasets", 25, datasets.size());*/
        
    }
    
}
