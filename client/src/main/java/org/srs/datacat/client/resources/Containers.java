
package org.srs.datacat.client.resources;

import com.google.common.base.Optional;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import static org.srs.datacat.client.resources.Path.getTarget;
import org.srs.datacat.model.DatasetContainer;

/**
 *
 * @author bvan
 */
public class Containers {
    
    WebTarget target;

    public Containers(WebTarget baseTarget){
        this.target = baseTarget.path("containers.json");
    }
        
    public Response getContainer(String path, Optional<String> versionId, Optional<String> site,
            Optional<String> stat){
        return getTarget(target.path(path), versionId, site).matrixParam("stat", stat.orNull()).
                request()
                .get();
    }
    
    public Response createContainer(String path, Entity<DatasetContainer> payload){
        return getTarget(target.path(path), Optional.<String>absent(), Optional.<String>absent())
                .request()
                .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
                .post(payload);
    }

    public Response patchContainer(String path, Entity<DatasetContainer> payload){
        return getTarget(target.path(path), Optional.<String>absent(), Optional.<String>absent())
                .request()
                .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
                .method("PATCH", payload);
    }
}
