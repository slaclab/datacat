/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacat.rest.resources;


import java.io.IOException;

import java.nio.file.Files;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.srs.datacat.rest.App;
import org.srs.datacat.rest.ConnectionResource;
import org.srs.datacat.model.RequestView;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.container.BasicStat.StatType;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcUriUtils;
import org.srs.datacat.vfs.attribute.ContainerViewProvider;
import org.srs.datacat.vfs.attribute.DatasetViewProvider;

import org.srs.rest.shared.HumanPath;


/**
 * The path resource will uniquely identify the object at the given path and
 * return it.
 * @author bvan
 */
@Path("/path")
public class PathResource extends ConnectionResource {
    private final String idRegex = "{id: [\\w\\d\\-_\\./]+}";
    private final String idPath = "[/path]*/{id}";
    
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
    public DatacatObject getRootBean(@DefaultValue("basic") @QueryParam("stat") StatTypeWrapper statType) throws IOException{
        return getBean("", statType);
    }
    
    @GET
    @Path(idRegex)
    @HumanPath(idPath)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public DatacatObject getBean(@PathParam("id") String path,
            @DefaultValue("basic") @QueryParam("stat") StatTypeWrapper statType) throws IOException{
        path = "/" + path;
        DcPath dcp = App.fsProvider.getPath(DcUriUtils.toFsUri(path, null, "SRS"));
        DcFile file = Files.readAttributes(dcp, DcFile.class);
        DatacatObject ret;
        if(file.getDatacatType() == DatacatObject.Type.DATASET){
            ret = file.getAttributeView(DatasetViewProvider.class).withView(new RequestView(DatacatObject.Type.DATASET, null));
        } else {
            ret = file.getAttributeView(ContainerViewProvider.class).withView(statType.getEnum());
        }
        return ret;
    }

}
