package org.srs.webapps.datacat.controllers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.srs.rest.datacat.shared.DatacatObject;
import org.srs.rest.datacat.shared.container.BasicStat.StatType;
import org.srs.rest.datacat.shared.container.DatasetContainer;
import org.srs.rest.datacat.shared.sql.DatacatDAO;

/**
 *
 * @author bvan
 */
public class BrowserController extends ConnectionHttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        
        try(DatacatDAO dao = new DatacatDAO( getConnection() )) {

            if(request.getPathInfo() == null || request.getPathInfo().length() == 1){
                request.setAttribute( "parentURL", "/" );
                request.setAttribute( "children", dao.rootChildren() );
                request.setAttribute( "path", null );
            } else {
                String strpath = request.getPathInfo().substring( 1 );
                List<String> path = Arrays.asList( strpath.split( "\\/" ) );


                boolean extendedStat = request.getParameter( "stat" ) != null && request.
                        getParameter( "stat" ).equals( "extended" );
                StatType t = extendedStat ? StatType.DATASET : StatType.BASIC;

                DatacatObject pathObject = dao.findAnyObject( path, t );
                if(pathObject instanceof DatasetContainer){
                    long ccCount = ((DatasetContainer) pathObject).getStat().
                            getChildContainerCount();
                    long dsCount = ((DatasetContainer) pathObject).getStat().getDatasetCount();
                    if(ccCount == 0){
                        if(dsCount > 0){
                            pathObject = dao.findAnyObject( path, StatType.DATASET );
                            if(dsCount < 10000){
                                request.setAttribute( "datasets", dao.getDatasets( path ) );
                            }
                        }
                        request.setAttribute( "selected", pathObject );
                        path = path.subList( 0, path.size() - 1 );
                        pathObject = dao.findAnyObject( path, StatType.DATASET );
                    }
                }

                request.setAttribute( "parentURL", request.getPathInfo() );
                request.setAttribute( "path", pathObject );
                request.setAttribute( "children", dao.getChildren( path, StatType.LAZY, false ) );
            }
        } catch (Exception ex) {
            request.getRequestDispatcher( "/browseview/error.jsp" ).forward( request, response );
            return;
        }
        request.getRequestDispatcher( "/browseview/browser.jsp" ).forward( request, response );
    }
}
