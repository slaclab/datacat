package org.srs.webapps.datacat.controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.ClientRequestFilter;
import org.srs.datacat.client.Client;
import org.srs.datacat.client.ClientBuilder;
import org.srs.datacat.client.auth.HeaderFilter;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.DatasetResultSetModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.RecordType;
import org.srs.datacat.model.security.DcAclEntry;
import org.srs.datacat.model.security.DcPermissions;
import org.srs.datacat.shared.DatasetStat;
import org.srs.datacat.shared.RequestView;
import org.srs.vfs.PathUtils;

/**
 *
 * @author bvan
 */
public class ControllerUtils {
    static final int DEFAULT_MAX = 100;
    
    public static ClientRequestFilter getCasClientFilter(HttpServletRequest request){
        Map<String, Object> headers = new HashMap<>();
        headers.put("x-cas-username", request.getSession().getAttribute("userName"));
        return new HeaderFilter(headers);
    }
    
    public static ClientRequestFilter getClientFilter(HttpServletRequest request){
        return getCasClientFilter(request);
    }
    
    public static Client getClient(HttpServletRequest request) throws IOException{
        String localUrl = String.format("%s://%s:%s%s/r",
                request.getScheme(), request.getServerName(), request.getServerPort(),
                request.getContextPath());
        try {
            return ClientBuilder.newBuilder()
                    .setUrl(localUrl)
                    .addClientRequestFilter(getClientFilter(request))
                    .build();
        } catch(URISyntaxException ex) {
            throw new IOException(ex);
        }
    }

    public static HashMap<String, Object> collectAttributes(HttpServletRequest request, boolean withDatasets)
            throws ServletException, IOException{

        HashMap<String, Object> requestAttributes = collectBasicAttributes(request);
        HashMap<String, List<String>> requestQueryParams = getQueryParams(request);
        RequestView rv = new RequestView(RecordType.FOLDER, null);
        // This Assumes all REST requests are routed to the same base URL
        Client client = getClient(request); 
        if(request.getPathInfo() == null || request.getPathInfo().length() == 1){
            requestAttributes.put("parentURL", "/");
            requestAttributes.put("containers", getContainers(client, "/", rv, requestQueryParams));
            requestAttributes.put("path", null);
        } else {
            String path = request.getPathInfo();
            DatacatNode target = client.getObject(path, "current", "all");

            if(target.getType().isContainer()){
                target = client.getContainer(path, "dataset");
            }
            String parentPath = PathUtils.getParentPath(target.getPath());
            if(parentPath.length() > 1){
                requestAttributes.put("parent", client.getContainer(parentPath, "dataset"));
            }

            requestAttributes.put("target", target);
            DcAclEntry e = client.getPermissions(path, null);
            requestAttributes.put("canWrite", e.getPermissions().contains(DcPermissions.WRITE));
            requestAttributes.put("canDelete", e.getPermissions().contains(DcPermissions.DELETE));
            requestAttributes.put("canInsert", e.getPermissions().contains(DcPermissions.INSERT));
            if(target.getType().isContainer()){
                DatasetStat t = (DatasetStat) ((DatasetContainer) target).getStat();
                long ccCount = t.getGroupCount() + t.getFolderCount();
                long dsCount = t.getDatasetCount();

                int offset = requestQueryParams.containsKey("offset")
                        ? Integer.valueOf(requestQueryParams.get("offset").get(0)) : 0;

                int max = requestQueryParams.containsKey("max")
                        ? Integer.valueOf(requestQueryParams.get("max").get(0)) : DEFAULT_MAX;

                if(withDatasets && dsCount > 0){
                    ArrayList<DatacatNode> datasets = new ArrayList<>();
                    DatasetResultSetModel searchResults = getDatasets(client, path, rv, requestQueryParams, offset, max);
                    for(DatacatNode d: searchResults.getResults()){
                        if(!d.getType().isContainer()){
                            datasets.add(d);
                        }
                    }
                    requestAttributes.put("datasets", datasets);
                    requestAttributes.put("datasetCount", searchResults.getCount());
                    // Paging
                    StringBuffer reqUrl = request.getRequestURL();
                    if(request.getQueryString() != null){
                        reqUrl.append('?').append(request.getQueryString());
                    }
                    requestAttributes.put("containers", getContainers(client, PathUtils.getParentPath(path), rv, requestQueryParams));
                } else if (ccCount > 0){
                    requestAttributes.
                            put("containers", getContainers(client, path, rv, requestQueryParams));
                }
                if (ccCount == 0){
                    if(!requestAttributes.containsKey("containers")){
                        requestAttributes.put("containers", getContainers(client, PathUtils.getParentPath(path), rv, requestQueryParams));
                    }
                    requestAttributes.put("selected", target);
                }
            }

            requestAttributes.put("parentURL", request.getPathInfo());
        }
        return requestAttributes;
    }
    
    
    public static HashMap<String, Object> collectSearchAttributes(HttpServletRequest request)
            throws ServletException, IOException{

        HashMap<String, Object> requestAttributes = collectBasicAttributes(request);
        HashMap<String, List<String>> requestQueryParams = getQueryParams(request);
        RequestView rv = new RequestView(RecordType.FOLDER, null);
        String searchPath = request.getPathInfo();
        Client client = getClient(request);        
        
        
        int offset = requestQueryParams.containsKey("offset")
                ? Integer.valueOf(requestQueryParams.get("offset").get(0)) : 0;

        int max = requestQueryParams.containsKey("max")
                ? Integer.valueOf(requestQueryParams.get("max").get(0)) : DEFAULT_MAX;

        ArrayList<DatacatNode> datasets = new ArrayList<>();
        DatasetResultSetModel searchResults = getDatasets(client, searchPath, rv, requestQueryParams, offset, max);
        for(DatacatNode d: searchResults.getResults()){
            if(!d.getType().isContainer()){
                datasets.add(d);
            }
        }
        requestAttributes.put("datasets", datasets);
        requestAttributes.put("datasetCount", searchResults.getCount());
        // Paging
        StringBuffer reqUrl = request.getRequestURL();
        if(request.getQueryString() != null){
            reqUrl.append('?').append(request.getQueryString());
        }

        requestAttributes.put("parentURL", request.getPathInfo());
        return requestAttributes;
    }

