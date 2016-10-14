package org.srs.webapps.datacat.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.srs.datacat.client.exception.DcException;
import org.srs.webapps.datacat.model.NodeTargetModel;

/**
 *
 * @author bvan
 */
public class NewController extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        
        
        try {
            String path = request.getPathInfo();
            // NodeTargetModel model = ControllerUtils.buildModel(request, path, false);
            //model.put("type", request.getParameter("type"));
            // request.setAttribute("model", model);
            request.setAttribute("type", request.getParameter("type"));
        } catch (DcException ex){   
            request.setAttribute("error", ex);
            request.getRequestDispatcher( "/display/error.jsp" ).forward( request, response );
            return;
        }
        request.getRequestDispatcher( "/display/new.jsp" ).forward( request, response );
    }
    
}
