
package org.srs.datacat.rest.resources;

import com.google.common.base.Optional;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.RequestView;
import org.srs.datacat.rest.BaseResource;
import static org.srs.datacat.rest.BaseResource.OPTIONAL_EXTENSIONS;
import org.srs.datacat.rest.FormParamConverter;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcUriUtils;
import org.srs.datacat.vfs.attribute.ContainerViewProvider;
import org.srs.datacat.vfs.attribute.DatasetViewProvider;
import org.srs.datacat.rest.RestException;
import org.srs.datacat.model.RecordType;
import org.srs.datacat.model.container.ContainerStat;
import org.srs.datacat.model.container.DatasetContainerBuilder;
import org.srs.datacat.model.security.CallContext;
import org.srs.datacat.rest.PATCH;
import org.srs.datacat.vfs.DcFileSystemProvider;

/**
 *
 * @author bvan
 */
@Path("/{containerType: (groups|folders|containers)}" +  OPTIONAL_EXTENSIONS)
public class ContainerResource extends BaseResource {
    
    private String requestPath;
    private String containerType;
    private HashMap<String, List<String>> matrixParams = new HashMap<>();
    
    public ContainerResource(@PathParam("containerType") String contType,
            @PathParam("id") List<PathSegment> pathSegments, @Context UriInfo ui){
        String path = "";
        this.containerType = contType;
        if(pathSegments != null && !pathSegments.isEmpty()){
            for(PathSegment s: pathSegments){
                path = path + "/" + s.getPath();
                matrixParams.putAll(s.getMatrixParameters());
            }   
        } else {
            path = "/";
            for(PathSegment s: ui.getPathSegments()){
                matrixParams.putAll(s.getMatrixParameters());
            }
        }
        requestPath = path;
    }
    
    private final String idRegex = "{id: [%\\w\\d\\-_\\./]+}";
    
    @Context HttpServletRequest httpRequest;