    public static HashMap<String, Object> collectBasicAttributes(HttpServletRequest request){
        HashMap<String, Object> requestAttributes = new HashMap<>();

        String endPoint = request.getContextPath() + request.getServletPath();
        requestAttributes.put("endPoint", endPoint);

        String base = endPoint.substring(0, endPoint.lastIndexOf("/"));
        requestAttributes.put("applicationBase", base);
        requestAttributes.put("contextPath", request.getContextPath());
        requestAttributes.put("endPoint", endPoint);
        return requestAttributes;
    }
    
    public static HashMap<String, List<String>> getQueryParams(HttpServletRequest request){
        HashMap<String, List<String>> requestQueryParams = new HashMap<>();
        Map<String, String[]> params = request.getParameterMap();
        for(Map.Entry<String, String[]> e: params.entrySet()){
            requestQueryParams.put(e.getKey(), Arrays.asList((String[]) e.getValue()));
        }
        return requestQueryParams;
    }

    private static DatasetResultSetModel getDatasets(Client c, String path, RequestView requestView,
            HashMap<String, List<String>> queryParams, int offset, int max){
        String filter = queryParams.containsKey("filter") ? queryParams.get("filter").get(0) : "";
        String sort[] = queryParams.containsKey("sort") ? queryParams.get("sort").
                toArray(new String[0]) : null;
        DatasetView dsView = requestView.getDatasetView(DatasetView.MASTER);
        DatasetResultSetModel results = c.searchForDatasets(path, Integer.toString(dsView.getVersionId()),
                dsView.getSite(), filter, sort, null, offset, max);

        return results;
    }

    private static List<DatacatNode> getContainers(Client c, String path, RequestView requestView,
            HashMap<String, List<String>> queryParams){
        int max = queryParams.containsKey("cmax") ? Integer.valueOf(queryParams.get("cmax").get(0)) : 100000;
        int offset = queryParams.containsKey("coffset") ? Integer.
                valueOf(queryParams.get("coffset").get(0)) : 0;

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
