
package org.srs.datacat.client.resources;

import com.google.common.base.Optional;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.DatasetModel;

/**
 * Helper class representing the path interface.
 * @author bvan
 */
public class Path {
    
    WebTarget target;

    public Path(WebTarget baseTarget){
        this.target = baseTarget.path("path.json");
    }
    
    public Response getChildren(String path, Optional<String> versionId, Optional<String> site,
        Optional<Integer> offset, Optional<Integer> max){
        return getTarget(target.path(path), versionId, site).matrixParam("children", "")
                .queryParam("offset", offset.orNull())
                .queryParam("max", max.orNull())
                .request()
                .get();
    }
    
    public Response getContainers(String path){
        return target.path(path).matrixParam("children", "containers").request().get();
    }
    
    public Response getObject(String path, Optional<String> versionId, Optional<String> site){
        return getTarget(target.path(path), versionId, site).request()
                .get();
    }
    
    public Response getContainer(String path, Optional<String> versionId, Optional<String> site, Optional<String> stat){
        return getTarget(target.path(path), versionId, site).matrixParam("stat", stat.orNull()).request()
                .get();
    }
    
    public Response patchContainer(String path, Entity<DatasetContainer> payload){
        return getTarget(target.path(path), Optional.<String>absent(), Optional.<String>absent())
                .request().post(payload);
    }
    
    public Response patchDataset(String path, Optional<String> versionId, Optional<String> site, 
            Entity<DatasetModel> payload){
        return getTarget(target.path(path), versionId, site).request().post(payload);
    }
        
    public static WebTarget getTarget(WebTarget base, Optional<String> versionId, Optional<String> site){
        if(versionId.isPresent()){
            base = base.matrixParam("v", versionId.get());
        }
        if(site.isPresent()){
            base = base.matrixParam("s", site.get());
        }
        return base;
    }

}
