package org.srs.datacat.client.resources;

import com.google.common.base.Optional;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.srs.datacat.rest.security.AclEntryProxy;

/**
 * Helper class representing the permissions interface.
 *
 * @author bvan
 */
public class Permissions {

    WebTarget target;

    public Permissions(WebTarget baseTarget){
        this.target = baseTarget.path("permissions.json");
    }

    public Response getPermissions(String path, Optional<String> groupSpec){
        return target.path(path)
                .queryParam("subject", groupSpec.isPresent() ? "group" : "user")
                .queryParam("group", groupSpec.orNull())
                .request()
                .get();
    }
    
    public Response getAcl(String path){
        return target.path(path).request().get();
    }
    
    public Response patchAcl(String path, Entity<List<AclEntryProxy>> payload){
        return target.path(path).request()
                .method("PATCH", payload);
    }

}
