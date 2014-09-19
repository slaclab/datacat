/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacat.rest.resources;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.srs.datacat.rest.BaseResource;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.dataset.FlatDataset;
//import org.srs.datacat.sql.DatacatDAO;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcUriUtils;  
import org.srs.rest.shared.HumanPath;
import org.srs.rest.shared.RestException;

/**
 * The datasets resource will return all datasets under a given path.
 * You can recurse a path to find all datasets that will fall underneath that
 * path.
 * @author bvan
 */
@Path("/datasets")
public class DatasetsResource extends BaseResource  { 
    private final String idRegex = "{id: [\\w\\d\\-_\\./]+}";
    private final String idPath = "[/path]*/{id}";
    
    @Context private HttpServletResponse response;
    
   
    @GET
    @Path(idRegex)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public String createDataset(@PathParam("id") String path, 
            @MatrixParam("v") List<String> versions,
            @MatrixParam("l") List<String> locations){
        System.out.println("hi");
        System.out.println(path);
        System.out.println(versions.toString());
        System.out.println(locations.toString());
        return versions + " " + locations;
    }
    
    
    /*@PUT
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    public Dataset reflectDatasetPut(@PathParam("id") String path, 
            MultivaluedMap<String, String> formParams,
            @MatrixParam("v") List<String> versions,
            @MatrixParam("l") List<String> locations){
        
        boolean createVersion = versions.size() != 1;
        if(versions.size() == 0){
            
        }
        
        HashMap<String, Method> jsonSetterMethods = new HashMap<>();
        ObjectMapper mapper =  new ObjectMapper();
        Dataset.Builder builder = new FlatDataset.Builder();
        
        for(Method m: builder.getClass().getMethods()){
            if( m.getAnnotation( JsonSetter.class ) != null ){
                jsonSetterMethods.put( m.getName(), m);
            }
        }
        
        try {
            for(String key: formParams.keySet()){
                Method m = jsonSetterMethods.get( key );
                List<String> lValue = formParams.get( key );
                if(lValue.size() != 1){
                    throw new RuntimeException( "Only one value per parameter is supported" );
                }
                Class<?> targetValueType = m.getParameterTypes()[0]; // Should only be one
                Object value = mapper.convertValue( lValue.get( 0 ), targetValueType );
                m.invoke( builder, value );
            }
        } catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger( ValidateResource.class.getName() ).log( Level.SEVERE, null, ex );
        }
        
        FlatDataset dsReq = (FlatDataset) builder.build();
        
        path = "/" + path;
        
        DcPath dcp = getProvider().getPath(DcUriUtils.toFsUri(path, null, "SRS"));        
        
        
        
        return builder.build();
    }*/


}
