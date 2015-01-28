package org.srs.webapps.datacat.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jersey.repackaged.com.google.common.base.Optional;
import org.srs.datacat.client.Client;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.DatasetModel;
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

    Client c;
    
    public BrowserController() throws MalformedURLException{
        c = new Client("http://scalnx-v04.slac.stanford.edu:8180/org-srs-datacat-war-0.2-SNAPSHOT/r");
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        HashMap<String, List<String>> requestQueryParams = new HashMap<>();
        Map<String, String[]> params = request.getParameterMap();
        for(Map.Entry<String, String[]> e: params.entrySet()){
            requestQueryParams.put(e.getKey(), Arrays.asList((String[]) e.getValue()));
        }
        
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
            request.setAttribute("container", pathObject);
            DatasetStat t = (DatasetStat) ((DatasetContainer) pathObject).getStat();
            long ccCount = t.getGroupCount() + t.getFolderCount();
            long dsCount = t.getDatasetCount();
            if(ccCount == 0){
                if(dsCount > 0){
                    ArrayList<DatacatNode> datasets = new ArrayList<>();
                    for(DatacatNode d: getDatasets(path, rv, requestQueryParams)){
                        if(!d.getType().isContainer()){
                            datasets.add(d);
                        }
                    }
                    request.setAttribute("datasets", datasets);
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
    
    List<DatasetModel> getDatasets(String path, RequestView requestView, HashMap<String, List<String>> queryParams) throws IOException{
        int max = queryParams.containsKey("max") ? Integer.valueOf( queryParams.get("max").get(0)) : 4000;
        int offset = queryParams.containsKey("offset") ? Integer.valueOf( queryParams.get("offset").get(0)) :0;
    
        String filter =  queryParams.containsKey("filter") ? queryParams.get("filter").get(0) :"";
        System.out.println(filter);;
        DatasetView dsView = requestView.getDatasetView(DatasetView.MASTER);
        List<DatasetModel> retList = c.searchForDatasets(path, Integer.toString(dsView.getVersionId()), 
                dsView.getSite(), filter, null ,null, offset, max);
        
        return retList;
    }
    
    List<DatacatNode> getContainers(String path, RequestView requestView, HashMap<String, List<String>> queryParams) throws IOException{
        int max = queryParams.containsKey("cmax") ? Integer.valueOf( queryParams.get("cmax").get(0)) :100000;
        int offset = queryParams.containsKey("coffset") ? Integer.valueOf( queryParams.get("coffset").get(0)) :0;
    
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
