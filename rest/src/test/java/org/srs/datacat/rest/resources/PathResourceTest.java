package org.srs.datacat.rest.resources;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import java.io.IOException;
import java.sql.SQLException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import junit.framework.TestCase;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.srs.datacat.rest.App;
import static org.srs.datacat.rest.resources.DatasetsResourceTest.generateFoldersAndDatasetsAndVersions;
import org.srs.datacat.test.DbHarness;
import org.srs.datacat.vfs.TestUtils;

/**
 *
 * @author bvan
 */
public class PathResourceTest extends JerseyTest {
    
    static final ObjectMapper mdMapper = new ObjectMapper();

    static {
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        AnnotationIntrospector pair = new AnnotationIntrospectorPair(primary, secondary);
        mdMapper.setAnnotationIntrospector( pair );
    }
    
    public PathResourceTest(){
    }
    
    @Override
    protected Application configure(){
        DbHarness harness = null;
        try {
            harness = DbHarness.getDbHarness();
        } catch(SQLException ex) {
            System.out.println(ex);

        }

        ResourceConfig app = new App(harness.getDataSource(), TestUtils.getLookupService())
                .register(TestSecurityFilter.class)
                .register(ContainerResource.class)
                .register(PathResource.class)
                .register(DatasetsResource.class);
        app.property( ServerProperties.TRACING, "ALL");
        for(Resource r: app.getResources()){
            System.out.println(r.getPath());
        }
        return app;
    }

    @Test
    public void testGetRootChildren() throws IOException{
        String expected = 
                "[{\"_type\":\"folder\",\"name\":\"testpath\",\"path\":\"/testpath\",\"pk\":1,\"parentPk\":0}]";
        Response resp = target("/path.json;children")
                .request()
                .header("authentication", DbHarness.TEST_USER)
                .get();
        TestCase.assertEquals(200, resp.getStatus());
        TestCase.assertEquals(expected, resp.readEntity(String.class));
        
        resp = target("/path.json/;children")
                .request()
                .header("authentication", DbHarness.TEST_USER)
                .get();
        TestCase.assertEquals(200, resp.getStatus());
        TestCase.assertEquals(expected, resp.readEntity(String.class));
        
        resp = target("/path.json//;children")
                .request()
                .header("authentication", DbHarness.TEST_USER)
                .get();
        TestCase.assertEquals(200, resp.getStatus());
        TestCase.assertEquals(expected, resp.readEntity(String.class));
        
        resp = target("/path.json/;children=containers")
                .request()
                .header("authentication", DbHarness.TEST_USER)
                .get();
        TestCase.assertEquals(200, resp.getStatus());
        TestCase.assertEquals(expected, resp.readEntity(String.class));
    }

    @Test
    public void testGetChildren() throws IOException{
        generateFoldersAndDatasetsAndVersions(this, 10, 10);
        Response resp = target("/path.json/testpath/folder00004;children")
                .request()
                .header("authentication", DbHarness.TEST_USER)
                .get();
        TestCase.assertEquals(200, resp.getStatus());
        String content = resp.readEntity(String.class);
        TestCase.assertTrue(content.startsWith("[{\"_type\":\"dataset\""));
        
        resp = target("/path.json/testpath/folder00004;children=")
                .request()
                .header("authentication", DbHarness.TEST_USER)
                .get();
        
        TestCase.assertTrue(content.equals(resp.readEntity(String.class)));
        TestCase.assertEquals(200, resp.getStatus());
    }
    
    @Test
    public void testGetChildContainers() throws IOException{
        generateFoldersAndDatasetsAndVersions(this, 10, 10);
        
        // no containers
        Response resp = target("/path.json/testpath/folder00004;children=containers")
                .request()
                .header("authentication", DbHarness.TEST_USER)
                .get();
        TestCase.assertEquals(200, resp.getStatus());
        TestCase.assertEquals("[]", resp.readEntity(String.class));
        
        // should have containers
        resp = target("/path.json/testpath;children=containers")
                .request()
                .header("authentication", DbHarness.TEST_USER)
                .get();
        TestCase.assertEquals(200, resp.getStatus());
        String content = resp.readEntity(String.class);
        TestCase.assertTrue(content.startsWith("[{\"_type\":\"folder\""));
    }
    
    @Test
    public void testHead() throws IOException{
        generateFoldersAndDatasetsAndVersions(this, 5, 5);
        Response resp = target("/path.json/testpath/folder00004")
                .request()
                .header("authentication", DbHarness.TEST_USER)
                .head();
        TestCase.assertEquals(200, resp.getStatus());        
        
        resp = target("/path.json/testpath/folder0000X")
                .request()
                .header("authentication", DbHarness.TEST_USER)
                .head();
        TestCase.assertEquals(404, resp.getStatus());
    }

    
}
