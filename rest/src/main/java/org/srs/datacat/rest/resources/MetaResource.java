package org.srs.datacat.rest.resources;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.server.ExtendedResourceContext;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.srs.datacat.rest.HumanPath;
import org.srs.datacat.rest.RestMethod;
import org.srs.datacat.rest.RestResource;

/**
 * A Resource that displays info about the resources
 *
 * @author bvan
 */

@Path("/meta")
public class MetaResource {
    
    private final String idRegex = "{id}";
    private final String idPath = "/{resourceName}";

    @Context private ExtendedResourceContext rconfig;    
    private HashMap<String, RestResource> resourcesMap;
    private ArrayList<RestResource> resources;

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public List<RestResource> getMetas() {
        checkResources();
        return resources;
    }

    @GET
    @Path(idRegex)
    @HumanPath(idPath)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public RestResource getMeta(@PathParam("id") String resource) {
        checkResources();
        RestResource t = resourcesMap.get( "/" + resource );
        return t;
    }
    
    private void checkResources() {
        if ( resources != null ) {
            return;
        }
        resourcesMap = new HashMap<>();
        resources = new ArrayList<>();
        List<Resource> parents = rconfig.getResourceModel().getRootResources();
        for ( Resource root : parents ) {

            ArrayList<RestMethod> mlist = new ArrayList<>();

            // Add all methods
            addMethod(root.getResourceLocator(), mlist);
            for ( ResourceMethod am : root.getResourceMethods() ) {
                addMethod(am, mlist);
            }
            
            for(Resource child : root.getChildResources() ){
                addMethod(child.getResourceLocator(), mlist);
                for ( ResourceMethod am : child.getResourceMethods() ) {
                    addMethod(am, mlist);
                }
            }
            
            
            RestResource rsrc = new RestResource( root.getPath(), mlist);
            resources.add( rsrc );
            resourcesMap.put( rsrc.path, rsrc );
        }
    }
    
    void addMethod(ResourceMethod am, List<RestMethod> mlist){
        if(am == null || am.getHttpMethod().equals( "OPTIONS" )){
            return;
        }
        List<String> al = new ArrayList<>();
        for(MediaType m: am.getProducedTypes()){
            al.add( m.toString() );
        }

        ArrayList<String> hms = new ArrayList<>();
        hms.add( am.getHttpMethod() );

        // Get query parameters
        List<String> qpars = new ArrayList<>();


        for(Parameter p: am.getInvocable().getParameters()){
            qpars.add( p.getSourceName() );
        }
        qpars = qpars.isEmpty() ? null : qpars;
        Invocable in = am.getInvocable();
        RestMethod rm = new RestMethod( hms, qpars, returnType( in.getHandlingMethod() ), al );
        if( in.getHandlingMethod().isAnnotationPresent( HumanPath.class )){
            rm.path = in.getHandlingMethod().getAnnotation( HumanPath.class ).value();
        }
        mlist.add( rm );
    }
    
    private String returnType(Method method){
        Class<?> rtype = method.getReturnType();

        // Check if we get an list/arraylist back
        boolean arr = false;
        if( List.class.isAssignableFrom( rtype ) ){
            arr = true;
            rtype = (Class<?>) ((ParameterizedType) method.
                    getGenericReturnType()).
                    getActualTypeArguments()[0];
        }   
        String rname = rtype.getSimpleName();
        return arr ? "array[" + rname + "]" : rname;
    }
}
