
package org.srs.datacat.rest.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NotDirectoryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.RequestView;
import org.srs.datacat.rest.BaseResource;
import org.srs.datacat.rest.FormParamConverter;
import org.srs.datacat.rest.RestException;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.dataset.FlatDataset;
import org.srs.datacat.shared.dataset.FullDataset;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcFileSystemProvider.DcFsException;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcUriUtils;  
import org.srs.datacat.vfs.attribute.DatasetOption;
import org.srs.datacat.vfs.attribute.DatasetViewProvider;

/**
 * The datasets resource will return all datasets under a given path.
 * You can recurse a path to find all datasets that will fall underneath that
 * path.
 * @author bvan
 */
@Path("/datasets")
public class DatasetsResource extends BaseResource  { 
    private final String idRegex = "{id: [%\\w\\d\\-_\\./]+}";
    
    @Context private HttpServletResponse response;
    
    private UriInfo ui;
    private List<PathSegment> pathSegments;
    private String requestPath;
    private HashMap<String, List<String>> requestMatrixParams = new HashMap<>();
    private HashMap<String, List<String>> requestQueryParams = new HashMap<>();
    
    public DatasetsResource(@PathParam("id") List<PathSegment> pathSegments, @Context UriInfo ui){
        this.ui = ui;
        String path = "";
        if(pathSegments != null){
            for(PathSegment s: pathSegments){
                path = path + "/" + s.getPath();
                requestMatrixParams.putAll(s.getMatrixParameters());
            }   
        }
        requestPath = path;
        requestQueryParams.putAll(ui.getQueryParameters());
    }
   
    @GET
    @Path(idRegex)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public String createDataset(@PathParam("id") String path, 
            @MatrixParam("v") List<String> versions,
            @MatrixParam("l") List<String> locations){
        System.out.println("hi");
        System.out.println(path);
        System.out.println(versions.toString());
        System.out.println(locations.toString());
        return versions + " " + locations;
    }
    
    /**
     * Dataset Creation entrypoint.
     * @param parent
     * @param formParams
     * @return
     * @throws IOException 
     */
    @POST
    @Path(idRegex)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response createDataset(MultivaluedMap<String, String> formParams) throws IOException{
        
        Dataset.Builder builder = FormParamConverter.getDatasetBuilder(formParams);
        DcPath targetPath = getProvider().getPath(DcUriUtils.toFsUri(requestPath, null, "SRS"));
        DatacatObject.Type targetType = null;
        Dataset dsReq = builder.build();
        DcFile parentFile = null;
        RequestView rv = null;
        try {
            parentFile = getProvider().getFile(targetPath);
            rv = new RequestView(parentFile.getObject().getType(), requestMatrixParams);
            targetType = getTargetType(parentFile, rv);
        } catch (FileNotFoundException ex) {
            throw new RestException(ex ,404, "File doesn't exist", ex.getMessage());
        } catch (IllegalArgumentException ex){
            throw new RestException(ex, 400, "Unable to validate request view", ex.getMessage());
        }
                        
        ArrayList<DatasetLocation> requestLocations = new ArrayList<>();
        DatasetVersion requestVersion = decomposeDataset(dsReq, requestLocations, true);
        
        Set<DatasetOption> options = new HashSet<>();
        boolean useDefaultVersion = false;
        switch(targetType){
            case GROUP:
            case FOLDER:
                targetPath = targetPath.resolve(dsReq.getName());
                options.add(DatasetOption.CREATE_NODE);
            case DATASET:
                if(requestVersion != null){
                    options.add(DatasetOption.CREATE_VERSION);
                }
                useDefaultVersion = true;
            case DATASETVERSION:
                if(!requestLocations.isEmpty()){
                    // Implied new version, if no version exists
                    if(requestVersion == null && useDefaultVersion){
                        requestVersion = new DatasetVersion.Builder().versionId(DatasetView.NEW_VER).build();
                        options.add(DatasetOption.CREATE_VERSION);
                    }
                    options.add(DatasetOption.CREATE_LOCATIONS);
                }
            break;
        }
        dsReq = new Dataset(dsReq);
        Response ret = createDataset(targetPath, dsReq, requestVersion, requestLocations, rv, options);
        return ret;
    }
    
    private DatacatObject.Type getTargetType(DcFile parentFile, RequestView rv){
        if(parentFile.isDirectory()){
            return parentFile.getObject().getType();
        } else {
            if(rv.getDatasetView().getVersionId() != DatasetView.EMPTY_VER){
                return DatacatObject.Type.DATASETVERSION;
            } else {
                return DatacatObject.Type.DATASET;
            }
        }
    }
    
