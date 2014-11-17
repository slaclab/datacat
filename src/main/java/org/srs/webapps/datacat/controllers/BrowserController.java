package org.srs.webapps.datacat.controllers;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.RequestView;
import org.srs.datacat.rest.ErrorResponse;
import org.srs.datacat.rest.RestException;
import org.srs.datacat.security.DcUser;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.container.BasicStat;
import org.srs.datacat.shared.container.BasicStat.StatType;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcFileSystemProvider;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcUriUtils;
import org.srs.datacat.vfs.attribute.ContainerViewProvider;
import org.srs.datacat.vfs.attribute.DatasetViewProvider;
import org.srs.vfs.AbstractFsProvider;

/**
 *
 * @author bvan
 */
public class BrowserController extends ConnectionHttpServlet {

    private String requestPath;
    private HashMap<String, List<String>> requestMatrixParams = new HashMap<>();
    private HashMap<String, List<String>> requestQueryParams = new HashMap<>();

    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        
        // TODO: Fix this so it parses the proper view
        RequestView rv = new RequestView(DatacatObject.Type.FOLDER, null);
        try {
            if(request.getPathInfo() == null || request.getPathInfo().length() == 1){
                DcPath dcp = getProvider().getPath(DcUriUtils.toFsUri("/", (DcUser) null, "SRS"));
                DcFile f = getProvider().getFile(dcp);
                request.setAttribute( "parentURL", "/" );
                request.setAttribute( "containers", getContainers(f, rv, requestQueryParams));
                request.setAttribute( "path", null );
            } else {
                String path = request.getPathInfo();
                DcPath dcp = getProvider().getPath(DcUriUtils.toFsUri(path, (DcUser) null, "SRS"));
                DcFile pathFile = getProvider().getFile(dcp);
                /*boolean extendedStat = request.getParameter( "stat" ) != null && request.
                        getParameter( "stat" ).equals( "extended" );
                StatType t = extendedStat ? StatType.DATASET : StatType.BASIC;*/

                DatacatObject pathObject = pathFile.getObject();
                if(pathFile.isDirectory()){
                    pathObject = pathFile.getAttributeView(ContainerViewProvider.class)
                            .withView(StatType.DATASET);
                    long ccCount = ((DatasetContainer) pathObject).getStat().
                            getChildContainerCount();
                    long dsCount = ((DatasetContainer) pathObject).getStat().getDatasetCount();
                    if(ccCount == 0){
                        if(dsCount > 0){
                            if(dsCount < 10000){
                                ArrayList<DatacatObject> datasets = new ArrayList<>();
                                for(DatacatObject d: getChildren(pathFile, rv, requestQueryParams)){
                                    if(!d.getType().isContainer()){
                                        datasets.add(d);
                                    }
                                }
                                request.setAttribute("datasets", datasets);
                            }
                        }
                        request.setAttribute( "selected", pathObject );
                    }
                }
                request.setAttribute("parentURL", request.getPathInfo() );
                request.setAttribute("path", pathObject );
                request.setAttribute("containers", getContainers(pathFile, rv, requestQueryParams));
            }
        } catch (AccessDeniedException ex) {
            request.setAttribute("error", new ErrorResponse(ex.getMessage(), ex.getClass().getSimpleName(), "403", null));
            response.setStatus(403);
            request.getRequestDispatcher( "/browseview/error.jsp" ).forward( request, response );
            return;
        } catch (NoSuchFileException ex) {
            request.setAttribute("error", new ErrorResponse(ex.getMessage(), ex.getClass().getSimpleName(), "404", null));
            response.setStatus(404);
            request.getRequestDispatcher( "/browseview/error.jsp" ).forward( request, response );
            return;
        }
        request.getRequestDispatcher( "/browseview/browser.jsp" ).forward( request, response );
    }
    
    List<DatacatObject> getChildren(DcFile dirFile, RequestView requestView, HashMap<String, List<String>> queryParams) throws IOException{
        
        boolean withDs = queryParams.containsKey("datasets") ? Boolean.valueOf( queryParams.get("datasets").get(0)) : true;
        BasicStat.StatType statType = queryParams.containsKey("stat") ? BasicStat.StatType.valueOf( queryParams.get("stat").get(0).toUpperCase()): BasicStat.StatType.NONE;
        int max = queryParams.containsKey("max") ? Integer.valueOf( queryParams.get("max").get(0)) :100000;
        int offset = queryParams.containsKey("offset") ? Integer.valueOf( queryParams.get("offset").get(0)) :0;
        boolean showCount = queryParams.containsKey("showCount") ? Boolean.valueOf( queryParams.get("showCount").get(0)) :false;
    
        List<DatacatObject> retList = new ArrayList<>();
        int count = 0;
        try (DirectoryStream<java.nio.file.Path> stream = getProvider()
                .newOptimizedDirectoryStream(dirFile.getPath(), AbstractFsProvider.AcceptAllFilter, 
                    max, requestView.getDatasetView(DatasetView.CURRENT_ALL))){
            Iterator<java.nio.file.Path> iter = stream.iterator();
            
            while(iter.hasNext() && (retList.size() < max || showCount)){
                java.nio.file.Path p = iter.next();
                DcFile file;
                try {
                     file = getProvider().getFile(p);
                } catch (AccessDeniedException ex){
                    continue;
                }
                if(!withDs && file.isRegularFile()){
                    continue;
                }
                if(count >= offset && retList.size() < max){
                    DatacatObject ret;
                    if(file.isRegularFile()){
                        try {
                            ret = file.getAttributeView(DatasetViewProvider.class).withView(requestView);
                        } catch (NoSuchFileException ex){
                            continue;
                        }
                    } else {
                        ret = file.getAttributeView(ContainerViewProvider.class).withView(statType);
                    }
                    retList.add(ret);
                }
                count++;
            }
        }
        return retList;
    }
    
    List<DatacatObject> getContainers(DcFile dirFile, RequestView requestView, HashMap<String, List<String>> queryParams) throws IOException{
        
        boolean withDs = queryParams.containsKey("datasets") ? Boolean.valueOf( queryParams.get("datasets").get(0)) : true;
        BasicStat.StatType statType = queryParams.containsKey("stat") ? BasicStat.StatType.valueOf( queryParams.get("stat").get(0).toUpperCase()): BasicStat.StatType.NONE;
        int max = queryParams.containsKey("max") ? Integer.valueOf( queryParams.get("max").get(0)) :100000;
        int offset = queryParams.containsKey("offset") ? Integer.valueOf( queryParams.get("offset").get(0)) :0;
        boolean showCount = queryParams.containsKey("showCount") ? Boolean.valueOf( queryParams.get("showCount").get(0)) :false;
    
        List<DatacatObject> retList = new ArrayList<>();
        int count = 0;
        try (DirectoryStream<DcPath> stream = getProvider().directSubdirectoryStream( dirFile.getPath(), DcFileSystemProvider.AcceptAllFilter)){
            Iterator<DcPath> iter = stream.iterator();
            
            while(iter.hasNext() && (retList.size() < max || showCount)){
                java.nio.file.Path p = iter.next();
                DcFile file;
                try {
                    file = getProvider().getFile(p);
                } catch (AccessDeniedException ex){ continue; }
                if(count >= offset && retList.size() < max){
                    System.out.println(file.getAttributeView(ContainerViewProvider.class).withView(statType).getPath());
                    retList.add(file.getAttributeView(ContainerViewProvider.class).withView(statType));                    
                }
                count++;
            }
        }
        return retList;
    }
}
