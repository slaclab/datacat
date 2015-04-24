
package org.srs.datacat.rest.resources;

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
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcUriUtils;
import org.srs.datacat.vfs.attribute.ContainerCreationAttribute;
import org.srs.datacat.vfs.attribute.ContainerViewProvider;
import org.srs.datacat.vfs.attribute.DatasetViewProvider;
import org.srs.datacat.rest.RestException;
import org.srs.datacat.model.RecordType;
import org.srs.datacat.model.container.ContainerStat;
import org.srs.datacat.model.container.DatasetContainerBuilder;
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
        
        DcPath containerPath = getProvider().getPath(DcUriUtils.toFsUri(requestPath, getUser(), "SRS"));
        
        try {
            if(!getProvider().getFile(containerPath).isDirectory()){
                throw new NotDirectoryException(containerPath.toString());
            }
        } catch (NoSuchFileException ex){
             throw new RestException(ex,404, "File doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (NotDirectoryException ex){
            throw new RestException(ex,404,"File exists, but Path is not a container");
        } catch (IOException ex){
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
        DcPath parentPath = getProvider().getPath(DcUriUtils.toFsUri(sParentPath, getUser(), "SRS"));
        DcPath targetPath = parentPath.resolve(builder.build().getName());
        builder.path(targetPath.toString());
        
        ContainerCreationAttribute request = new ContainerCreationAttribute(builder.build());
        
        try {
            getProvider().createDirectory(targetPath, request);
            //System.out.println("req: " + parentPath.resolve(request.value().getName()));
            return Response.created(DcUriUtils.toFsUri(targetPath.toString(), getUser(), "SRS")).entity(builder.build()).build();
        } catch (NoSuchFileException ex){
             throw new RestException(ex ,404, "Parent file doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (NotDirectoryException ex){
            throw new RestException(ex, 404, "File exists, but Path is not a container");
        } catch (IOException ex){
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

        DcPath parentPath = getProvider().getPath(DcUriUtils.toFsUri(sParentPath, getUser(), "SRS"));
        DcPath targetPath = parentPath.resolve(container.getName());
        builder.path(targetPath.toString());
        
        ContainerCreationAttribute request = new ContainerCreationAttribute(container);
        
        try {
            getProvider().createDirectory(targetPath, request);
            return Response.created(DcUriUtils.toFsUri(targetPath.toString(), getUser(), "SRS"))
                    .entity(builder.build()).build();
        } catch (NoSuchFileException ex){
             throw new RestException(ex ,404, "Parent file doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (NotDirectoryException ex){
            throw new RestException(ex, 404, "File exists, but Path is not a container");
        } catch (IOException ex){
            ex.printStackTrace();
            throw new RestException(ex, 500);
        }
        
    }
    
    @PATCH
    @Path(idRegex)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response patchDataset(DatasetContainer containerReq) throws IOException{
        DcPath targetPath = getProvider().getPath(DcUriUtils.toFsUri(requestPath, getUser(), "SRS"));
        try {
            getProvider().patchContainer(targetPath, containerReq);
            return objectView(targetPath, null);
        } catch (NoSuchFileException ex) {
            throw new RestException(ex ,404, "Dataset doesn't exist", ex.getMessage());
        } catch (IllegalArgumentException ex){
            throw new RestException(ex, 400, "Unable to validate request view", ex.getMessage());
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
    }
    
    @DELETE
    @Path(idRegex)
    public Response deleteContainer() throws IOException{
        DcPath dcPath = getProvider().getPath(DcUriUtils.toFsUri(requestPath, getUser(), "SRS"));
        try {
            if(!getProvider().getFile(dcPath).isDirectory()){
                throw new NoSuchFileException("Path doesn't resolve to a container");
            }
            getProvider().delete(dcPath);
            return Response.noContent().build();
        } catch (DirectoryNotEmptyException ex){
            throw new RestException(ex, 409, "Directory not empty");
        } catch (NoSuchFileException ex){
             throw new RestException(ex, 404, "Directory doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
    }
    
    public Response objectView(DcPath containerPath, Class<? extends ContainerStat> statType) throws IOException{
        DcFile file = getProvider().getFile(containerPath);
        return Response
                .ok(file.getAttributeView(ContainerViewProvider.class)
                .withView(statType)).build();
    }

    public Response childrenView(DcPath containerPath, RequestView rv, int offset, int max, Class<? extends ContainerStat> statType){
        
        boolean withDs = true;
        if(rv.containsKey("datasets")){
            withDs = Boolean.valueOf(rv.get("datasets"));
        }
        
        ArrayList<DatacatNode> retList = new ArrayList<>();
        try (DirectoryStream<DcPath> stream = getProvider().newOptimizedDirectoryStream(containerPath, DcFileSystemProvider.ACCEPT_ALL_FILTER, Integer.MAX_VALUE, DatasetView.EMPTY)){
            Iterator<DcPath> iter = stream.iterator();
            for(int i = 0; iter.hasNext() && retList.size() < max; i++){
                DcPath p = iter.next();
                if(i < offset){
                    continue;
                }
                DcFile file = getProvider().getFile(p.withUser(containerPath.getUserName()));
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
