package org.srs.datacat.client;

import com.google.common.base.Optional;
import java.net.URI;
import java.util.ArrayList;
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
import org.srs.datacat.client.resources.Containers;
import org.srs.datacat.client.resources.Datasets;
import org.srs.datacat.client.resources.Path;
import org.srs.datacat.client.resources.Permissions;
import org.srs.datacat.client.resources.Search;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetResultSetModel;
import org.srs.datacat.model.ModelProvider;
import org.srs.datacat.model.RecordType;
import org.srs.datacat.model.security.DcAclEntry;
import org.srs.datacat.model.security.DcGroup;
import org.srs.datacat.rest.ErrorResponse;
import org.srs.datacat.rest.JacksonFeature;
import org.srs.datacat.rest.security.AclEntryProxy;
import org.srs.datacat.shared.Provider;
import org.srs.vfs.PathUtils;

/**
 * Main client interface for RESTful API.
 *
 * @author bvan
 */
public class Client {
    private javax.ws.rs.client.Client client;
    private WebTarget baseTarget;
    private Path pathResource;
    private Search searchResource;
    private Datasets datasetsResource;
    private Containers containersResource;
    private Permissions permissionsResource;
    private ModelProvider modelProvider;

    public Client(URI url, List<ClientRequestFilter> requestFilters,
            List<ClientResponseFilter> responseFilters, List<Feature> features,
            Map<String, Object> properties){
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
        this.modelProvider = new Provider();
        ClientBuilder builder = ClientBuilder.newBuilder()
                .register(new JacksonFeature(modelProvider))
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
        this.permissionsResource = new Permissions(baseTarget);
    }

    /**
     * Check if a record exists at given path.
     * @param path
     * @return true or false
     * @throws DcException otherwise, or in the case where record exists but you may not have permission to read it.
     */
    public boolean exists(String path){
        return exists(path, null, null);
    }

    /**
     * Check if a record exists at given path.
     * @param path Path of record
     * @param versionId Optional version to specify
     * @param site Optional site to specify
     * @return true or false
     * @throws DcException otherwise, or in the case where record exists but you may not have permission to read it.
     */
    public boolean exists(String path, String versionId, String site){
        try {
            getObject(path, versionId, site);
            return true;
        } catch(DcClientException ex) {
            if(ex.getType().contains("NoSuchFile")){
                return false;
            }
            throw ex;
        }
    }

    /**
     * Get datasets and containers at given path.
     * @param path Path of container to search into.
     * @return List of DatacatNodes to return.
     */
    public List<DatacatNode> getChildren(String path){
        Response resp = pathResource.getChildren(path, Optional.<String>absent(), Optional.<String>absent(), 
                Optional.<Integer>absent(), Optional.<Integer>absent());
        checkResponse(resp);
        return resp.readEntity(new GenericType<List<DatacatNode>>() {});
    }

    /**
     * Get datasets and containers at given path.
     * @param path Path of container to search into.
     * @param versionId version ID specifier for datasets you want back (null, "current", "0", "1", etc...)
     * @param site site specifier for datasets you want back ("any", "master", "SLAC", etc...) 
     * @return List of DatacatNodes to return.
     */
    public List<DatacatNode> getChildren(String path, String versionId, String site){
        Response resp = pathResource.getChildren(path, Optional.fromNullable(versionId), Optional.fromNullable(site),
                Optional.<Integer>absent(), Optional.<Integer>absent());
        checkResponse(resp);
        return resp.readEntity(new GenericType<List<DatacatNode>>() {});
    }

    /**
     * Get datasets and containers at given path.
     * @param path Path of container to search into.
     * @param versionId version ID specifier for datasets you want back (null, "current", "0", "1", etc...)
     * @param site site specifier for datasets you want back ("any", "master", "SLAC", etc...)
     * @param offset Offset of records
     * @param max Max number of records to return.
     * @return List of DatacatNodes to return.
     */
    public List<DatacatNode> getChildren(String path, String versionId, String site, int offset,
            int max){
        Response resp = pathResource.getChildren(path, Optional.fromNullable(versionId), Optional.fromNullable(site),
                Optional.of(offset), Optional.of(max));
        checkResponse(resp);
        return resp.readEntity(new GenericType<List<DatacatNode>>() {});
    }

    /**
     * Get children containers.
     * @param path Path to search into for child containers.
     */
    public List<DatasetContainer> getContainers(String path){
        Response resp = pathResource.getContainers(path);
        checkResponse(resp);
        return resp.readEntity(new GenericType<List<DatasetContainer>>() {});
    }

    /**
     * Just get an object. If the object is a dataset, you will NOT receive the version, locations, or
     * versionMetadata for that object, only the basic information about it, such as name.
     * @param path Path of object
     */
    public DatacatNode getObject(String path){
        return getObject(path, null, null);
    }

