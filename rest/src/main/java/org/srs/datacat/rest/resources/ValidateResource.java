
package org.srs.datacat.rest.resources;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.dataset.FlatDataset;

/**
 *
 * @author bvan
 */
@Path("/validate")
public class ValidateResource {
    
    private final String idRegex = "{id: [\\w\\d\\-_\\./]+}";

    /**
     *
     * @param object
     * @return
     *
    @Path("/generic")
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    public DatacatObject reflectGenericPost(DatacatObject object){
        return null;
    }
    
    @Path("/generic")
    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    public DatacatObject reflectGenericPut(DatacatObject object){
        return object;
    }

    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Path("/folder")
    @Produces(MediaType.APPLICATION_JSON)
    public DatacatObject reflectFolderPost(MultivaluedMap<String, String> formParams){
        for(String key: formParams.keySet()){
            System.out.print( key +":\n");
            System.out.println(formParams.get(key).toString());
        }
        return null;
    }
    
    @PUT
    @Consumes("application/x-www-form-urlencoded")
    @Path("/folder")
    @Produces(MediaType.APPLICATION_JSON)
    public DatacatObject reflectFolderPut(MultivaluedMap<String, String> formParams){
        for(String key: formParams.keySet()){
            System.out.print( key +":\n");
            System.out.println(formParams.get(key).toString());
        }
        return null;
    }

    @Path("/group")
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    public DatacatObject reflectGroupPost(MultivaluedMap<String, String> formParams){
            * 
        return null;
    }
    
    @Path("/group")
    @PUT
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    public DatacatObject reflectGroupPut(MultivaluedMap<String, String> formParams){
        
        return null;
    }
    
    @Path("/dataset")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DatacatObject reflectDatasetPost(Dataset form){
        return form;
    }
    */
    /*
    @Path("/dataset")
    @PUT
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    public Dataset reflectDatasetPut(MultivaluedMap<String, String> formParams){
        
        HashMap<String, Method> jsonSetterMethods = new HashMap<>();
        ObjectMapper mapper =  new ObjectMapper();
        DatasetBuilder builder = DatasetBuilder.create( FlatDataset.Builder.class );
        
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
        
        return builder.build();
    }
    */
    
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
        //System.out.println(matParams.getMatrixParameters().toString());
        /*System.out.println(matParams.getPath());
        System.out.println(matParams.getMatrixParameters());
        StringBuilder sb = new StringBuilder("path: ").append( path );
        for(Entry<String, List<String>> e: matParams.getMatrixParameters().entrySet()){
            sb.append( "key: ").append( e.getKey()).append( "\n");
            for(String s: e.getValue()){
                sb.append( "\t").append(s);
            }
        }*/
        return versions + " " + locations;
    }
}
