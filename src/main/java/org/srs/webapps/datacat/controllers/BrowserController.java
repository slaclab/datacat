package org.srs.webapps.datacat.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
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
    
    static final int DEFAULT_MAX = 100;

    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        Client client = new Client(request.getRequestURL().append("/r").toString(), request);
        HashMap<String, List<String>> requestQueryParams = new HashMap<>();
        Map<String, String[]> params = request.getParameterMap();
        for(Map.Entry<String, String[]> e: params.entrySet()){
            requestQueryParams.put(e.getKey(), Arrays.asList((String[]) e.getValue()));
        }
        
        RequestView rv = new RequestView(RecordType.FOLDER, null);
        if(request.getPathInfo() == null || request.getPathInfo().length() == 1){
            request.setAttribute( "parentURL", "/" );
            request.setAttribute( "containers", getContainers(client, "/", rv, requestQueryParams));
            request.setAttribute( "path", null );
        } else {
            String path = request.getPathInfo();
            DatacatNode pathObject = client.getObject(path);
            request.setAttribute("target", pathObject );
            
            if(!pathObject.getType().isContainer()){
                request.setAttribute("dataset", pathObject);
                System.out.println(((Dataset) pathObject).getDateCreated());
                path = PathUtils.getParentPath(pathObject.getPath());
            }
            
            pathObject = client.getContainer(path, "dataset");
            request.setAttribute("container", pathObject);
            DatasetStat t = (DatasetStat) ((DatasetContainer) pathObject).getStat();
            long ccCount = t.getGroupCount() + t.getFolderCount();
            long dsCount = t.getDatasetCount();
            
            int offset = requestQueryParams.containsKey("offset") ? 
                    Integer.valueOf( requestQueryParams.get("offset").get(0)) :0;
            
            int max = requestQueryParams.containsKey("max") ? 
                    Integer.valueOf( requestQueryParams.get("max").get(0)) : DEFAULT_MAX;

            if(ccCount == 0){
                if(dsCount > 0){
                    ArrayList<DatacatNode> datasets = new ArrayList<>();
                    for(DatacatNode d: getDatasets(client, path, rv, requestQueryParams, offset, max)){
                        if(!d.getType().isContainer()){
                            datasets.add(d);
                        }
                    }
                    request.setAttribute("datasets", datasets);
                    // Paging
                    StringBuffer reqUrl = request.getRequestURL();
                    if(request.getQueryString() != null){
                        reqUrl.append('?').append(request.getQueryString());
                    }
                    if(datasets.size() == max){
                        request.setAttribute("next", nextUrl(reqUrl.toString(), offset, max));
                    }
                    if(offset > 0){
                        request.setAttribute("previous", previousUrl(reqUrl.toString(), offset, max));
                    }
                }
                request.setAttribute("containers", 
                        getContainers(client, PathUtils.getParentPath(path), rv, requestQueryParams));
                request.setAttribute("selected", pathObject );
            } else {
                request.setAttribute("containers", 
                        getContainers(client, path, rv, requestQueryParams));
            }
            

            request.setAttribute("parentURL", request.getPathInfo() );
        }
        request.getRequestDispatcher( "/browseview/browser.jsp" ).forward( request, response );
    }
    
    List<DatasetModel> getDatasets(Client c, String path, RequestView requestView, 
            HashMap<String, List<String>> queryParams, int offset, int max) throws IOException{
        String filter =  queryParams.containsKey("filter") ? queryParams.get("filter").get(0) :"";
        DatasetView dsView = requestView.getDatasetView(DatasetView.MASTER);
        List<DatasetModel> retList = c.searchForDatasets(path, Integer.toString(dsView.getVersionId()), 
                dsView.getSite(), filter, null ,null, offset, max);
        
        return retList;
    }
    
    List<DatacatNode> getContainers(Client c, String path, RequestView requestView, 
            HashMap<String, List<String>> queryParams) throws IOException{
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
    
    private String nextUrl(String requestUri, int offset, int max){
        return UriBuilder.fromUri(requestUri)
                .replaceQueryParam("offset", offset+max).build()
                .getQuery();
    }
    
    private String previousUrl(String requestUri, int offset, int max){
        return UriBuilder.fromUri(requestUri)
                .replaceQueryParam("offset", offset-max > 0 ? offset - max : 0).build()
                .getQuery();
    }
}
