package org.srs.webapps.datacat.controllers;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.srs.datacat.client.Client;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.RecordType;
import org.srs.datacat.shared.DatasetStat;
import org.srs.datacat.shared.RequestView;
import org.srs.vfs.PathUtils;

/**
 *
 * @author bvan
 */
public class ControllerUtils {
    static final int DEFAULT_MAX = 100;
    
    public static Client getClient(HttpServletRequest request) throws MalformedURLException{
        String localUrl = String.format("%s://%s:%s%s/r",
                request.getScheme(), request.getServerName(), request.getServerPort(),
                request.getContextPath());
        return new Client(localUrl, request);
    }

    public static HashMap<String, Object> collectAttributes(HttpServletRequest request, boolean withDatasets)
            throws ServletException, MalformedURLException{

        HashMap<String, Object> requestAttributes = new HashMap<>();

        String endPoint = request.getContextPath() + request.getServletPath();
        requestAttributes.put("endPoint", endPoint);

        String base = endPoint.substring(0, endPoint.lastIndexOf("/"));
        requestAttributes.put("applicationBase", base);
        requestAttributes.put("endPoint", endPoint);
        // This Assumes all REST requests are routed to the same base URL
        Client client = getClient(request);
        HashMap<String, List<String>> requestQueryParams = new HashMap<>();
        Map<String, String[]> params = request.getParameterMap();
        for(Map.Entry<String, String[]> e: params.entrySet()){
            requestQueryParams.put(e.getKey(), Arrays.asList((String[]) e.getValue()));
        }

        RequestView rv = new RequestView(RecordType.FOLDER, null);
        if(request.getPathInfo() == null || request.getPathInfo().length() == 1){
            requestAttributes.put("parentURL", "/");
            requestAttributes.put("containers", getContainers(client, "/", rv, requestQueryParams));
            requestAttributes.put("path", null);
        } else {
            String path = request.getPathInfo();
            DatacatNode target = client.getObject(path);

            if(target.getType().isContainer()){
                target = client.getContainer(path, "dataset");
            }
            String parentPath = PathUtils.getParentPath(target.getPath());
            if(parentPath.length() > 1){
                requestAttributes.put("parent", client.getContainer(parentPath, "dataset"));
            }

            requestAttributes.put("target", target);
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
                    List<DatasetModel> results = getDatasets(client, path, rv, requestQueryParams, offset, max);
                    for(DatacatNode d: results){
                        if(!d.getType().isContainer()){
                            datasets.add(d);
                        }
                    }
                    requestAttributes.put("datasets", datasets);
                    requestAttributes.put("datasetCount", results.size());
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

    private static List<DatasetModel> getDatasets(Client c, String path, RequestView requestView,
            HashMap<String, List<String>> queryParams, int offset, int max){
        String filter = queryParams.containsKey("filter") ? queryParams.get("filter").get(0) : "";
        String sort = queryParams.containsKey("sort") ? queryParams.get("sort").
                toArray(new String[0])[0] : null;
        DatasetView dsView = requestView.getDatasetView(DatasetView.MASTER);
        List<DatasetModel> results = c.searchForDatasets(path, Integer.toString(dsView.getVersionId()),
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
