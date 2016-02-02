
package org.srs.datacat.client.resources;

import com.google.common.base.Optional;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import static org.srs.datacat.client.resources.Path.getTarget;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.dataset.DatasetLocationModel;

/**
 * Interface to Datasets resource.
 * @author bvan
 */
public class Datasets {
    
    WebTarget target;

    public Datasets(WebTarget baseTarget){
        this.target = baseTarget.path("datasets.json");
    }
    
    /**
     * Create a new dataset.
     * @param path Path of the container to create this dataset in.
     * @param payload Object to create
     */
    public Response mkds(String path, Entity<DatasetModel> payload){
        return Path.getTarget(target.path(path), Optional.<String>absent(), Optional.<String>absent())
                .request().post(payload);
    }
    
    /**
     * Add a location to an existing dataset.
     * @param path Path of the existing dataset
     * @param payload Location to create
     */
    public Response mkloc(String path, Entity<DatasetLocationModel> payload){
        return Path.getTarget(target.path(path), Optional.<String>absent(), Optional.<String>absent())
                .request().post(payload);
    }
    
    public Response patchDataset(String path, Optional<String> versionId, Optional<String> site,
            Entity<DatasetModel> payload){
        return getTarget(target.path(path), versionId, site).request()
                .method("PATCH", payload);
    }
}
