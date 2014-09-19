


package org.srs.datacat.rest.resources;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import junit.framework.TestCase;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.rest.App;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.LogicalFolder;
import org.srs.datacat.test.HSqlDbHarness;
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
                    .post( Entity.form(new Form("name",name)));
            TestCase.assertEquals(resp.readEntity(String.class), expectedStatus, resp.getStatus());
        }
    }

    @Override
    protected Application configure(){
        HSqlDbHarness harness = null;
        try {
            harness = new HSqlDbHarness();
        } catch(SQLException ex) {
            System.out.println(ex);

        }

        ResourceConfig app = new App(harness.getDataSource()).register(ContainerResource.class).register( PathResource.class);
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
        
        resp = target("/folders.txt/testpath;something=hello")
                .request()
                .get();
        System.out.println(resp.readEntity(String.class));
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
            TestCase.assertEquals( resp.getStatus(), expectedStatus);
        }
        
        MultivaluedHashMap<String,String> entity = new MultivaluedHashMap<>();
        entity.add( "name", "createFolderTest");
                
        resp = target("/folders.txt/testpath")
                .request()
                .post(Entity.form( entity ));
        TestCase.assertEquals( Status.CREATED, Status.fromStatusCode(resp.getStatus()));
        
        System.out.println("getting /folders.txt/testpath/createFolderTest");
        resp = target("/folders.txt/testpath/createFolderTest")
                .request()
                .get();
        System.out.println(resp.readEntity( String.class));
        
        resp = target("/folders.txt/testpath/createFolderTest")
                .request()
                .post(Entity.form( entity ));
        TestCase.assertEquals( Status.CREATED, Status.fromStatusCode(resp.getStatus()));
        
        resp = target("/folders.txt/testpath/createFolderTest")
            .request()
            .delete();
        TestCase.assertEquals( Status.CONFLICT, Status.fromStatusCode(resp.getStatus()));
        
        resp = target("/folders.txt/testpath/createFolderTest/createFolderTest")
            .request()
            .delete();
        TestCase.assertEquals( Status.NO_CONTENT, Status.fromStatusCode(resp.getStatus()));
        
        resp = target("/folders.txt/testpath/createFolderTest")
            .request()
            .delete();
        TestCase.assertEquals( Status.NO_CONTENT, Status.fromStatusCode(resp.getStatus()));
        
        resp = target("/folders.txt/testpath/createFolderTest")
            .request()
            .delete();
        TestCase.assertEquals( Status.NOT_FOUND, Status.fromStatusCode(resp.getStatus()));
        
        /*resp = target("/folders.txt/junit/createFolderTest")
            .request()
            .delete();
        TestCase.assertEquals( Status.NO_CONTENT, Status.fromStatusCode(resp.getStatus()));
                */
        
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
            TestCase.assertEquals( resp.getStatus(), expectedStatus);
        }

        expectedStatus = 204;
        for(int i = 0; i < 10; i++){
            String name =String.format("folder%05d", i);
            String newPath = PathUtils.resolve(parent, name);
            resp = target("/folders.txt" + newPath)
                    .request()
                    .delete();
            TestCase.assertEquals( resp.getStatus(), expectedStatus);
        }
        
        expectedStatus = 404;
        for(int i = 0; i < 10; i++){
            String name =String.format("folder%05d", i);
            String newPath = PathUtils.resolve(parent, name);
            resp = target("/folders.txt" + newPath)
                    .request( MediaType.APPLICATION_JSON )
                    .get();
            TestCase.assertEquals( resp.getStatus(), expectedStatus);
        }

    }
            
    
}