    /**
     * Get an object. 
     * In most cases, you'll want to specify "current" and "all" for the versionId and site, respectively.
     * Doing so guarantees you get information about the version, it's metadata, and all available locations.
     * @param path Path of object
     * @param versionId version ID specifier for datasets you want back (null, "current", "0", "1", etc...)
     * @param site site specifier for datasets you want back ("any", "master", "SLAC", etc...) 
     */
    public DatacatNode getObject(String path, String versionId, String site){
        Response resp = pathResource.getObject(path, Optional.fromNullable(versionId), Optional.fromNullable(site));
        checkResponse(resp);
        return resp.readEntity(new GenericType<DatacatNode>() {});
    }

    /**
     * Get a container and return a specific stat.
     * @param path Path of container we want to get.
     * @param stat Type of stat object to get (none, basic, dataset)
     * @return DatasetContainer at given location.
     */
    public DatasetContainer getContainer(String path, String stat){
        Response resp = containersResource.getContainer(path, Optional.<String>absent(), Optional.<String>absent(),
                Optional.of(stat));
        checkResponse(resp);
        return resp.readEntity(new GenericType<DatasetContainer>() {});
    }
    
    /**
     * Create a container. 
     * @param path Path to place the container.
     * @param payload Representation of the container to be created. Should include name.
     * @throws DcClientException if path doesn't exist
     * @return A representation of the container that was created.
     */
    public DatasetContainer createContainer(String path, DatasetContainer payload){
        return createContainer(path, payload, false);
    }
    
    /**
     * Create a container.
     * @param path Path to place this container in
     * @param payload Representation of the container to be created. Should include name field.
     * @param parents Include parents if they don't exist
     * @throws DcClientException if path doesn't exist
     * @return A representation of the container that was created.
     */
    public DatasetContainer createContainer(String path, DatasetContainer payload, boolean parents){
        if(parents){
            String parentpath = path;
            ArrayList<String> parts = new ArrayList<>();
            while(!exists(parentpath)){
                parts.add(PathUtils.getFileName(parentpath));
                parentpath = PathUtils.getParentPath(parentpath);
            }
            if(parts.size() > 0){
                Collections.reverse(parts);
                for(String part: parts){
                    parentpath = parentpath + "/" + part;
                    DatasetContainer next = (DatasetContainer) modelProvider.getContainerBuilder()
                            .name(part)
                            .type(RecordType.FOLDER)
                            .build();
                    createContainer(parentpath, next, false);
                }
            }
        }
        Response resp = containersResource.createContainer(path, Entity.entity(payload, MediaType.APPLICATION_JSON));
        checkResponse(resp);
        return resp.readEntity(new GenericType<DatasetContainer>() {});
    }
    
    /**
     * Patch the container at path with the changes in the payload object.
     * @param path Path of container to be patched.
     * @param payload A diff representation of the changes you want made to the container.
     * @return An updated representation of the container.
     */
    public DatasetContainer patchContainer(String path, DatasetContainer payload){
        Response resp = containersResource.patchContainer(path, Entity.entity(payload, MediaType.APPLICATION_JSON));
        checkResponse(resp);
        return resp.readEntity(new GenericType<DatasetContainer>() {});
    }

    /**
     * Create a new dataset.
     * @param path Path to place the container the dataset will be placed in.
     * @param payload Representation of the dataset to be created. Must include name.
     * @throws DcClientException if there's no container at given path
     * @return The representation of the dataset that was created.
     */
    public DatasetModel createDataset(String path, DatasetModel payload){
        Response resp = datasetsResource.mkds(path, Entity.entity(payload, MediaType.APPLICATION_JSON));
        checkResponse(resp);
        return resp.readEntity(new GenericType<DatasetModel>() {});
    }

    /**
     * Patch the dataset at given dataset path.
     * @param path Path of dataset to be patched.
     * @param payload A diff representation of the changes you want made to the dataset.
     * @return An updated representation of the container.
     */
    public DatasetModel patchDataset(String path, String versionId, String site,
            DatasetModel payload){
        Response resp = datasetsResource.patchDataset(path, Optional.fromNullable(versionId),
                Optional.fromNullable(site), Entity.entity(payload, MediaType.APPLICATION_JSON));
        checkResponse(resp);
        return resp.readEntity(new GenericType<DatasetModel>() {});
    }

    /**
     * Search a target. A target is a Container of some sort. It may also be specified as a glob, as in:
     * @see #searchForDatasets(java.lang.String, java.lang.String, java.lang.String, java.lang.String, 
     * java.lang.String[], java.lang.String[], java.lang.Integer, java.lang.Integer) 
     * @param target The path (or glob-like path) of which to search
     * @param versionId Version Id to return
     * @param site Site to query
     * @param query Query String
     * @param folderQuery Query string for matching folders
     * @param sort Fields and Metadata fields to sort on.
     * @param show Metadata fields to optionally return
     * @return Response object of the search
     */
    public DatasetResultSetModel searchForDatasets(String target, String versionId, String site,
            String query, String[] sort, String[] show){
        return searchForDatasets(target, versionId, site, query, null, sort, show, null, null);
    }

