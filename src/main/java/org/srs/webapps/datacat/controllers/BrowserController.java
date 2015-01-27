package org.srs.webapps.datacat.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.srs.datacat.client.Client;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.DatasetView;

import org.srs.datacat.shared.RequestView;
import org.srs.datacat.model.RecordType;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetStat;
import org.srs.vfs.PathUtils;


/**
 *
 * @author bvan
 */
public class BrowserController extends HttpServlet {

    private HashMap<String, List<String>> requestMatrixParams = new HashMap<>();
    private HashMap<String, List<String>> requestQueryParams = new HashMap<>();
    Client c;
    
    public BrowserController() throws MalformedURLException{
        c = new Client("http://scalnx-v04.slac.stanford.edu:8180/org-srs-datacat-war-0.2-SNAPSHOT/r");
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        
        RequestView rv = new RequestView(RecordType.FOLDER, null);
        if(request.getPathInfo() == null || request.getPathInfo().length() == 1){
            request.setAttribute( "parentURL", "/" );
            request.setAttribute( "containers", getContainers("/", rv, requestQueryParams));
            request.setAttribute( "path", null );
        } else {
            String path = request.getPathInfo();
            DatacatNode pathObject = c.getObject(path);
            request.setAttribute("target", pathObject );
            
            if(!pathObject.getType().isContainer()){
                request.setAttribute("dataset", pathObject);
                System.out.println(((Dataset) pathObject).getDateCreated());
                path = PathUtils.getParentPath(pathObject.getPath());
            }
            
            pathObject = c.getContainer(path, "dataset");
            DatasetStat t = (DatasetStat) ((DatasetContainer) pathObject).getStat();
            long ccCount = t.getGroupCount() + t.getFolderCount();
            long dsCount = t.getDatasetCount();
            if(ccCount == 0){
                if(dsCount > 0){
                    if(dsCount < 1000){
                        ArrayList<DatacatNode> datasets = new ArrayList<>();
                        for(DatacatNode d: getChildren(path, rv, requestQueryParams)){
                            if(!d.getType().isContainer()){
                                datasets.add(d);
                            }
                        }
                        request.setAttribute("datasets", datasets);
                    } else {
                        request.setAttribute("overflow", new Object());
                    }
                }
                request.setAttribute("containers", 
                        getContainers(PathUtils.getParentPath(path), rv, requestQueryParams));
                request.setAttribute( "selected", pathObject );
            } else {
                request.setAttribute("containers", 
                        getContainers(path, rv, requestQueryParams));
            }
            

            request.setAttribute("parentURL", request.getPathInfo() );
        }
        request.getRequestDispatcher( "/browseview/browser.jsp" ).forward( request, response );
    }
    
    List<DatacatNode> getChildren(String path, RequestView requestView, HashMap<String, List<String>> queryParams) throws IOException{
        int max = queryParams.containsKey("max") ? Integer.valueOf( queryParams.get("max").get(0)) :100000;
        int offset = queryParams.containsKey("offset") ? Integer.valueOf( queryParams.get("offset").get(0)) :0;
    
        List<DatacatNode> retList = new ArrayList<>();
        int count = 0;
        DatasetView dsView = requestView.getDatasetView(DatasetView.MASTER);
        for(DatacatNode n: c.getChildren(path, Integer.toString(dsView.getVersionId()), dsView.getSite() )){
            if(count >= offset && retList.size() < max){
                retList.add(n);
            }
            count++;
        }
        return retList;
    }
    
    List<DatacatNode> getContainers(String path, RequestView requestView, HashMap<String, List<String>> queryParams) throws IOException{
        int max = queryParams.containsKey("max") ? Integer.valueOf( queryParams.get("max").get(0)) :100000;
        int offset = queryParams.containsKey("offset") ? Integer.valueOf( queryParams.get("offset").get(0)) :0;
    
        List<DatacatNode> retList = new ArrayList<>();
        int count = 0;

        for(DatacatNode n: c.getContainers(path)){
            if(count >= offset && retList.size() < max){
                retList.add(n);
            }
            count++;
        }
        return retList;
    }
}
