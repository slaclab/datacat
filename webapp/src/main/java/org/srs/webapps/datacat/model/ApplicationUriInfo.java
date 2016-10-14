package org.srs.webapps.datacat.model;

import java.util.List;
import java.util.Map;
import javax.ws.rs.Path;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

/**
 *
 * @author bvan
 */
public class ApplicationUriInfo {

    private String applicationRoot;
    private String displayRoot;
    private String resourceRoot;
    private String datacatPath;

    public ApplicationUriInfo(){ }

    /**
     * Return the root of this application. Normally this is equivalent to the contextPath.
     *
     * @return A relative path to the host name in the URI.
     */
    public String getApplicationRoot(){
        return applicationRoot;
    }

    public void setApplicationRoot(String baseUrl){
        this.applicationRoot = baseUrl;
    }

    /**
     * Get the display root. This is effectively applicationRoot + "/display"
     * @return A relative path to the host name in the URI.
     */
    public String getDisplayRoot(){
        return displayRoot;
    }

    public void setDisplayRoot(String displayUrl){
        this.displayRoot = displayUrl;
    }

    /**
     * Get the Resource Root. This is effectively applicationRoot + "/display/" + resourceName
     * @return A relative URI for the application.
     */
    public String getResourceRoot(){
        return resourceRoot;
    }

    public void setResourceRoot(String resourceUrl){
        this.resourceRoot = resourceUrl;
    }

    /**
     * Return the datacatalog path. This will not include any matrix parameters.
     * @return 
     */
    public String getDatacatPath(){
        return datacatPath;
    }

    public void setDatacatPath(String datacatPath){
        this.datacatPath = datacatPath;
    }

    public static String pathHelper(List<PathSegment> pathSegments,
            Map<String, List<String>> matrixParams){
        String path = "";
        if(pathSegments != null && !pathSegments.isEmpty()){
            for(PathSegment s: pathSegments){
                path = path + "/" + s.getPath();
                if(matrixParams != null){
                    matrixParams.putAll(s.getMatrixParameters());
                }
            }
        } else {
            path = "/";
        }
        return path;
    }

    public static ApplicationUriInfo getUriModel(UriInfo uriInfo, Class<?> resource, String path){
        String resourcePath = resource.getAnnotation(Path.class).value();
        ApplicationUriInfo uriModel = new ApplicationUriInfo();
        String relativeBase = uriInfo.getBaseUri().getPath();
        if(relativeBase.endsWith("/")){
            relativeBase = relativeBase.substring(0, relativeBase.lastIndexOf("/"));
        }
        
        uriModel.setApplicationRoot(relativeBase);
        uriModel.setDisplayRoot(relativeBase + "/display");
        uriModel.setResourceRoot(relativeBase + "/" + resourcePath);
        uriModel.setDatacatPath(path);
        return uriModel;
    }

}