    public Response createDataset(DcPath datasetPath, Dataset reqDs, DatasetVersion version, 
            List<DatasetLocation> datasetLocations, RequestView rv, Set<DatasetOption> options){
        Dataset.Builder requestBuilder = new Dataset.Builder(reqDs);
        if(version != null){
            requestBuilder.version(version);
        }
        if(!datasetLocations.isEmpty()){
            if(datasetLocations.size() == 1){
                requestBuilder.location(datasetLocations.get(0));
            } else {
                requestBuilder.locations(datasetLocations);
            }
        }
        reqDs = requestBuilder.build();
        try {
            Dataset ret = getProvider().createDataset(datasetPath, reqDs, options);
            return Response.created(DcUriUtils.toFsUri(datasetPath.toString(), null, "SRS")).entity(ret).build();
        
        } catch (FileAlreadyExistsException ex) {
            DcFsException exc = DcFsException.valueOf(ex.getReason());
            int vid = version.getVersionId();
            if(vid == DatasetView.NEW_VER || vid == DatasetView.EMPTY_VER){
                vid = DatasetView.CURRENT_VER;
            }
            DatasetView existingView = new DatasetView(vid, DatasetView.ALL_SITES);
            Dataset existing;
            try {
                 existing = getProvider().getFile(datasetPath)
                         .getAttributeView(DatasetViewProvider.class).withView(existingView, true);
            } catch (IOException ex2){
                throw new RestException(ex2, 500, "Unable to check current dataset", ex2.getMessage());
            }

            DatasetVersion existingVersion = decomposeDataset(existing, null, false);
            switch(exc){
                case DATASET_EXISTS:
                    if(options.removeAll(DatasetOption.VIEW_WORK)){
                        URI newUri = ui.getAbsolutePathBuilder().path(existing.getName()).build();
                        return Response.seeOther(newUri).entity(existing).build();
                    }
                    return Response.status(Response.Status.FOUND).entity(existing).build();
                case VERSION_EXISTS:
                    if(options.contains(DatasetOption.CREATE_LOCATIONS)){
                        URI newUri = ui.getAbsolutePathBuilder().matrixParam("v","{v}")
                                .build(existingVersion.getVersionId());
                        return Response.seeOther(newUri).entity(existing).build();
                    }
                    return Response.status(Response.Status.FOUND).entity(existing).build();
                case LOCATION_EXISTS:
                    return Response.status(Response.Status.FOUND).entity(existing).build();
            }
            throw new RestException(ex, 500, "Shouldn't reach here", ex.getMessage());
        } catch (FileNotFoundException ex){
             throw new RestException(ex, 404, "Parent file doesn't exist", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (NotDirectoryException ex){
            throw new RestException(ex, 404, "File exists, but Path is not a container");
        } catch (IOException ex){
            ex.printStackTrace();
            throw new RestException(ex, 500);
        }
    }
       
    private DatasetVersion decomposeDataset(Dataset ds, List<DatasetLocation> returnLocations, boolean validate){
        DatasetVersion retVersion = null;
        if(ds instanceof FullDataset){
            FullDataset fd = (FullDataset) ds;
            
            if(validate){
                try {
                    fd.getVersion().validateFields();
                    retVersion = fd.getVersion();
                } catch(NullPointerException ex) {
                    // This is okay
                }
            } else {
                retVersion = new DatasetVersion(fd.getVersion());
            }
            
            DatasetLocation dsl = null;    
            Iterator<DatasetLocation> iter = fd.getLocations().iterator();
            while(iter.hasNext()){   // Verify all locations
                dsl = iter.next();
                if(validate){
                    try {
                        dsl.validateFields();
                    } catch(NullPointerException ex) {
                        throw new RestException(new IllegalArgumentException(ex.getMessage()), 400, 
                                "Unable to validate location: " + dsl.toString());
                    }
                }
                if(returnLocations != null){
                    returnLocations.add(dsl);
                }
            }
            return retVersion;
        }
        
        if(ds instanceof FlatDataset){
            FlatDataset fd = (FlatDataset) ds;
            try {
                fd.getVersion().validateFields();
                retVersion = fd.getVersion();
            } catch (NullPointerException ex){
                // This is okay
            }
            try {
                fd.getLocation().validateFields();
                returnLocations.add(fd.getLocation());
            } catch (NullPointerException ex){
                // this is okay too
            }
            return retVersion;
        }
        return null;
    }

    
    /**
     * Checks the request for validity and returns the options for createDataset.
     *
        1. If we POSTed to a container, we are creating a dataset, 
            possibly a version, and possibly locations
           a. If a dataset exists, but the representations in the data store and request 
               conflict, send a 409 - Conflict
           b. If a dataset exists, but a version doesn't exist, do a 303 - See Other 
               with the dataset URL
           c. If a dataset and version exists, but not all locations exist, do a 303 - See Other
               with the dataset+version URL
           d. If a dataset exists, and the representations in the data store and request 
               do not conflict, and there is no additional info, send a 302 - Found
        2. If we POSTed to a dataset, we are creating a version, and possibly locations
           a. If a version exists, but the representations in the data store and request 
               conflict, send a 409 - Conflict
           b. If a version exists, but location(s) are present and location(s) don't exist, 
               do a 303 - See Other with the dataset+version URL
           c. If a version exists, and the representations in the data store and request 
               are equivalent, and there is no additional info, do a 302 - Found
        3. If we POSTed to a version, we are creating locations.
           a. If a location exists, but the representations in the data store and request 
               conflict, send a 409 - Conflict
           b. If a version exists, but the location(s) don't exist, do a 303 - See Other 
               with the dataset+version URL
           c. If a location exists, and the representations in the data store and request 
               are equivalent, and there is no new information, send a 302 - Found
    */
}
