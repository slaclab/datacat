/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacat.rest.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.srs.datacat.rest.BaseResource;
import org.srs.datacat.model.RequestView;
import org.srs.datacat.rest.resources.PathResource.StatTypeWrapper;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcUriUtils;
import org.srs.datacat.vfs.attribute.ContainerViewProvider;
import org.srs.datacat.vfs.attribute.DatasetViewProvider;
import org.srs.rest.shared.RestException;
import org.srs.vfs.AbstractFsProvider;


/**
 * The children resource will return objects that belong to supplied parent
 * path.
 * @author bvan
 */
@Path("/children")
public class ChildrenResource extends BaseResource {
    
    private final String idRegex = "{id: [\\w\\d\\-_\\./]+}";
    
    @GET
    @Path(idRegex)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public List<DatacatObject> getChildren(@PathParam("id") String path, 
        @DefaultValue("true") @QueryParam("datasets") boolean withDs,
        @DefaultValue("none") @QueryParam("stat") StatTypeWrapper statType,
        @DefaultValue("100000") @QueryParam("max") int max,
        @DefaultValue("0") @QueryParam("offset") int offset){

        path = "/" + path;
        DcPath dirPath = getProvider().getPath(DcUriUtils.toFsUri(path, null, "SRS"));
        try {
            Files.readAttributes(dirPath, DcFile.class);
        } catch (FileNotFoundException ex){
             throw new RestException("File doesn't exist", 404);
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
        
        RequestView rv = new RequestView(DatacatObject.Type.DATASET,null);
        ArrayList<DatacatObject> retList = new ArrayList<>();
        try (DirectoryStream<java.nio.file.Path> stream = getProvider()
                .newOptimizedDirectoryStream(dirPath, AbstractFsProvider.AcceptAllFilter, 
                    max, rv.getDatasetView())){
            Iterator<java.nio.file.Path> iter = stream.iterator();
            int skipped = 0;
            while(iter.hasNext() && retList.size() < max){
                java.nio.file.Path p = iter.next();
                DcFile file = Files.readAttributes(p, DcFile.class);
                if(!withDs && file.isRegularFile()){
                    continue;
                }
                DatacatObject ret;
                if(file.isRegularFile()){
                    try {
                        ret = file.getAttributeView(DatasetViewProvider.class).withView(rv);
                    } catch (FileNotFoundException ex){
                        continue;
                    }
                } else {
                    ret = file.getAttributeView(ContainerViewProvider.class).withView(statType.getEnum());
                }
                if(skipped >= offset){
                    retList.add(ret);
                } else {
                    skipped++;
                }
            }
        } catch (NotDirectoryException ex){
            throw new RestException( "File exists, but Path is not a directory", 404);
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
        return retList;
    }

}