    /**
     * Search a target. A target is a Container of some sort. It may also be specified as a glob, as in:
     * @see #searchForDatasets(java.lang.String, java.lang.String, java.lang.String, java.lang.String, 
     * java.lang.String[], java.lang.String[], java.lang.Integer, java.lang.Integer) 
     * @param target The path (or glob-like path) of which to search
     * @param versionId Version Id to return
     * @param site Site to query
     * @param query Query String
     * @param folderQuery Query string for matching folders
     * @param sort Fields and Metadata fields to sort on.
     * @param show Metadata fields to optionally return
     * @return Response object of the search
     */
    public DatasetResultSetModel searchForDatasets(String target, String versionId, String site,
            String query, String folderQuery, String[] sort, String[] show){
        return searchForDatasets(target, versionId, site, query, folderQuery, sort, show, null, null);
    }
    
    public DatasetResultSetModel searchForDatasets(String target, String versionId, String site,
            String query, String[] sort, String[] show, Integer offset, Integer max){
        return searchForDatasets(target, versionId, site, query, null, sort, show, null, null);
    }
    
    /**
     * Search a target. A target is a Container of some sort. It may also be specified as a glob, as in: <p>
     *   1. {@code /path/to} - target {@code /path/to} _only_ <p>
     *   2. {@code /path/to/*} - target is all containers directly in {@code /path/to/}<p>
     *   3. {@code /path/to/**} - target is all containers, recursively, under {@code /path/to/} <p>
     *   4. {@code /path/to/*$} - target is only folders directly under {@code /path/to/} <p>
     *   5. {@code /path/to/**^} - target is only groups, recursively, under {@code /path/to/}<p>
     * @param target The path (or glob-like path) of which to search
     * @param versionId Version Id to return
     * @param site Site to query
     * @param query Query String
     * @param folderQuery Query string for matching folders
     * @param sort Fields and Metadata fields to sort on.
     * @param show Metadata fields to optionally return
     * @param offset Offset at which to start returning objects.
     * @param max Maximum number of datasets to return
     * @return Response object of the search
     */
    public DatasetResultSetModel searchForDatasets(String target, String versionId, String site,
            String query, String folderQuery, String[] sort, String[] show, Integer offset, Integer max){
        try {
            Response resp = searchResource.searchForDatasets(target, Optional.fromNullable(versionId),
                    Optional.fromNullable(site), Optional.fromNullable(query),
                    Optional.fromNullable(folderQuery),
                    Optional.fromNullable(sort), Optional.fromNullable(show),
                    Optional.<Integer>fromNullable(offset), Optional.<Integer>fromNullable(max));
            checkResponse(resp);
            return resp.readEntity(new GenericType<DatasetResultSetModel>() {});
        } catch(WebApplicationException ex) {
            throw new DcRequestException(ex);
        }
    }
    
    /**
     * Get the effective permissions for you or a specific group.
     * @param path Path of DatacatNode you want to check permissions for.
     * @param group If group is not null, then return permissions as seen by this group.
     * @return A DcAclEntry of the permissions.
     */
    public DcAclEntry getPermissions(String path, DcGroup group){
        Optional<String> groupSpec = Optional.fromNullable(group != null ? group.toString(): null);
        Response resp = permissionsResource.getPermissions(path, groupSpec);
        checkResponse(resp);
        return resp.readEntity(new GenericType<AclEntryProxy>() {}).entry();
    }
    
    /**
     * Get the ACL of a given object.
     * @param path Datacat object path
     * @return ACL
     */
    public List<DcAclEntry> getAcl(String path){
        Response resp = permissionsResource.getAcl(path);
        checkResponse(resp);
        List<DcAclEntry> ret = new ArrayList<>();
        for(AclEntryProxy p: resp.readEntity(new GenericType<List<AclEntryProxy>>() {})){
            ret.add(p.entry());
        }
        return ret;
    }
    
    /**
     * Modify the ACL of an object.
     * Note: A user must have the Admin permission in order to modify the ACL.
     * @param path Path of a container (not dataset) to modify
     * @param acl A List of entries to be patched.
     * @return The updated ACL
     */
    public List<AclEntryProxy> patchAcl(String path, List<DcAclEntry> acl){
        List<AclEntryProxy> req = new ArrayList<>();
        for(DcAclEntry e: acl){
            req.add(new AclEntryProxy(e));
        }
        Response resp = permissionsResource.patchAcl(path, Entity.entity(req, MediaType.APPLICATION_JSON));
        checkResponse(resp);
        return resp.readEntity(new GenericType<List<AclEntryProxy>>() {});
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

    public Path getPathResource(){
        return pathResource;
    }

    public Search getSearchResource(){
        return searchResource;
    }

    public Datasets getDatasetsResource(){
        return datasetsResource;
    }

    public Containers getContainersResource(){
        return containersResource;
    }

    public Permissions getPermissionsResource(){
        return permissionsResource;
    }
    
}
