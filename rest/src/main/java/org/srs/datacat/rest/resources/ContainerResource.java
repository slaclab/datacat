
package org.srs.datacat.rest.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import org.srs.datacat.rest.App;
import org.srs.datacat.model.RequestView;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.container.BasicStat.StatType;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcUriUtils;
import org.srs.datacat.vfs.attribute.ContainerViewProvider;
import org.srs.datacat.vfs.attribute.DatasetViewProvider;
import org.srs.rest.shared.RestException;

/**
 *
 * @author bvan
 */
@Path("/{containerType: (group|folder)}")
public class ContainerResource {
    
    private final String idRegex = "{id: [\\w\\d\\-_\\./]+}";
    
    @GET
    @Path(idRegex)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response getChildren(@PathParam("containerType") String cont, 
            @PathParam("id") PathSegment pathSegment,
            @DefaultValue("100000") @QueryParam("max") int max,
            @DefaultValue("0") @QueryParam("offset") int offset) throws IOException{

        String path = pathSegment.getPath();
        RequestView rv = new RequestView(DatacatObject.Type.valueOf(cont.toUpperCase()), pathSegment.getMatrixParameters());
        path = "/" + path;
        DcPath containerPath = App.fsProvider.getPath(DcUriUtils.toFsUri(path, null, "SRS"));
        
        try {
            Files.readAttributes(containerPath, DcFile.class);
        } catch (FileNotFoundException ex){
             throw new RestException("File doesn't exist", 404);
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
        StatType statType = StatType.NONE;
        if(rv.containsValue( "stat" )){
            statType = StatType.valueOf(rv.get("stat").toUpperCase());
        }
        if(rv.containsKey("children")){
            return childrenView(containerPath, rv, offset, max, statType);
        }
        
        return objectView(containerPath, rv, statType);
    }
    
    
    public Response objectView(DcPath containerPath, RequestView rv, StatType statType) throws IOException{
        DcFile file = Files.readAttributes(containerPath, DcFile.class);
        return Response
                .ok(file.getAttributeView(ContainerViewProvider.class)
                .withView(statType)).build();
    }

    public Response childrenView(DcPath containerPath, RequestView rv, int offset, int max, StatType statType){
        
        boolean withDs = true;
        if(rv.containsValue("datasets")){
            withDs = Boolean.valueOf(rv.get("datasets"));
        }
        
        ArrayList<DatacatObject> retList = new ArrayList<>();
        try (DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(containerPath)){
            Iterator<java.nio.file.Path> iter = stream.iterator();
            for(int i = 0; iter.hasNext() && retList.size() < max; i++){
                java.nio.file.Path p = iter.next();
                if(i < offset){
                    continue;
                }
                DcFile file = Files.readAttributes(p, DcFile.class);
                if(!withDs && file.getDatacatObject().isType(DatacatObject.Type.DATASET)){
                    continue;
                }
                DatacatObject ret;
                if(file.getDatacatType() == DatacatObject.Type.DATASET){
                    ret = file.getAttributeView(DatasetViewProvider.class).withView(rv);
                } else {
                    ret = file.getAttributeView(ContainerViewProvider.class).withView(statType);
                }
                retList.add(ret);
            }
        } catch (NotDirectoryException ex){
            throw new RestException( "File exists, but Path is not a directory", 404);
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
        return Response.ok( retList ).build();
    }

}