    @GET
    @Path(idRegex)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response getChildren( @DefaultValue("100000") @QueryParam("max") int max,
            @DefaultValue("0") @QueryParam("offset") int offset) throws IOException{

        RecordType type = RecordType.FOLDER; // Folder by default
        if(containerType.equalsIgnoreCase("groups")){
            type = RecordType.GROUP;
        }
        
        RequestView rv = null;
        try {
             rv = new RequestView(type, matrixParams);
        } catch (IllegalArgumentException ex){
            throw new RestException(ex, 400, "Unable to validate request view", ex.getMessage());
        }
        
        java.nio.file.Path containerPath = getProvider().getPath(requestPath);
        
        try {
            if(!getProvider().getFile(containerPath, buildCallContext()).isDirectory()){
                throw new NotDirectoryException(containerPath.toString());
            }
        } catch (NoSuchFileException ex){
             throw new RestException(ex,404, "File doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403, ex.getMessage());
        } catch (NotDirectoryException ex){
            throw new RestException(ex,404,"File exists, but Path is not a container");
        } catch (IOException ex){
            Logger.getLogger(PermissionsResource.class.getName()).log(Level.WARNING, "Unknown exception", ex);
            throw new RestException(ex, 500);
        }
        
        Class<? extends ContainerStat> statType = null;
        if(rv.containsKey( "stat" )){
            statType = getProvider().getModelProvider().getStatByName(rv.get("stat").toLowerCase());
        }
        if(rv.containsKey("children")){
            return childrenView(containerPath, rv, offset, max, statType);
        }
        
        return objectView(containerPath, statType);
    }
    
    @POST
    @Path(idRegex)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response createContainer(MultivaluedMap<String, String> formParams) throws IOException{
        String sParentPath = requestPath;
        RecordType type = RecordType.FOLDER; // Folder by default
        if(containerType.equalsIgnoreCase( "groups")){
            type = RecordType.GROUP;
        }

        DatasetContainerBuilder builder = FormParamConverter.getContainerBuilder( type, formParams );
        java.nio.file.Path parentPath = getProvider().getPath(sParentPath);
        CallContext callContext = buildCallContext();
        java.nio.file.Path targetPath = parentPath.resolve(builder.build().getName());
        builder.path(targetPath.toString());
        
        try {
            getProvider().createDirectory(targetPath, callContext, builder.build());
            return Response.created(DcUriUtils.toFsUri(targetPath.toString())).entity(builder.build()).build();
        } catch (NoSuchFileException ex){
             throw new RestException(ex ,404, "Parent file doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403, ex.getMessage());
        } catch (NotDirectoryException ex){
            throw new RestException(ex, 404, "File exists, but Path is not a container");
        } catch (IOException ex){
            Logger.getLogger(PermissionsResource.class.getName()).log(Level.WARNING, "Unknown exception", ex);
            ex.printStackTrace();
            throw new RestException(ex, 500);
        }
        
    }
    
    @POST
    @Path(idRegex)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response createContainerJson(DatasetContainer container) throws IOException{
        String sParentPath = requestPath;
        RecordType type = RecordType.FOLDER; // Folder by default
        if(containerType.equalsIgnoreCase("groups")){
            type = RecordType.GROUP;
        }
        DatasetContainerBuilder builder = getProvider().getModelProvider().getContainerBuilder().create(container);

        java.nio.file.Path parentPath = getProvider().getPath(sParentPath);
        java.nio.file.Path targetPath = parentPath.resolve(container.getName());
        builder.path(targetPath.toString());
        
        try {
            getProvider().createDirectory(targetPath, buildCallContext(), container);
            return Response.created(DcUriUtils.toFsUri(targetPath.toString()))
                    .entity(builder.build()).build();
        } catch (NoSuchFileException ex){
             throw new RestException(ex ,404, "Parent file doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403, ex.getMessage());
        } catch (NotDirectoryException ex){
            throw new RestException(ex, 404, "File exists, but Path is not a container");
        } catch (IOException ex){
            Logger.getLogger(PermissionsResource.class.getName()).log(Level.WARNING, "Unknown exception", ex);
            ex.printStackTrace();
            throw new RestException(ex, 500);
        }
        
    }
    
    @PATCH
    @Path(idRegex)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response patchDataset(DatasetContainer containerReq) throws IOException{
        java.nio.file.Path targetPath = getProvider().getPath(requestPath);
        try {
            getProvider().patchContainer(targetPath, buildCallContext(), containerReq);
            return objectView(targetPath, null);
        } catch (NoSuchFileException ex) {
            throw new RestException(ex ,404, "Dataset doesn't exist", ex.getMessage());
        } catch (IllegalArgumentException ex){
            throw new RestException(ex, 400, "Unable to validate request view", ex.getMessage());
        } catch (IOException ex){
            Logger.getLogger(PermissionsResource.class.getName()).log(Level.WARNING, "Unknown exception", ex);
            throw new RestException(ex, 500);
        }
    }
    
    @DELETE
    @Path(idRegex)
    public Response deleteContainer() throws IOException{
        java.nio.file.Path dcPath = getProvider().getPath(requestPath);
        CallContext context = buildCallContext();
        try {
            if(!getProvider().getFile(dcPath, context).isDirectory()){
                throw new NoSuchFileException("Path doesn't resolve to a container");
            }
            getProvider().delete(dcPath, context);
            return Response.noContent().build();
        } catch (DirectoryNotEmptyException ex){
            throw new RestException(ex, 409, "Directory not empty");
        } catch (NoSuchFileException ex){
             throw new RestException(ex, 404, "Directory doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403, ex.getMessage());
        } catch (IOException ex){
            Logger.getLogger(PermissionsResource.class.getName()).log(Level.WARNING, "Unknown exception", ex);
            throw new RestException(ex, 500);
        }
    }
    
    public Response objectView(java.nio.file.Path containerPath, Class<? extends ContainerStat> statType) throws IOException{
        DcFile file = getProvider().getFile(containerPath, buildCallContext());
        return Response
                .ok(file.getAttributeView(ContainerViewProvider.class)
                .withView(statType)).build();
    }

    public Response childrenView(java.nio.file.Path containerPath, RequestView rv, int offset, int max, Class<? extends ContainerStat> statType){
        
        boolean withDs = true;
        if(rv.containsKey("datasets")){
            withDs = Boolean.valueOf(rv.get("datasets"));
        }
        
        ArrayList<DatacatNode> retList = new ArrayList<>();
        try (DirectoryStream<java.nio.file.Path> stream = getProvider().newOptimizedDirectoryStream(containerPath, 
                buildCallContext(), DcFileSystemProvider.ACCEPT_ALL_FILTER, Integer.MAX_VALUE, Optional.of(DatasetView.EMPTY))){
            Iterator<java.nio.file.Path> iter = stream.iterator();
            for(int i = 0; iter.hasNext() && retList.size() < max; i++){
                java.nio.file.Path p = iter.next();
                if(i < offset){
                    continue;
                }
                DcFile file = getProvider().getFile(p, buildCallContext());
                if(!withDs && file.isRegularFile()){
                    continue;
                }
                DatacatNode ret;
                if(file.isRegularFile()){
                    ret = file.getAttributeView(DatasetViewProvider.class).withView(rv.getDatasetView(), rv.includeMetadata());
                } else {
                    ret = file.getAttributeView(ContainerViewProvider.class).withView(statType);
                }
                retList.add(ret);
            }
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
        return Response.ok( new GenericEntity<ArrayList<DatacatNode>>(retList){} ).build();
    }

}
