
package org.srs.datacat.rest.resources;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.srs.datacat.model.security.DcAclEntry;
import org.srs.datacat.rest.BaseResource;
import static org.srs.datacat.rest.BaseResource.OPTIONAL_EXTENSIONS;
import org.srs.datacat.rest.PATCH;
import org.srs.datacat.rest.RestException;
import org.srs.datacat.rest.security.AclEntryProxy;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcUriUtils;

/**
 *
 * @author bvan
 */
@Path("/permissions" + OPTIONAL_EXTENSIONS)
public class PermissionsResource extends BaseResource {
    private final String idRegex = "{id: [%\\w\\d\\-_\\./]+}";
    
    private UriInfo ui;
    private List<PathSegment> pathSegments;
    private String requestPath;
    private HashMap<String, List<String>> requestMatrixParams = new HashMap<>();
    private HashMap<String, List<String>> requestQueryParams = new HashMap<>();
    
    public PermissionsResource(@PathParam("id") List<PathSegment> pathSegments, @Context UriInfo ui){
        System.out.println("hello world");
        this.pathSegments = pathSegments;
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
    public Response getPermissions() {
        
        DcPath dcp = getProvider().getPath(DcUriUtils.toFsUri(requestPath, getUser(), "SRS"));
        try {
            DcFile dcf = getProvider().getFile(dcp);
            List<AclEntryProxy> acl = new ArrayList<>(dcf.getAcl().size());
            for(DcAclEntry e: dcf.getAcl()){
                acl.add(new AclEntryProxy(e));
            }
            return Response.ok(new GenericEntity<List<AclEntryProxy>>(acl){}).build();
        } catch (NoSuchFileException ex){
             throw new RestException(ex,404 , "File doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
    }
    
    @PUT
    @Path(idRegex)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response setPermissions(List<AclEntryProxy> proxyAcl){
        DcPath dcp = getProvider().getPath(DcUriUtils.toFsUri(requestPath, getUser(), "SRS"));

        try {
            ArrayList<DcAclEntry> acl = new ArrayList<>(proxyAcl.size());
            for(AclEntryProxy p: proxyAcl){
                acl.add(p.entry());
            }
            getProvider().mergeContainerAclEntries(dcp, acl, true);
            return Response.noContent().build();
        } catch (NoSuchFileException ex){
             throw new RestException(ex,404 , "File doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
    }
    
    @PATCH
    @Path(idRegex)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response mergePermissions(List<AclEntryProxy> proxyAcl){
        DcPath dcp = getProvider().getPath(DcUriUtils.toFsUri(requestPath, getUser(), "SRS"));
        
        try {
            ArrayList<DcAclEntry> acl = new ArrayList<>(proxyAcl.size());
            for(AclEntryProxy p: proxyAcl){
                acl.add(p.entry());
            }
            getProvider().mergeContainerAclEntries(dcp, acl, false);
            DcFile dcf = getProvider().getFile(dcp);
            
            proxyAcl = new ArrayList<>(dcf.getAcl().size());
            for(DcAclEntry e: dcf.getAcl()){
                proxyAcl.add(new AclEntryProxy(e));
            }
            return Response.ok(new GenericEntity<List<AclEntryProxy>>(proxyAcl){}).build();
        } catch (NoSuchFileException ex){
             throw new RestException(ex,404 , "File doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
    }
    
}
