/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacat.rest.resources;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;

import java.nio.file.NotDirectoryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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

import org.srs.datacat.rest.BaseResource;
import org.srs.datacat.model.RequestView;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.container.BasicStat.StatType;
import org.srs.datacat.shared.dataset.FlatDataset;
import org.srs.datacat.shared.dataset.FullDataset;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcUriUtils;
import org.srs.datacat.vfs.attribute.ContainerViewProvider;
import org.srs.datacat.vfs.attribute.DatasetViewProvider;

import org.srs.datacat.rest.RestException;
import org.srs.rest.shared.metadata.MetadataEntry;
import org.srs.vfs.AbstractFsProvider;


/**
 * The path resource will uniquely identify the object at the given path and
 * return it.
 * @author bvan
 */
@Path("/path")
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
    
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response getRootBean(@DefaultValue("basic") @QueryParam("stat") StatTypeWrapper statType, 
            @DefaultValue("false") @QueryParam("refresh") boolean refresh) throws IOException{
        return getBean("", new HashMap<String, List<String>>(), new HashMap<String, List<String>>(), refresh);
    }
    
    @GET
    @Path(idRegex)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response getBean(@PathParam("id") List<PathSegment> pathSegments, 
            @DefaultValue("false") @QueryParam("refresh") boolean refresh, @Context UriInfo ui) throws IOException{
        HashMap<String, List<String>> matrixParams = new HashMap<>();
        HashMap<String, List<String>> queryParams = new HashMap<>();
        String path = "";
        for(PathSegment s: pathSegments){
            path = path + "/" + s.getPath();
            matrixParams.putAll(s.getMatrixParameters());
        }
        queryParams.putAll(ui.getQueryParameters());
        return getBean(path, matrixParams, queryParams, refresh);
    }
    
    public Response getBean(String path, HashMap<String,List<String>> matrixParams, 
            HashMap<String, List<String>> extraQueryParams, boolean refresh) throws IOException{
        List<String> stl = matrixParams.get( "stat");
        String st = stl != null && !stl.isEmpty() ? stl.get(0).toUpperCase() : null;
        StatType statType = st != null ? StatType.valueOf(st) : StatType.BASIC;
        DcPath dcp = getProvider().getPath(DcUriUtils.toFsUri(path, null, "SRS"));
        try {
            if(refresh){
                getProvider().getCache().removeFile(dcp);
            }
            DcFile file = getProvider().getFile(dcp);
            DatacatObject ret;
            RequestView rv = new RequestView(file.getObject().getType(), matrixParams);
            if(file.isRegularFile()){
                ret = file.getAttributeView(DatasetViewProvider.class).withView(rv);
            } else {
                ret = file.getAttributeView(ContainerViewProvider.class).withView(statType);
            }
            switch(rv.getPrimaryView()){
                case RequestView.CHILDREN:
                    return getChildren( file, rv, extraQueryParams );
                case RequestView.METADATA:
                    List<MetadataEntry> entries = null;
                    if(rv.containsKey("metadata")){
                        entries = ret.getMetadata();
                    } else if(rv.containsKey( "versionMetadata")){
                        if(ret instanceof FullDataset){
                            entries = ((FullDataset) ret).getVersionMetadata();
                        } else if(ret instanceof FlatDataset){
                            entries = ((FlatDataset) ret).getVersionMetadata();
                        }
                    }
                    if(entries == null){
                        return Response.noContent().build();
                    }
                    return Response.ok( new GenericEntity<List<MetadataEntry>>(entries){} ).build();
                default:
                    return Response.ok( new GenericEntity(ret, DatacatObject.class) ).build();
            }
        } catch (IllegalArgumentException ex){
            throw new RestException(ex, 400 , "Unable to correctly process view", ex.getMessage());
        } catch (FileNotFoundException ex){
             throw new RestException(ex,404 , "File doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
    }
 
    public Response getChildren(DcFile dirFile, RequestView requestView, HashMap<String, List<String>> queryParams){
        boolean withDs = queryParams.containsKey("datasets") ? Boolean.valueOf( queryParams.get("datasets").get(0)) : true;
        StatType statType = queryParams.containsKey("stat") ? StatType.valueOf( queryParams.get("stat").get(0).toUpperCase()): StatType.NONE;
        int max = queryParams.containsKey("max") ? Integer.valueOf( queryParams.get("max").get(0)) :100000;
        int offset = queryParams.containsKey("offset") ? Integer.valueOf( queryParams.get("offset").get(0)) :0;
        boolean showCount = queryParams.containsKey("showCount") ? Boolean.valueOf( queryParams.get("showCount").get(0)) :false;
    
        List<DatacatObject> retList = new ArrayList<>();
        int count = 0;
        try (DirectoryStream<java.nio.file.Path> stream = getProvider()
                .newOptimizedDirectoryStream(dirFile.getPath(), AbstractFsProvider.AcceptAllFilter, 
                    max, requestView.getDatasetView())){
            Iterator<java.nio.file.Path> iter = stream.iterator();
            
            while(iter.hasNext() && (retList.size() < max || showCount)){
                java.nio.file.Path p = iter.next();
                DcFile file = getProvider().getFile(p);
                if(!withDs && file.isRegularFile()){
                    continue;
                }
                if(count >= offset && retList.size() < max){
                    DatacatObject ret;
                    if(file.isRegularFile()){
                        try {
                            ret = file.getAttributeView(DatasetViewProvider.class).withView(requestView);
                        } catch (FileNotFoundException ex){
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
        }
        
        String start = Integer.toString(offset);
        String end = Integer.toString(offset+ (retList.size() - 1));
        String len= showCount ? Integer.toString(count - 1) : "*";
        Response resp = Response
                .ok( new GenericEntity<List<DatacatObject>>(retList) {})
                .header( "Content-Range", String.format("items %s-%s/%s", start, end, len))
                .build();
        return resp;
    }

}
