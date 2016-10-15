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
@Path("display/edit")
public class Edit {
    @Context HttpServletRequest request;
    @Context UriInfo uriInfo;

    @GET
    @Path("{id: ([%\\w\\d\\-_\\./]+)?}")
    @Template(name = "/display/edit.jsp")
    @ErrorTemplate(name = "/display/error.jsp")
    public NodeTargetModel getModel(@PathParam("id") List<PathSegment> pathSegments) throws ServletException, IOException{
        String path = ApplicationUriInfo.pathHelper(pathSegments, null);
        ApplicationUriInfo uriModel = ApplicationUriInfo.getUriModel(uriInfo, getClass(), path);
        return ControllerUtils.buildModel(request, uriModel, false);
    }
    
    @POST
    @Path("{id: ([%\\w\\d\\-_\\./]+)?}")
    @Consumes("application/x-www-form-urlencoded")
    @ErrorTemplate(name = "/display/error.jsp")
    public Response updateNode(@PathParam("id") List<PathSegment> pathSegments) throws ServletException, IOException{
        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<>();
        for(Entry<String, String[]> entry: request.getParameterMap().entrySet()){
            formParams.put(entry.getKey(), Arrays.asList(entry.getValue()));
        }
        String path = ApplicationUriInfo.pathHelper(pathSegments, null);
        String referer = request.getParameter("_referer");
        Dataset ds = FormParamConverter.getDatasetBuilder(formParams).build();
        ControllerUtils.getClient(request).patchDataset(path, ds);
        return Response.seeOther(UriBuilder.fromUri(referer).build()).build();
    }

}
