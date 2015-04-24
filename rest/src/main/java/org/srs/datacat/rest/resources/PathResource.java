
package org.srs.datacat.rest.resources;


import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;

import java.nio.file.NotDirectoryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.dataset.DatasetWithViewModel;
import org.srs.datacat.model.HasMetadata;
import org.srs.datacat.model.container.ContainerStat;

import org.srs.datacat.rest.BaseResource;
import org.srs.datacat.shared.RequestView;
import static org.srs.datacat.rest.BaseResource.OPTIONAL_EXTENSIONS;
import org.srs.datacat.shared.BasicStat.StatType;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcUriUtils;
import org.srs.datacat.vfs.attribute.ContainerViewProvider;
import org.srs.datacat.vfs.attribute.DatasetViewProvider;

import org.srs.datacat.rest.RestException;
import org.srs.datacat.shared.metadata.MetadataEntry;
import org.srs.datacat.vfs.DcFileSystemProvider;
import org.srs.vfs.AbstractFsProvider;


/**
 * The path resource will uniquely identify the object at the given path and
 * return it.
 * @author bvan
 */
@Path("/path" +  OPTIONAL_EXTENSIONS)
public class PathResource extends BaseResource {
    private final String idRegex = "{id: [%\\w\\d\\-_\\./]+}";
    
    public static class StatTypeWrapper {
        private final StatType value;
        private StatTypeWrapper(StatType e){ this.value = e; }
        
        public static StatTypeWrapper valueOf(String value){
            return new StatTypeWrapper(StatType.valueOf( value.toUpperCase() ) );
        }
        public StatType getEnum(){
            return this.value;
        }
    }
    
    private UriInfo ui;
    private List<PathSegment> pathSegments;
    private String requestPath;
    private HashMap<String, List<String>> requestMatrixParams = new HashMap<>();
    private HashMap<String, List<String>> requestQueryParams = new HashMap<>();
    
    public PathResource(@PathParam("id") List<PathSegment> pathSegments, @Context UriInfo ui){
        this.pathSegments = pathSegments;
        this.ui = ui;
        String path = "";
        if(pathSegments != null && !pathSegments.isEmpty()){
            for(PathSegment s: pathSegments){
                path = path + "/" + s.getPath();
                requestMatrixParams.putAll(s.getMatrixParameters());
            }   
        } else {
            path = "/";
            for(PathSegment s: ui.getPathSegments()){
                requestMatrixParams.putAll(s.getMatrixParameters());
            }
        }
        requestPath = path;
        requestQueryParams.putAll(ui.getQueryParameters());
    }
    
