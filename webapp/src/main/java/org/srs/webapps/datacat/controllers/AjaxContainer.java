
package org.srs.webapps.datacat.controllers;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import org.srs.webapps.datacat.model.NodeTargetModel;
import org.glassfish.jersey.server.mvc.Template;
import org.srs.webapps.datacat.model.ApplicationUriInfo;

/**
 *
 * @author bvan
 */
@Path("ajax/container")
public class AjaxContainer {
    @Context HttpServletRequest request;
    @Context UriInfo uriInfo;

    @GET
    @Path("{id: [%\\w\\d\\-_\\./]*}")
    @Template(name = "/views/container.jsp")
    public NodeTargetModel getContainer(@PathParam("id") List<PathSegment> pathSegments) throws ServletException, IOException{
        String path = ApplicationUriInfo.pathHelper(pathSegments, null);
        ApplicationUriInfo uriModel = ApplicationUriInfo.getUriModel(uriInfo, getClass(), path);
        return ControllerUtils.buildModel(request, uriModel, false);
    }
}
