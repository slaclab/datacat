/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacat.rest.resources;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

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

import org.srs.rest.shared.RestException;
import org.srs.rest.shared.metadata.MetadataEntry;


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
    public Response getRootBean(@DefaultValue("basic") @QueryParam("stat") StatTypeWrapper statType) throws IOException{
        return getBean("", null, statType.getEnum());
    }
    
    @GET
    @Path(idRegex)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response getBean(@PathParam("id") List<PathSegment> pathSegments,
            @DefaultValue("basic") @QueryParam("stat") StatTypeWrapper statTypeWrapper) throws IOException{
        
        StatType statType = statTypeWrapper.getEnum();
        HashMap<String, List<String>> matrixParams = new HashMap<>();
        String path = "";
        for(PathSegment s: pathSegments){
            path = path + "/" + s.getPath();
            matrixParams.putAll(s.getMatrixParameters());
        }
        return getBean(path, matrixParams, statType);
    }
    
    public Response getBean(String path, HashMap<String,List<String>> matrixParams, 
            StatType statType) throws IOException{
        DcPath dcp = getProvider().getPath(DcUriUtils.toFsUri(path, null, "SRS"));
        try {
            DcFile file = Files.readAttributes(dcp, DcFile.class);
            DatacatObject ret;
            RequestView rv = new RequestView(file.getObject().getType(), matrixParams);
            if(file.isRegularFile()){
                ret = file.getAttributeView(DatasetViewProvider.class).withView(rv);
            } else {
                ret = file.getAttributeView(ContainerViewProvider.class).withView(statType);
            }
            if(rv.getPrimaryView() == RequestView.METADATA){
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
            }
            return Response.ok( new GenericEntity(ret, DatacatObject.class) ).build();
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

}
