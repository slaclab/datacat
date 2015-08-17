package org.srs.webapps.datacat.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.srs.datacat.client.Client;

/**
 *
 * @author bvan
 */
public class NewController extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        
        
        try {
            HashMap<String, Object> model = ControllerUtils.collectAttributes(request, false);
            model.put("type", request.getParameter("type"));
            for(Map.Entry<String, Object> entry: model.entrySet()){
                request.setAttribute(entry.getKey(), entry.getValue());
            }
        } catch (Client.DcException ex){   
            request.setAttribute("error", ex);
            request.getRequestDispatcher( "/display/error.jsp" ).forward( request, response );
            return;
        }
        request.getRequestDispatcher( "/display/new.jsp" ).forward( request, response );
    }
    
}
