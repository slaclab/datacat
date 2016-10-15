
package org.srs.webapps.datacat.controllers;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.glassfish.jersey.server.mvc.ErrorTemplate;
import org.glassfish.jersey.server.mvc.Template;
import org.srs.datacat.rest.FormParamConverter;
import org.srs.datacat.shared.Dataset;
import org.srs.webapps.datacat.model.NodeTargetModel;
import org.srs.webapps.datacat.model.ApplicationUriInfo;

/**
 *
 * @author bvan
 */
@Path("display/new")
public class New {
        
    @Context HttpServletRequest request;
    @Context UriInfo uriInfo;

    @GET
    @Path("{id: ([%\\w\\d\\-_\\./]+)?}")
    @Template(name = "/display/new.jsp")
    @ErrorTemplate(name = "/display/error.jsp")
    public NodeTargetModel getModel(@PathParam("id") List<PathSegment> pathSegments) throws IOException{
        String path = ApplicationUriInfo.pathHelper(pathSegments, null);
        ApplicationUriInfo uriModel = ApplicationUriInfo.getUriModel(uriInfo, getClass(), path);
        return ControllerUtils.buildModel(request, uriModel, false);
    }
    
    @POST
    @Path("{id: ([%\\w\\d\\-_\\./]+)?}")
    @Consumes("application/x-www-form-urlencoded")
    @ErrorTemplate(name = "/display/error.jsp")
    public Response updateNode(@PathParam("id") List<PathSegment> pathSegments) throws  IOException{
        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<>();
        for(Map.Entry<String, String[]> entry: request.getParameterMap().entrySet()){
            formParams.put(entry.getKey(), Arrays.asList(entry.getValue()));
        }
        String path = ApplicationUriInfo.pathHelper(pathSegments, null);
        Dataset newDataset = FormParamConverter.getDatasetBuilder(formParams).build();
        
        // Try to return to the original view
        String originalReferer = request.getParameter("_referer");
        URI returnUri = null;
        if(originalReferer != null && !originalReferer.isEmpty()){
            URI displayUrl = uriInfo.getBaseUriBuilder().path("display").build();
            String refPath = displayUrl.relativize(UriBuilder.fromUri(originalReferer).build()).toString();
            String oldView = refPath.substring(0, refPath.indexOf("/"));
            returnUri = uriInfo.getBaseUriBuilder()
                .path("display") // Should always come from a display
                .path(oldView)
                .path(path)
                .path(newDataset.getName()).build();
        } else {
            returnUri = uriInfo.getBaseUriBuilder()
                .path("display")
                .path("browser") // Return to browser by default
                .path(path)
                .path(newDataset.getName()).build();
        }
        ControllerUtils.getClient(request).createDataset(path, newDataset);
        return Response.seeOther(returnUri).build();
    }

}
