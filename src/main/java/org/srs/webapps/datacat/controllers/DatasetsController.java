package org.srs.webapps.datacat.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.srs.datacat.client.exception.DcException;


/**
 *
 * @author bvan
 */
public class DatasetsController extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        
        try {
            HashMap<String, Object> requestAttributes = ControllerUtils.collectAttributes(request, true);
            for(Map.Entry<String, Object> entry: requestAttributes.entrySet()){
                request.setAttribute(entry.getKey(), entry.getValue());
            }
        } catch (DcException ex){   
            request.setAttribute("error", ex);
            request.getRequestDispatcher( "/display/error.jsp" ).forward( request, response );
            return;
        }
        request.getRequestDispatcher( "/display/datasets.jsp" ).forward( request, response );
    }
    
}
