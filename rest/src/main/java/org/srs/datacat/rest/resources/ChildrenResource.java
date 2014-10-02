
package org.srs.datacat.rest.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.srs.datacat.rest.BaseResource;
import org.srs.datacat.model.RequestView;
import org.srs.datacat.rest.resources.PathResource.StatTypeWrapper;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcUriUtils;
import org.srs.datacat.vfs.attribute.ContainerViewProvider;
import org.srs.datacat.vfs.attribute.DatasetViewProvider;
import org.srs.rest.shared.RestException;
import org.srs.vfs.AbstractFsProvider;


/**
 * The children resource will return objects that belong to supplied parent
 * path.
 * @author bvan

@Path("/children")
public class ChildrenResource extends BaseResource {
    
    private final String idRegex = "{id: [\\w\\d\\-_\\./]+}";
    
    /**
     *
     * @param path      Target path of children to search
     * @param withDs    Whether or not to include datasets in results
     * @param statType  Type of stat of child containers
     * @param max       max results of _viewable_ children to return
     * @param offset    offset of first _viewable_ child
     * @param showCount showCount signals to calculate total _viewable_ children count
     * @return

    @GET
    @Path(idRegex)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response getChildren(@PathParam("id") String path, 
        @DefaultValue("true") @QueryParam("datasets") boolean withDs,
        @DefaultValue("none") @QueryParam("stat") StatTypeWrapper statType,
        @DefaultValue("100000") @QueryParam("max") int max,
        @DefaultValue("0") @QueryParam("offset") int offset,
        @DefaultValue("false") @QueryParam("showCount") boolean showCount){

        path = "/" + path;
        DcPath dirPath = getProvider().getPath(DcUriUtils.toFsUri(path, null, "SRS"));
        try {
            Files.readAttributes(dirPath, DcFile.class);
        } catch (FileNotFoundException ex){
             throw new RestException(ex, 404, "File doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
        
        RequestView rv = new RequestView(DatacatObject.Type.DATASET,null);
        List<DatacatObject> retList = new ArrayList<>();
        int count = 0;
        try (DirectoryStream<java.nio.file.Path> stream = getProvider()
                .newOptimizedDirectoryStream(dirPath, AbstractFsProvider.AcceptAllFilter, 
                    max, rv.getDatasetView())){
            Iterator<java.nio.file.Path> iter = stream.iterator();
            
            while(iter.hasNext() && (retList.size() < max || showCount)){
                java.nio.file.Path p = iter.next();
                DcFile file = Files.readAttributes(p, DcFile.class);
                if(!withDs && file.isRegularFile()){
                    continue;
                }
                if(count >= offset && retList.size() < max){
                    DatacatObject ret;
                    if(file.isRegularFile()){
                        try {
                            ret = file.getAttributeView(DatasetViewProvider.class).withView(rv);
                        } catch (FileNotFoundException ex){
                            continue;
                        }
                    } else {
                        ret = file.getAttributeView(ContainerViewProvider.class).withView(statType.getEnum());
                    }
                    retList.add(ret);
                }
                count++;
            }
        } catch (NotDirectoryException ex){
            return Response.status(Status.NOT_FOUND)
                    .entity("File exists, but Path is not a directory").build();
        } catch (IOException ex){
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Error accessing the file system: " + ex.getMessage()).build();
        }
        
        String start = Integer.toString(offset);
        String end = Integer.toString(offset+ (retList.size() - 1));
        String len= showCount ? Integer.toString(count - 1) : "*";
        Response resp = Response
                .ok( new GenericEntity<List<DatacatObject>>(retList) {})
                .header( "Content-Range", String.format("items %s-%s/%s", start, end, len))
                .build();
        return resp;
    }

}
    */