    @HEAD
    public Response getHead(@DefaultValue("false") @QueryParam("refresh") boolean refresh) throws IOException{
        DcPath dcp = getProvider().getPath(DcUriUtils.toFsUri(requestPath, getUser(), "SRS"));
        try {
            if(refresh){
                //getProvider().getCache().removeFile(dcp);
            }
            DcFile file = getProvider().getFile(dcp);
            if(file.isRegularFile()){
                RequestView rv = new RequestView(file.getObject().getType(), requestMatrixParams);
                file.getAttributeView(DatasetViewProvider.class).withView(rv.getDatasetView(), rv.includeMetadata());
            }
            return Response.ok().build();
        } catch (IllegalArgumentException ex){
            throw new RestException(ex, 400 , "Unable to correctly process view", ex.getMessage());
        } catch (NoSuchFileException ex){
             throw new RestException(ex,404 , "File doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
    }
    
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response getRootBean(@DefaultValue("basic") @QueryParam("stat") StatTypeWrapper statType, 
            @DefaultValue("false") @QueryParam("refresh") boolean refresh) throws IOException{
        return getBean(requestPath, requestMatrixParams, requestQueryParams, refresh);
    }
    
    @GET
    @Path(idRegex)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response getBean(@DefaultValue("false") @QueryParam("refresh") boolean refresh) throws IOException{
        return getBean(requestPath, requestMatrixParams, requestQueryParams, refresh);
    }
    
    public Response getBean(String path, HashMap<String,List<String>> matrixParams, 
            HashMap<String, List<String>> extraQueryParams, boolean refresh) throws IOException{
        List<String> stl = matrixParams.get( "stat");
        String st = stl != null && !stl.isEmpty() ? stl.get(0).toUpperCase() : null;
        Class<? extends ContainerStat> statType = ContainerStat.class;
        if(st != null){
            statType = getProvider().getModelProvider().getStatByName(st);
        }
        DcPath dcp = getProvider().getPath(DcUriUtils.toFsUri(path, getUser(), "SRS"));
        try {
            if(refresh){
                //getProvider().getCache().removeFile(dcp);
            }
            DcFile file = getProvider().getFile(dcp);
            DatacatNode ret;
            RequestView rv = new RequestView(file.getObject().getType(), matrixParams);
            if(file.isRegularFile()){
                ret = file.getAttributeView(DatasetViewProvider.class).withView(rv.getDatasetView(), rv.includeMetadata());
            } else {
                ret = file.getAttributeView(ContainerViewProvider.class).withView(statType);
            }
            switch(rv.getPrimaryView()){
                case RequestView.CHILDREN:
                    return getChildren(dcp, file, rv, extraQueryParams );
                case RequestView.METADATA:
                    List<MetadataEntry> entries = null;
                    if(rv.containsKey("metadata") && ret instanceof HasMetadata){
                        entries = MetadataEntry.toList(((HasMetadata) ret).getMetadataMap());
                    } else if(rv.containsKey("versionMetadata") && ret instanceof DatasetWithViewModel){
                        entries = MetadataEntry.toList(((DatasetWithViewModel) ret).getViewInfo()
                                .getVersion().getMetadataMap());
                    }
                    if(entries == null){
                        return Response.noContent().build();
                    }
                    return Response.ok( new GenericEntity<List<MetadataEntry>>(entries){} ).build();
                default:
                    return Response.ok( new GenericEntity(ret, DatacatNode.class) ).build();
            }
        } catch (IllegalArgumentException ex){
            throw new RestException(ex, 400 , "Unable to correctly process view", ex.getMessage());
        } catch (NoSuchFileException ex){
             throw new RestException(ex,404 , "File doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
    }
 
    public Response getChildren(DcPath dcp, DcFile dirFile, RequestView requestView, HashMap<String, List<String>> queryParams){
        boolean withDs = queryParams.containsKey("datasets") ? Boolean.valueOf( queryParams.get("datasets").get(0)) : true;
        Class<? extends ContainerStat> statType = null;
        if(queryParams.containsKey("stat")){
            statType = getProvider().getModelProvider().getStatByName(queryParams.get("stat").get(0).toLowerCase());
        }
        int max = queryParams.containsKey("max") ? Integer.valueOf( queryParams.get("max").get(0)) :100000;
        int offset = queryParams.containsKey("offset") ? Integer.valueOf( queryParams.get("offset").get(0)) :0;
        boolean showCount = queryParams.containsKey("showCount") ? Boolean.valueOf( queryParams.get("showCount").get(0)) :false;
    
        List<DatacatNode> retList = new ArrayList<>();
        int count = 0;
        DirectoryStream<DcPath> stream = null;
        try {
            String childrenView = requestView.get("children");
            if("containers".equals(childrenView)){
                stream = getProvider()
                        .unCachedDirectoryStream(dirFile.getPath(), AbstractFsProvider.AcceptAllFilter, null, false);
            } else {
                stream = getProvider()
                        .newOptimizedDirectoryStream(dirFile.getPath(), DcFileSystemProvider.ACCEPT_ALL_FILTER, 
                            max, requestView.getDatasetView(DatasetView.CURRENT_ALL));
            }
            Iterator<DcPath> iter = stream.iterator();
            
            while(iter.hasNext() && (retList.size() < max || showCount)){
                DcPath p = iter.next();
                DcFile file = null;
                try {
                    file = getProvider().getFile(p.withUser(dcp.getUserName()));
                } catch (AccessDeniedException ex){
                    continue;
                }
                if(!withDs && file.isRegularFile()){
                    continue;
                }
                if(count >= offset && retList.size() < max){
                    DatacatNode ret;
                    if(file.isRegularFile()){
                        try {
                            ret = file.getAttributeView(DatasetViewProvider.class).withView(requestView.getDatasetView(), requestView.includeMetadata());
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
        } catch (NotDirectoryException ex){
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("File exists, but Path is not a directory").build();
        } catch (IOException ex){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error accessing the file system: " + ex.getMessage()).build();
        } finally {
            try {
                if(stream != null){
                    stream.close();
                }
            } catch(IOException ex) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error accessing the file system: " + ex.getMessage()).build();
            }
            
        }
        
        String start = Integer.toString(offset);
        String end = Integer.toString(offset+ (retList.size() - 1));
        String len= showCount ? Integer.toString(count - 1) : "*";
        Response resp = Response
                .ok( new GenericEntity<List<DatacatNode>>(retList) {})
                .header( "Content-Range", String.format("items %s-%s/%s", start, end, len))
                .build();
        return resp;
    }

}
