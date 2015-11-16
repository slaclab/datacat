
package org.srs.datacat.client;

import com.google.common.base.Optional;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.srs.datacat.client.exception.DcClientException;
import org.srs.datacat.client.exception.DcException;
import org.srs.datacat.client.exception.DcRequestException;
import org.srs.datacat.client.impl.JacksonFeature;
import org.srs.datacat.client.resources.Containers;
import org.srs.datacat.client.resources.Datasets;
import org.srs.datacat.client.resources.Path;
import org.srs.datacat.client.resources.Search;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetResultSetModel;
import org.srs.datacat.rest.ErrorResponse;

/**
 * Main client interface for RESTful API.
 * @author bvan
 */
public class Client {
    private javax.ws.rs.client.Client client;
    private WebTarget baseTarget;
    private Path pathResource;
    private Search searchResource;
    private Datasets datasetsResource;
    private Containers  containersResource;
    
    public Client(URI url, List<ClientRequestFilter> requestFilters, 
            List<ClientResponseFilter> responseFilters, List<Feature> features, 
            Map<String, Object> properties) {
        init(url, requestFilters, responseFilters, features, properties);
    }
    
    public Client(URI url){
        init(url,
                Collections.<ClientRequestFilter>emptyList(),
                Collections.<ClientResponseFilter>emptyList(),
                Collections.<Feature>emptyList(),
                Collections.<String, Object>emptyMap());
    }
        
    private void init(URI baseUrl, List<ClientRequestFilter> requestFilters, 
            List<ClientResponseFilter> responseFilters, List<Feature> features, 
            Map<String, Object> properties){
        ClientBuilder builder = ClientBuilder.newBuilder()
                .register(new JacksonFeature())
                .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
        
        for(ClientRequestFilter filter: requestFilters){
            builder.register(filter);
        }
        
        for(ClientResponseFilter filter: responseFilters){
            builder.register(filter);
        }
        
        for(Feature feature: features){
            builder.register(feature);
        }

        for(Entry<String, Object> e: properties.entrySet()){
            builder.property(e.getKey(), e.getValue());
        }
        
        this.client = builder.build();
        this.baseTarget = client.target(baseUrl);
        this.pathResource = new Path(baseTarget);
        this.searchResource = new Search(baseTarget);
        this.datasetsResource = new Datasets(baseTarget);
        this.containersResource = new Containers(baseTarget);
    }
    
    public List<DatacatNode> getChildren(String path){
        Response resp = pathResource.getChildren(path, Optional.<String>absent(), Optional.<String>absent(),
                Optional.<Integer>absent(), Optional.<Integer>absent());
        checkResponse(resp);
        return resp.readEntity(new GenericType<List<DatacatNode>>(){});
    }
    
    public List<DatacatNode> getChildren(String path, String versionId, String site){
        Response resp = pathResource.getChildren(path, Optional.fromNullable(versionId), Optional.fromNullable(site),
                Optional.<Integer>absent(), Optional.<Integer>absent());
        checkResponse(resp);
        return resp.readEntity(new GenericType<List<DatacatNode>>(){});
    }
    
    public List<DatacatNode> getChildren(String path, String versionId, String site, int offset, int max){
        Response resp = pathResource.getChildren(path, Optional.fromNullable(versionId), Optional.fromNullable(site),
                Optional.of(offset), Optional.of(max));
        checkResponse(resp);
        return resp.readEntity(new GenericType<List<DatacatNode>>(){});
    }
    
    public List<DatacatNode> getContainers(String path){
        Response resp = pathResource.getContainers(path);
        checkResponse(resp);
        return resp.readEntity(new GenericType<List<DatacatNode>>(){});
    }
    
    public DatacatNode getObject(String path){
        Response resp = pathResource.getObject(path, Optional.fromNullable("current"), Optional.fromNullable("all"));
        checkResponse(resp);
        return resp.readEntity(new GenericType<DatacatNode>(){});
    }
    
    public DatacatNode getContainer(String path, String stat){
        Response resp = containersResource.getContainer(path, Optional.<String>absent(), Optional.<String>absent(),
                Optional.of(stat));
        checkResponse(resp);
        return resp.readEntity(new GenericType<DatacatNode>(){});
    }
    
    public DatacatNode patchContainer(String path, DatasetContainer payload){
        Response resp = containersResource.patchContainer(path, 
                Entity.entity(payload, MediaType.APPLICATION_JSON));
        checkResponse(resp);
        return resp.readEntity(new GenericType<DatacatNode>(){});
    }
    
    public DatacatNode patchDataset(String path, String versionId, String site, 
            DatasetModel payload){
        Response resp = datasetsResource.patchDataset(path, Optional.fromNullable(versionId), 
                Optional.fromNullable(site), Entity.entity(payload, MediaType.APPLICATION_JSON));
        checkResponse(resp);
        return resp.readEntity(new GenericType<DatacatNode>(){});
    }
    
    public DatasetResultSetModel searchForDatasets(String target, String versionId, String site, 
            String query, String[] sort, String show, Integer offset, Integer max){
        try {
            Response resp = searchResource.searchForDatasets(target, Optional.fromNullable(versionId), 
                Optional.fromNullable(site), Optional.fromNullable(query), 
                Optional.fromNullable(sort), Optional.fromNullable(show), 
                Optional.<Integer>fromNullable(offset), Optional.<Integer>fromNullable(max));
            checkResponse(resp);
            return resp.readEntity(new GenericType<DatasetResultSetModel>(){});
        } catch (WebApplicationException ex){
            throw new DcRequestException(ex);
        }
    }

    public static void checkResponse(Response resp) throws DcException{
        if(resp.getStatusInfo().getFamily() == Status.Family.SUCCESSFUL){
            return;
        }
        // Assume an error we can parse.
        if(MediaType.APPLICATION_JSON_TYPE.isCompatible(resp.getMediaType())){
            ErrorResponse err = resp.readEntity(ErrorResponse.class);
            throw new DcClientException(err.getType(), err.getMessage(), 
                    err.getCause(), err.getCode(), resp.getStatus());
        }

        if(resp.getStatusInfo().getFamily() == Status.Family.CLIENT_ERROR){
            throw new DcRequestException(String.format("Unknown HTTP Client Error (%d)", resp.getStatus()), resp);
        }
        if(resp.getStatusInfo().getFamily() == Status.Family.SERVER_ERROR){
            throw new DcRequestException(String.format("Unknown HTTP Server Error (%d)", resp.getStatus()), resp);
        }
        throw new DcRequestException("Unknown HTTP Error", resp);
    }

}
