


package org.srs.datacat.rest.resources;

import java.io.IOException;
import java.sql.SQLException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import junit.framework.TestCase;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.RecordType;
import org.srs.datacat.rest.App;
import org.srs.datacat.rest.FormParamConverter;
import org.srs.datacat.test.DbHarness;
import org.srs.datacat.vfs.TestUtils;
import org.srs.vfs.PathUtils;

/**
 *
 * @author bvan
 */
public class ContainerResourceTest extends JerseyTest {
    
    public ContainerResourceTest(){ }
    
    public static void generateFolders(JerseyTest testInstance, int folders) throws IOException{
        String parent = "/testpath";
        // Create 10 folders
        int expectedStatus = 201;
        for(int i = 0; i < folders; i++){
            String name =String.format("folder%05d", i);
            String newPath = PathUtils.resolve(parent, name);
            System.out.println("/folders.txt" + newPath);
            Response resp = testInstance.target("/folders.txt" + parent)
                    .request()
                    .header("authentication", DbHarness.TEST_USER)
                    .post( Entity.form(new Form("name",name)));
            TestCase.assertEquals(resp.readEntity(String.class), expectedStatus, resp.getStatus());
        }
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
                .register(PathResource.class);
        app.property( ServerProperties.TRACING, "ALL");
        for(Resource r: app.getResources()){
            System.out.println(r.getPath());
        }
        return app;
    }

    @Test           
    public void testDispatch(){
        Response resp;
        
        resp = target("/folders.txt/testpath")
                .request()
                .get();
        System.out.println(resp.readEntity(String.class));
        
        // setup more complicated test
        MultivaluedHashMap<String,String> entity = new MultivaluedHashMap<>();
        entity.add( "name", "dispatchTest");
                
        resp = target("/folders.txt/testpath")
                .request()
                .header("authentication", DbHarness.TEST_USER)
                .post(Entity.form( entity ));
        TestCase.assertEquals( Status.CREATED, Status.fromStatusCode(resp.getStatus()));
        
        entity = new MultivaluedHashMap<>();
        entity.add( "name", "dispatchTest2");
        resp = target("/folders.txt/testpath/dispatchTest")
                .request()
                .header("authentication", DbHarness.TEST_USER)
                .post(Entity.form(entity));
        TestCase.assertEquals( Status.CREATED, Status.fromStatusCode(resp.getStatus()));
        
        resp = target("/folders.txt/testpath/dispatchTest/dispatchTest2")
                .request()
                .header("authentication", DbHarness.TEST_USER)
                .get();
        TestCase.assertEquals( Status.OK, Status.fromStatusCode(resp.getStatus()));
        
        // We have a structure we can test with now       
        resp = target("/folders.txt/testpath/dispatchTest%2fdispatchTest2%3bv=2")
                .request()
                .get();
        TestCase.assertEquals("Escaped semicolon should fail", Status.NOT_FOUND, Status.fromStatusCode(resp.getStatus()));
        
        resp = target("/folders.txt/testpath/dispatchTest%2fdispatchTest2;v=0")
                .request()
                .get();
        TestCase.assertEquals("Should have parsed request", Status.OK, Status.fromStatusCode(resp.getStatus()));
        
        resp = target("/folders.txt%2ftestpath%2fdispatchTest%3bdispatchTest2")
                .request()
                .get();
        TestCase.assertEquals("Should have failed to parse resource", Status.NOT_FOUND, Status.fromStatusCode(resp.getStatus()));        
    }
    
