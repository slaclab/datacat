/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import static org.junit.Assert.*;
import org.srs.datacat.rest.App;
import org.srs.datacat.shared.Provider;
import org.srs.datacat.test.DbHarness;
import org.srs.datacat.vfs.TestUtils;

/**
 *
 * @author bvan
 */
public class PermissionsResourceTest extends JerseyTest {
    private final Provider modelProvider = new Provider();
    
    public PermissionsResourceTest(){ }
    static final ObjectMapper mdMapper = new ObjectMapper();

    static {
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        AnnotationIntrospector pair = new AnnotationIntrospectorPair(primary, secondary);
        mdMapper.setAnnotationIntrospector( pair );
    }
    
    @Override
    protected Application configure(){
        DbHarness harness = null;
        try {
            harness = DbHarness.getDbHarness();
        } catch(SQLException ex) {
            System.out.println(ex);

        }
        
        ResourceConfig app = new App(harness.getDataSource(), modelProvider, TestUtils.getLookupService())
                .register(TestSecurityFilter.class)
                .register(ContainerResource.class)
                .register(PathResource.class)
                .register(DatasetsResource.class)
                .register(PermissionsResource.class);
        app.property( ServerProperties.TRACING, "ALL");
        for(Resource r: app.getResources()){
            System.out.println(r.getPath());
        }
        return app;
    }
    
    @Test
    public void testQuery() throws IOException{
        Response resp;
        resp = target("/permissions.json/testpath")
                .request()
                .get();
        assertEquals(200, resp.getStatus());
    }
}
