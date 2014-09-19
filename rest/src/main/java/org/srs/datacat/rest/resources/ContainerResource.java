
package org.srs.datacat.rest.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.attribute.AclEntryPermission;
import java.util.ArrayList;
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
import org.glassfish.jersey.uri.UriComponent;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.rest.App;
import org.srs.datacat.model.RequestView;
import org.srs.datacat.rest.BaseResource;
import org.srs.datacat.rest.FormParamConverter;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.container.BasicStat.StatType;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcUriUtils;
import org.srs.datacat.vfs.attribute.ContainerCreationAttribute;
import org.srs.datacat.vfs.attribute.ContainerViewProvider;
import org.srs.datacat.vfs.attribute.DatasetViewProvider;
import org.srs.rest.shared.RestException;
import org.srs.vfs.PathUtils;

/**
 *
 * @author bvan
 */
@Path("/{containerType: (groups|folders|containers)}")
public class ContainerResource extends BaseResource {
    
    private final String idRegex = "{id: [\\w\\d\\-_\\./]+}";
    
    @Context HttpServletRequest httpRequest;

    @GET
    @Path(idRegex)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response getChildren(@PathParam("containerType") String contType, 
            @PathParam("id") String path,
            @DefaultValue("100000") @QueryParam("max") int max,
            @DefaultValue("0") @QueryParam("offset") int offset) throws IOException{

        path = "/" + path;
        DatacatObject.Type type = DatacatObject.Type.FOLDER; // Folder by default
        if(contType.equalsIgnoreCase( "groups")){
            type = DatacatObject.Type.GROUP;
        }
        
        MultivaluedMap matrixParams = UriComponent.decodeMatrix( path, true );
        /*if(!matrixParams.isEmpty()){
            String p = "/";
            for(PathSegment ps: UriComponent.decodePath( path, true )){
                p = PathUtils.resolve( p, ps.getPath());
            }
            path = p;            
        }*/
        
        RequestView rv = new RequestView(type, matrixParams);
        DcPath containerPath = getProvider().getPath(DcUriUtils.toFsUri(path, null, "SRS"));
        
        try {
            if(!Files.readAttributes(containerPath, DcFile.class).isDirectory()){
                throw new NotDirectoryException(containerPath.toString());
            }
        } catch (FileNotFoundException ex){
             throw new RestException("File doesn't exist", 404);
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (NotDirectoryException ex){
            throw new RestException( "File exists, but Path is not a container", 404);
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
        
        StatType statType = StatType.NONE;
        if(rv.containsKey( "stat" )){
            statType = StatType.valueOf(rv.get("stat").toUpperCase());
        }
        if(rv.containsKey("children")){
            return childrenView(containerPath, rv, offset, max, statType);
        }
        
        return objectView(containerPath, rv, statType);
    }
    
    
    @POST
    @Path(idRegex)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response createContainer(@PathParam("containerType") String contType, 
            @PathParam("id") String parent,
            MultivaluedMap<String, String> formParams) throws IOException{
        String sParentPath = "/" + parent;
        DatacatObject.Type type = DatacatObject.Type.FOLDER; // Folder by default
        if(contType.equalsIgnoreCase( "groups")){
            type = DatacatObject.Type.GROUP;
        }

        DatasetContainer.Builder builder = FormParamConverter.getContainerBuilder( type, formParams );
        DcPath parentPath = getProvider().getPath(DcUriUtils.toFsUri(sParentPath, null, "SRS"));
        DcPath targetPath = parentPath.resolve(builder.name);
        builder.path(targetPath.toString());
        
        ContainerCreationAttribute request = new ContainerCreationAttribute(builder.build());
        
        try {
            getProvider().createDirectory(targetPath, request);
            //System.out.println("req: " + parentPath.resolve(request.value().getName()));
            return Response.created(DcUriUtils.toFsUri(targetPath.toString(), null, "SRS")).entity(builder.build()).build();
        } catch (FileNotFoundException ex){
             throw new RestException("A parent path doesn't exist", 404);
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (NotDirectoryException ex){
            throw new RestException("File exists, but Path is not a container", 404);
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
        
    }
    
    @DELETE
    @Path(idRegex)
    public Response deleteContainer(@PathParam("containerType") String contType, 
            @PathParam("id") String path) throws IOException{
        path = "/" + path;
        DcPath dcPath = getProvider().getPath(DcUriUtils.toFsUri(path, null, "SRS"));
        try {
            Files.delete(dcPath);
            return Response.noContent().build();
        } catch (DirectoryNotEmptyException ex){
            throw new RestException("Directory not empty", 409);
        } catch (FileNotFoundException ex){
             throw new RestException("A parent path doesn't exist", 404);
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
    }
    
    @DELETE
    @Path(idRegex + ";metadata")
    public Response deleteMetadata(@PathParam("containerType") String contType, 
            @PathParam("id") String path) throws IOException{
        System.out.println("try to delete metadata");
        return null;
    }
    
    public Response objectView(DcPath containerPath, RequestView rv, StatType statType) throws IOException{
        DcFile file = Files.readAttributes(containerPath, DcFile.class);
        return Response
                .ok(file.getAttributeView(ContainerViewProvider.class)
                .withView(statType)).build();
    }

    public Response childrenView(DcPath containerPath, RequestView rv, int offset, int max, StatType statType){
        
        boolean withDs = true;
        if(rv.containsKey("datasets")){
            withDs = Boolean.valueOf(rv.get("datasets"));
        }
        
        ArrayList<DatacatObject> retList = new ArrayList<>();
        try (DirectoryStream<java.nio.file.Path> stream = getProvider().newDirectoryStream(containerPath)){
            Iterator<java.nio.file.Path> iter = stream.iterator();
            for(int i = 0; iter.hasNext() && retList.size() < max; i++){
                java.nio.file.Path p = iter.next();
                if(i < offset){
                    continue;
                }
                DcFile file = getProvider().readAttributes(p, DcFile.class);
                if(!withDs && file.isRegularFile()){
                    continue;
                }
                DatacatObject ret;
                if(file.isRegularFile()){
                    ret = file.getAttributeView(DatasetViewProvider.class).withView(rv);
                } else {
                    ret = file.getAttributeView(ContainerViewProvider.class).withView(statType);
                }
                retList.add(ret);
            }
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
        return Response.ok( new GenericEntity<ArrayList<DatacatObject>>(retList){} ).build();
    }

}
