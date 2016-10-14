
package org.srs.webapps.datacat.controllers;

import com.google.common.base.Optional;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import org.srs.datacat.client.Client;
import org.srs.webapps.datacat.model.ApplicationUriInfo;

/**
 *
 * @author bvan
 */
@Path("ajax/children")
public class AjaxChildren {
    @Context HttpServletRequest request;
    
    @GET
    public Response getRootChildren(@PathParam("id") List<PathSegment> pathSegments) throws ServletException, IOException{
        return getChildren(pathSegments);
    }
    
    @GET
    @Path("{id: ([%\\w\\d\\-_\\./]+)?}")
    public Response getChildren(@PathParam("id") List<PathSegment> segments) throws IOException{
        Client client = ControllerUtils.getClient(request);
        
        String path = ApplicationUriInfo.pathHelper(segments, null);
        String proxiedResponse = client.getPathResource().getChildren(path, 
                Optional.<String>absent(), Optional.<String>absent(), 
                Optional.<Integer>absent(), Optional.<Integer>absent()).readEntity(String.class);
        return Response.ok(proxiedResponse, MediaType.APPLICATION_JSON).build();
    }
    
}
