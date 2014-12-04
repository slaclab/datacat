
package org.srs.datacat.rest.resources;

import com.google.common.base.Optional;
import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
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
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetVersionModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.RequestView;
import org.srs.datacat.rest.BaseResource;
import static org.srs.datacat.rest.BaseResource.OPTIONAL_EXTENSIONS;
import org.srs.datacat.rest.FormParamConverter;
import org.srs.datacat.rest.PATCH;
import org.srs.datacat.rest.RestException;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.DatasetViewInfo;
import org.srs.datacat.shared.DatasetWithView;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcFileSystemProvider.DcFsExceptions;
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
@Path("/datasets" +  OPTIONAL_EXTENSIONS)
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
    public Response getDataset(@PathParam("id") String path, 
            @MatrixParam("v") List<String> versions,
            @MatrixParam("l") List<String> locations) throws IOException{
        System.out.println(ui.getAbsolutePath());
        DcPath targetPath = getProvider().getPath(DcUriUtils.toFsUri(requestPath, getUser(), "SRS"));
        DatacatObject.Type targetType = null;
        try {
            DcFile file = getProvider().getFile(targetPath);
            DatacatNode ret;
            RequestView rv = new RequestView(DatacatObject.Type.DATASET, requestMatrixParams);
            System.out.println(rv.getDatasetView().toString());
            if(file.isRegularFile()){
                ret = file.getAttributeView(DatasetViewProvider.class).withView(rv.getDatasetView(DatasetView.CURRENT_ALL), true);
                return Response.ok( new GenericEntity(ret, Dataset.class) ).build();
            }
            throw new NoSuchFileException(path, "Path is not a dataset","NO_SUCH_FILE");
        } catch (IllegalArgumentException ex){
            throw new RestException(ex, 400 , "Unable to correctly process view", ex.getMessage());
        } catch (NoSuchFileException ex){
            // TODO: Check exception
            throw new RestException(ex, 400 , "The target is not a dataset", ex.getMessage());
        } catch (AccessDeniedException ex){
             throw new RestException(ex, 403);
        } catch (IOException ex){
            throw new RestException(ex, 500);
        }
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
    public Response createDatasetFromForm(MultivaluedMap<String, String> formParams) throws IOException{
        Dataset.Builder builder = FormParamConverter.getDatasetBuilder(formParams);
        Dataset dsReq = builder.build();
        return createDataset(dsReq);
    }
    
    @POST
    @Path(idRegex)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response createDataset(Dataset dsReq) throws IOException{
        DcPath targetPath = getProvider().getPath(DcUriUtils.toFsUri(requestPath, getUser(), "SRS"));
        DcFile parentFile = null;
        DatacatObject.Type targetType = null;
        RequestView rv = null;
        try {
            parentFile = getProvider().getFile(targetPath);
            rv = new RequestView(parentFile.getObject().getType(), requestMatrixParams);
            targetType = getTargetType(parentFile, rv);
        } catch (NoSuchFileException ex) {
            throw new RestException(ex ,404, "File doesn't exist", ex.getMessage());
        } catch (IllegalArgumentException ex){
            throw new RestException(ex, 400, "Unable to validate request view", ex.getMessage());
        }
                        
        Optional<DatasetViewInfo> viewRequestOpt;
        if(dsReq instanceof DatasetWithView){
            viewRequestOpt = Optional.of(((DatasetWithView) dsReq).getViewInfo());
        } else {
            viewRequestOpt = Optional.absent();
        }
        
        Set<DatasetOption> options = new HashSet<>();
        switch(targetType){
            case GROUP:
            case FOLDER:
                targetPath = targetPath.resolve(dsReq.getName());
                options.add(DatasetOption.CREATE_NODE);
                if(viewRequestOpt.isPresent() 
                        && !viewRequestOpt.get().versionOpt().isPresent() 
                        && viewRequestOpt.get().locationsOpt().isPresent()){
                    // The user omitted versionId, but included locations
                    viewRequestOpt = Optional.of(new DatasetViewInfo(DatasetVersion.NEW_VERSION, 
                            viewRequestOpt.get().getLocations()));
                }
            case DATASET:
                if(viewRequestOpt.isPresent() && viewRequestOpt.get().versionOpt().isPresent()){
                    options.add(DatasetOption.CREATE_VERSION);
                }
            case DATASETVERSION:
                if(!viewRequestOpt.isPresent() && viewRequestOpt.get().locationsOpt().isPresent()){
                    options.add(DatasetOption.CREATE_LOCATIONS);
                }
            break;
        }
        dsReq = new Dataset(dsReq);
        return createDataset(targetPath, dsReq, viewRequestOpt, rv, options);
    }
            
    public Response createDataset(DcPath datasetPath, Dataset reqDs,
            Optional<DatasetViewInfo> viewRequestOpt, RequestView rv, Set<DatasetOption> options){
        Dataset.Builder requestBuilder = new Dataset.Builder(reqDs);
        if(viewRequestOpt.isPresent()){
            requestBuilder.view(viewRequestOpt.get());
        }
        reqDs = requestBuilder.build();
        try {
            DatasetModel ret = getProvider().createDataset(datasetPath, reqDs, options);
            return Response.created(DcUriUtils.toFsUri(datasetPath.toString(), getUser(), "SRS"))
                    .entity(ret).build();
        } catch (FileAlreadyExistsException ex) {
            return handleFileAlreadyExists(ex, datasetPath, viewRequestOpt, options);
        } catch (NoSuchFileException ex){
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
    
    /**
      Return an appropriate response in the case where a file alredy exists
     */
    private Response handleFileAlreadyExists(FileAlreadyExistsException ex, DcPath datasetPath, 
            Optional<DatasetViewInfo> viewOpt, Set<DatasetOption> options){

        DcFsExceptions exc = DcFsExceptions.valueOf( ex.getReason() );
        DatasetView existingView = new DatasetView(DatasetView.CURRENT_VER, DatasetView.ALL_SITES);
        if(viewOpt.isPresent() && viewOpt.get().versionOpt().isPresent()){
            int vid = viewOpt.get().getVersion().getVersionId();
            if(vid >= 0){
                 existingView = new DatasetView(vid, DatasetView.ALL_SITES );
            }
        }
        
        DatasetModel existing;
        try {
            existing = getProvider().getFile( datasetPath )
                    .getAttributeView(DatasetViewProvider.class).withView(existingView, true);
        } catch(IOException ex2) {
            throw new RestException(ex2, 500, "Unable to check current dataset", ex2.getMessage());
        }
        DatasetViewInfo currentViewInfo = existing instanceof DatasetWithView
                ? ((DatasetWithView) existing).getViewInfo() : null;

        UriBuilder newUriBuilder = ui.getAbsolutePathBuilder();
        switch(exc){
            case DATASET_EXISTS:
                newUriBuilder.path(existing.getName()).build();
                return Response.seeOther(newUriBuilder.build()).build();
            case VERSION_EXISTS:
                /* By definition, currentVer is non-null */
                DatasetVersionModel currentVer = currentViewInfo.getVersion();
                newUriBuilder.matrixParam("v", currentVer.getVersionId());
                return Response.seeOther(newUriBuilder.build()).build();
            case LOCATION_EXISTS:
                newUriBuilder.matrixParam("l", "all");
        }
        return Response.seeOther(newUriBuilder.build()).build();
    }
    
    @PATCH
    @Path(idRegex)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response patchDataset(Dataset dsReq) throws IOException{
        System.out.println(dsReq.toString());
        return Response.ok().build();
    }

}
