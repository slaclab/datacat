
package org.srs.datacat.rest.resources;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.srs.datacat.model.security.CallContext;
import org.srs.datacat.model.security.DcAclEntry;
import org.srs.datacat.model.security.DcGroup;
import org.srs.datacat.model.security.DcSubject;
import org.srs.datacat.rest.BaseResource;
import static org.srs.datacat.rest.BaseResource.OPTIONAL_EXTENSIONS;
import org.srs.datacat.rest.PATCH;
import org.srs.datacat.rest.RestException;
import org.srs.datacat.rest.security.AclEntryProxy;

/**
 *
 * @author bvan
 */
@Path("/permissions" + OPTIONAL_EXTENSIONS)
public class PermissionsResource extends BaseResource {
    private final String idRegex = "{id: [%\\w\\d\\-_\\./]+}";
    
    private final UriInfo ui;
    private final  String requestPath;
    private final  HashMap<String, List<String>> requestMatrixParams = new HashMap<>();
    private final  HashMap<String, List<String>> requestQueryParams = new HashMap<>();
    
    public PermissionsResource(@PathParam("id") List<PathSegment> pathSegments, @Context UriInfo ui){
        this.ui = ui;
        String path = "";
        if(pathSegments != null){
            for(PathSegment s: pathSegments){
                path = path + "/" + s.getPath();
                requestMatrixParams.putAll(s.getMatrixParameters());
            }   
        }
        requestPath = path;
        requestQueryParams.putAll(ui.getQueryParameters());
    }

    @GET
    @Path(idRegex)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response getAclOrPermissions(@DefaultValue("") @QueryParam("subject") String subject,
            @DefaultValue("") @QueryParam("group") String groupSpec) {
        
        java.nio.file.Path dcp = getProvider().getPath(requestPath);
        try {
            if(!subject.isEmpty()){
                return getPermissions(dcp, subject, groupSpec);
            }
            List<AclEntryProxy> acl = new ArrayList<>();
            for(DcAclEntry e: getProvider().getAcl(dcp, buildCallContext())){
                acl.add(new AclEntryProxy(e));
            }
            return Response.ok(new GenericEntity<List<AclEntryProxy>>(acl){}).build();
        } catch (IllegalArgumentException ex){
            throw new RestException(ex, 400 , "Unable to process subject permissions", ex.getMessage());
        } catch (NoSuchFileException ex){
             throw new RestException(ex,404 , "File doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (IOException ex){
            Logger.getLogger(PermissionsResource.class.getName()).log(Level.WARNING, "Unknown exception", ex);
            throw new RestException(ex, 500);
        }
    }
    
    @PUT
    @Path(idRegex)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response setAcl(List<AclEntryProxy> proxyAcl){
        java.nio.file.Path dcp = getProvider().getPath(requestPath);

        try {
            ArrayList<DcAclEntry> acl = new ArrayList<>();
            for(AclEntryProxy p: proxyAcl){
                acl.add(p.entry());
            }
            getProvider().mergeContainerAclEntries(dcp, buildCallContext(), acl, true);
            return Response.noContent().build();
        } catch (NoSuchFileException ex){
             throw new RestException(ex,404 , "File doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (IOException ex){
            Logger.getLogger(PermissionsResource.class.getName()).log(Level.WARNING, "Unknown exception", ex);
            throw new RestException(ex, 500);
        }
    }
    

    @PATCH
    @Path(idRegex)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response mergeAcl(List<AclEntryProxy> proxyAcl){
        java.nio.file.Path dcp = getProvider().getPath(requestPath);
        
        try {
            ArrayList<DcAclEntry> acl = new ArrayList<>();
            for(AclEntryProxy p: proxyAcl){
                acl.add(p.entry());
            }
            CallContext context = buildCallContext();
            getProvider().mergeContainerAclEntries(dcp, context, acl, false);
            
            proxyAcl = new ArrayList<>();
            for(DcAclEntry e: getProvider().getAcl(dcp, context)){
                proxyAcl.add(new AclEntryProxy(e));
            }
            return Response.ok(new GenericEntity<List<AclEntryProxy>>(proxyAcl){}).build();
        } catch (NoSuchFileException ex){
             throw new RestException(ex,404 , "File doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (IOException ex){
            Logger.getLogger(PermissionsResource.class.getName()).log(Level.WARNING, "Unknown exception", ex);
            throw new RestException(ex, 500);
        }
    }
    
    private Response getPermissions(java.nio.file.Path dcp, String subject, String groupSpec) throws IOException{
        DcSubject target;
        String permissions = "";
        CallContext callContext = buildCallContext();
        if("group".equalsIgnoreCase(subject)){
            if(groupSpec.isEmpty()){
                throw new IllegalArgumentException("No Group is specified");
            }
            target = DcGroup.fromSpec(groupSpec);
            permissions = getProvider().getPermissions(dcp, callContext, (DcGroup) target);
        } else if ("user".equalsIgnoreCase(subject)){
            target = callContext.getSubject();
            permissions = getProvider().getPermissions(dcp, callContext, null);
        } else {
            throw new IllegalArgumentException("'user' or 'group' must be specified");
        }
        if(target == null){
            target = DcGroup.PUBLIC_GROUP;
        }
        DcAclEntry e = DcAclEntry.newBuilder()
            .subject(target)
            .permissions(permissions)
            .build();
        return Response.ok(new GenericEntity<AclEntryProxy>(new AclEntryProxy(e)){}).build();
    }
}
