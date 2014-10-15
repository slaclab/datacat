
package org.srs.datacat.rest.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.srs.datacat.rest.BaseResource;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcUriUtils;
import org.srs.datacat.rest.RestException;
import static org.srs.vfs.AbstractFsProvider.AcceptAllFilter;

/**
 * The children resource will return objects that belong to supplied parent
 * path.
 * @author bvan
 */
@Path("/root")
public class RootResource extends BaseResource {
    
    public static RestException wrapForRestException(Exception e){
        if(e instanceof FileNotFoundException){
            return new RestException(e,404);
        } else if(e instanceof SQLException){
            Logger.getLogger(RootResource.class.getName()).log(Level.INFO, "SQL Exception encountered", e);
            return new RestException(e,500);
        } else {
            Logger.getLogger(RootResource.class.getName()).log(Level.INFO, "Unkown Exception encountered", e);
            return new RestException(e,500);
        }
    }
    
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public ArrayList<DatacatObject> getChildren(){
        DcPath dcpath = getProvider().getPath( DcUriUtils.toFsUri("/", null, "SRS"));
        DirectoryStream.Filter filter = AcceptAllFilter;
        try (DirectoryStream<java.nio.file.Path> ds = getProvider().newDirectoryStream(dcpath, filter)){
            ArrayList<DatacatObject> retList = new ArrayList<>();
            for(java.nio.file.Path path : ds){
                retList.add(getProvider().readAttributes(path, DcFile.class).getObject());
            }
            return retList;
        } catch (IOException e){
            throw wrapForRestException(e);
        }
    }
    
}