    @Test
    public void testCreate() throws IOException{
        Response resp;
        System.out.println("getting /folders.txt/testpath");
        resp = target("/folders.txt/testpath")
                .request()
                .get();
        System.out.println("got:" + resp.readEntity( String.class));
        
        generateFolders(this, 10);
        int expectedStatus = 200;
        String parent = "/testpath";
        for(int i = 0; i < 10; i++){
            String name =String.format("folder%05d", i);
            String newPath = PathUtils.resolve(parent, name);
            resp = target("/folders.txt" + newPath)
                    .request( MediaType.APPLICATION_JSON )
                    .get();
            TestCase.assertEquals(expectedStatus, resp.getStatus());
        }
        
        MultivaluedHashMap<String,String> entity = new MultivaluedHashMap<>();
        entity.add( "name", "createFolderTest");
                
        resp = target("/folders.txt/testpath")
                .request()
                .header("authentication", DbHarness.TEST_USER)
                .post(Entity.form( entity ));
        TestCase.assertEquals( Status.CREATED, Status.fromStatusCode(resp.getStatus()));
        
        System.out.println("getting /folders.txt/testpath/createFolderTest");
        resp = target("/folders.txt/testpath/createFolderTest")
                .request()
                .get();
        System.out.println(resp.readEntity( String.class));
        
        resp = target("/folders.txt/testpath/createFolderTest")
                .request()
                .header("authentication", DbHarness.TEST_USER)
                .post(Entity.form( entity ));
        TestCase.assertEquals( Status.CREATED, Status.fromStatusCode(resp.getStatus()));
        
        resp = target("/folders.txt/testpath/createFolderTest")
            .request()
            .header("authentication", DbHarness.TEST_USER)
            .delete();
        TestCase.assertEquals( Status.CONFLICT, Status.fromStatusCode(resp.getStatus()));
        
        resp = target("/folders.txt/testpath/createFolderTest/createFolderTest")
            .request()
            .header("authentication", DbHarness.TEST_USER)
            .delete();
        TestCase.assertEquals( Status.NO_CONTENT, Status.fromStatusCode(resp.getStatus()));
        
        resp = target("/folders.txt/testpath/createFolderTest")
            .request()
            .header("authentication", DbHarness.TEST_USER)
            .delete();
        TestCase.assertEquals( Status.NO_CONTENT, Status.fromStatusCode(resp.getStatus()));
        
        resp = target("/folders.txt/testpath/createFolderTest")
            .request()
            .header("authentication", DbHarness.TEST_USER)
            .delete();
        TestCase.assertEquals( Status.NOT_FOUND, Status.fromStatusCode(resp.getStatus()));
        
        /*resp = target("/folders.txt/junit/createFolderTest")
            .request()
            .delete();
        TestCase.assertEquals( Status.NO_CONTENT, Status.fromStatusCode(resp.getStatus()));
                */

    }
    
    @Test
    public void testCreateJson() throws IOException {
        MultivaluedHashMap<String,String> entity = new MultivaluedHashMap<>();
        entity.add( "name", "dispatchTest");
        DatasetContainer fold1 = 
                FormParamConverter.getContainerBuilder(RecordType.FOLDER, entity).build();
        
        Response resp = target("/folders.txt/testpath")
                .request()
                .get();
        
        TestCase.assertEquals(Status.OK, Status.fromStatusCode(resp.getStatus()));
        
        resp = target("/folders.json/testpath")
                .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
                .register(App.JacksonFeature.class)
                .property(ClientProperties.FOLLOW_REDIRECTS, "false")
                .request()
                .header("authentication", DbHarness.TEST_USER)
                .post(Entity.entity(fold1, MediaType.APPLICATION_JSON));
        
        TestCase.assertEquals( Status.CREATED, Status.fromStatusCode(resp.getStatus()));    
    }
    
    @Test
    public void testDeleteFolders() throws IOException{
        generateFolders(this, 10);
        Response resp;
        String parent = "/testpath";
        
        int expectedStatus = 200;
        for(int i = 0; i < 10; i++){
            String name =String.format("folder%05d", i);
            String newPath = PathUtils.resolve(parent, name);
            resp = target("/folders.txt" + newPath)
                    .request( MediaType.APPLICATION_JSON )
                    .get();
            TestCase.assertEquals(expectedStatus, resp.getStatus());
        }

        expectedStatus = 204;
        for(int i = 0; i < 10; i++){
            String name =String.format("folder%05d", i);
            String newPath = PathUtils.resolve(parent, name);
            resp = target("/folders.txt" + newPath)
                    .request()
                    .header("authentication", DbHarness.TEST_USER)
                    .delete();
            TestCase.assertEquals(expectedStatus, resp.getStatus());
        }
        
        expectedStatus = 404;
        for(int i = 0; i < 10; i++){
            String name =String.format("folder%05d", i);
            String newPath = PathUtils.resolve(parent, name);
            resp = target("/folders.txt" + newPath)
                    .request( MediaType.APPLICATION_JSON )
                    .get();
            TestCase.assertEquals(resp.getStatus(), expectedStatus);
        }

    }
            
    
}
