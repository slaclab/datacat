package org.srs.webapps.datacat.controllers;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
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
import org.srs.datacat.client.Client;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetContainer;
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
        Client client = ControllerUtils.getClient(request);
        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<>();
        String referer = request.getParameter("_referer");
        Set<String> omitFromConverter = new HashSet<>(Arrays.asList("_referer"));
        for(Entry<String, String[]> entry: request.getParameterMap().entrySet()){
            if(!omitFromConverter.contains(entry.getKey())){
                formParams.put(entry.getKey(), Arrays.asList(entry.getValue()));
            }
        }
        String path = ApplicationUriInfo.pathHelper(pathSegments, null);
        DatacatNode target = client.getObject(path);
        if(target.getType().isContainer()){
            DatasetContainer container = FormParamConverter.getContainerBuilder(target.getType(), formParams).build();
            client.patchContainer(path, container);
        } else {
            Dataset ds = FormParamConverter.getDatasetBuilder(formParams).build();
            client.patchDataset(path, ds);
        }
        
        URI returnUri = null;
        if(referer != null && !referer.isEmpty()){
            returnUri = UriBuilder.fromUri(referer).build();
        } else {
            returnUri = uriInfo.getBaseUriBuilder()
                .path("display") // Should always come from a display
                .path("browser")
                .path(path).build();
        }
        return Response.seeOther(returnUri).build();
    }

}