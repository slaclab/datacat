/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacat.rest.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.srs.datacat.rest.ConnectionResource;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.sql.ContainerDAO;
import org.srs.rest.shared.RestException;

/**
 * The children resource will return objects that belong to supplied parent
 * path.
 * @author bvan
 */
@Path("/root")
public class RootResource extends ConnectionResource {
    
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
        try (ContainerDAO dao = new ContainerDAO( getConnection() ) ){
            ArrayList<DatacatObject> retList = new ArrayList<>();
            for(DatacatObject child: dao.getChildrenStream( 0L, "/")){
                retList.add( child );
            }
            return retList;
        } catch (SQLException | IOException e){
            throw wrapForRestException(e);
        }
    }
    
}
