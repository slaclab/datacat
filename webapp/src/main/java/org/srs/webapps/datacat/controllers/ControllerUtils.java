package org.srs.webapps.datacat.controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.srs.webapps.datacat.model.NodeTargetModel;
import org.srs.webapps.datacat.model.ApplicationUriInfo;

/**
 *
 * @author bvan
 */
public class ControllerUtils {
    static final int DEFAULT_MAX = 100;
    
    
    private static ClientRequestFilter getCasClientFilter(HttpServletRequest request){
        Map<String, Object> headers = new HashMap<>();
        // Client configuration can go here if testing
        String srsClientId = request.getServletContext().getInitParameter("org.srs.datacat.srs_client_id");
        if(srsClientId != null){
            headers.put("x-client-id", srsClientId); // This web applications has a clientId which should be trusted
        }

        headers.put("x-cas-username", request.getSession().getAttribute("userName"));
        return new HeaderFilter(headers);
    }
    
    private static ClientRequestFilter getPassThroughFilter(HttpServletRequest request){
        final Set<String> passThroughAuths = new HashSet<>(Arrays.asList(
                HttpServletRequest.BASIC_AUTH,
                HttpServletRequest.DIGEST_AUTH
        ));
        Map<String, Object> headers = new HashMap<>();
        if(passThroughAuths.contains(request.getAuthType())){
            headers.put("Authorization", request.getHeader("Authorization"));
        }
        if(request.getHeader("Cookie") != null){
            headers.put("Cookie", request.getHeader("Cookie"));
        }
        return new HeaderFilter(headers);
    }
    
    private static ClientRequestFilter getClientFilter(HttpServletRequest request){
        if(request.getSession().getAttribute("userName") != null){
            return getCasClientFilter(request);
        }
        return getPassThroughFilter(request);
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

    public static NodeTargetModel buildModel(HttpServletRequest request, ApplicationUriInfo info, boolean includeDatasets)
            throws IOException{

        NodeTargetModel requestModel = buildBasicModel(request, info);
        HashMap<String, List<String>> requestQueryParams = getQueryParams(request);
        RequestView rv = new RequestView(RecordType.FOLDER, null);
        String path = info.getDatacatPath();
        // This Assumes all REST requests are routed to the same base URL
        Client client = getClient(request); 
        if(path == null || path.length() <= 1){
            requestModel.setParentURL("/");
            requestModel.setContainers(getContainers(client, "/", rv, requestQueryParams));
            requestModel.setPath(null);
        } else {
            DatacatNode target = client.getObject(path, "current", "all");

            if(target.getType().isContainer()){
                target = client.getContainer(path, "dataset");
            }
            String parentPath = PathUtils.getParentPath(target.getPath());
            if(parentPath.length() > 1){
                requestModel.setParent(client.getContainer(parentPath, "dataset"));
            }

            requestModel.setTarget(target);
            DcAclEntry e = client.getPermissions(path, null);
            requestModel.setWritable(e.getPermissions().contains(DcPermissions.WRITE));
            requestModel.setDeletable(e.getPermissions().contains(DcPermissions.DELETE));
            requestModel.setInsertable(e.getPermissions().contains(DcPermissions.INSERT));
            if(target.getType().isContainer()){
                DatasetStat t = (DatasetStat) ((DatasetContainer) target).getStat();
                long ccCount = t.getGroupCount() + t.getFolderCount();
                long dsCount = t.getDatasetCount();

                int offset = requestQueryParams.containsKey("offset")
                        ? Integer.valueOf(requestQueryParams.get("offset").get(0)) : 0;

                int max = requestQueryParams.containsKey("max")
                        ? Integer.valueOf(requestQueryParams.get("max").get(0)) : DEFAULT_MAX;

                if(includeDatasets && dsCount > 0){
                    ArrayList<DatacatNode> datasets = new ArrayList<>();
                    DatasetResultSetModel searchResults = getDatasets(client, path, rv, requestQueryParams, offset, max);
                    for(DatacatNode d: searchResults.getResults()){
                        if(!d.getType().isContainer()){
                            datasets.add(d);
                        }
                    }
                    requestModel.setDatasets(datasets);
                    requestModel.setDatasetCount(searchResults.getCount());
                    // Paging
                    StringBuffer reqUrl = request.getRequestURL();
                    if(request.getQueryString() != null){
                        reqUrl.append('?').append(request.getQueryString());
                    }
                    requestModel.setContainers(getContainers(client, PathUtils.getParentPath(path), rv, requestQueryParams));
                } else if (ccCount > 0){
                    requestModel.setContainers(getContainers(client, path, rv, requestQueryParams));
                }
                if (ccCount == 0){
                    if(requestModel.getContainers() == null){
                        requestModel.setContainers(getContainers(client, PathUtils.getParentPath(path), rv, requestQueryParams));
                    }
                    requestModel.setSelected(target);
                }
            }

            requestModel.setParentURL(request.getPathInfo());
        }
        return requestModel;
    }
    
    
    public static NodeTargetModel buildSearchModel(HttpServletRequest request, ApplicationUriInfo info)
            throws ServletException, IOException{

        NodeTargetModel targetModel = buildBasicModel(request, info);
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
        targetModel.setDatasets(datasets);
        
        targetModel.setDatasetCount(searchResults.getCount());
        // Paging
        StringBuffer reqUrl = request.getRequestURL();
        if(request.getQueryString() != null){
            reqUrl.append('?').append(request.getQueryString());
        }

        targetModel.setParentURL(request.getPathInfo());
        return targetModel;
    }
    

    public static NodeTargetModel buildBasicModelOld(HttpServletRequest request){
        NodeTargetModel requestAttributes = new NodeTargetModel();

        String endPoint = request.getContextPath() + request.getServletPath();

        String base = endPoint.substring(0, endPoint.lastIndexOf("/"));
        requestAttributes.setContextPath(request.getContextPath());
        requestAttributes.setApplicationBase(base);
        requestAttributes.setEndPoint(endPoint);
        return requestAttributes;
    }
    
    public static NodeTargetModel buildBasicModel(HttpServletRequest request, ApplicationUriInfo model){
        NodeTargetModel requestAttributes = new NodeTargetModel();
        requestAttributes.setContextPath(model.getApplicationRoot());
        requestAttributes.setApplicationBase(model.getDisplayRoot());
        requestAttributes.setEndPoint(model.getResourceRoot());
        return requestAttributes;
    }

    
    private static HashMap<String, List<String>> getQueryParams(HttpServletRequest request){
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
