


package org.srs.datacat.rest.resources;

import org.srs.datacat.rest.resources.ContainerResource;
import org.srs.datacat.rest.resources.PathResource;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.srs.datacat.rest.App;

/**
 *
 * @author bvan
 */
public class ContainerResourceTest extends JerseyTest {
    
    public ContainerResourceTest(){ }

    @Override
    protected Application configure(){
        System.out.println("\n\nHELLO\n\n");
        ResourceConfig app = new App().register(ContainerResource.class).register( PathResource.class);
        for(Resource r: app.getResources()){
            System.out.println(r.getPath());
        }
        return app;
    }

    @Test           
    public void testDispatch(){
        Response resp = target("/folder.txt/testpath")
                .request()
                .get();
        System.out.println(resp.readEntity(String.class));
    }
    
}
