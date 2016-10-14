package org.srs.webapps.datacat.controllers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import org.glassfish.jersey.server.mvc.Template;
import org.srs.datacat.rest.FormParamConverter;
import org.srs.webapps.datacat.model.NodeTargetModel;
import org.srs.webapps.datacat.model.ApplicationUriInfo;

/**
 *
 * @author bvan
 */
@Path("display/edit")
public class Edit {
    @Context HttpServletRequest request;
    @Context UriInfo uriInfo;
    List<PathSegment> pathSegments;
    
    public Edit(@PathParam("id") List<PathSegment> pathSegments){
        this.pathSegments = pathSegments;
    }

    @GET
    @Path("{id: ([%\\w\\d\\-_\\./]+)?}")
    @Template(name = "/display/edit.jsp")
    public NodeTargetModel getModel(@PathParam("id") List<PathSegment> pathSegments) throws ServletException, IOException{
        String path = ApplicationUriInfo.pathHelper(pathSegments, null);
        ApplicationUriInfo uriModel = ApplicationUriInfo.getUriModel(uriInfo, getClass(), path);
        return ControllerUtils.buildModel(request, uriModel, false);
    }
    
    @POST
    @Path("{id: ([%\\w\\d\\-_\\./]+)?}")
    @Template(name = "/display/edit.jsp")
    @Consumes("application/x-www-form-urlencoded")
    public void updateNode() throws ServletException, IOException{
        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<>();
        for(Entry<String, String[]> entry: request.getParameterMap().entrySet()){
            formParams.put(entry.getKey(), Arrays.asList(entry.getValue()));
        }
        String path = ApplicationUriInfo.pathHelper(pathSegments, null);
        ApplicationUriInfo uriModel = ApplicationUriInfo.getUriModel(uriInfo, getClass(), path);
        return ;//ControllerUtils.buildModel(request, uriModel, false);
    }

}
