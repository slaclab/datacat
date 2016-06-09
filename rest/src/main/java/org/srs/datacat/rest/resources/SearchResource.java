package org.srs.datacat.rest.resources;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
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
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetResultSetModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.RequestView;
import org.srs.datacat.rest.BaseResource;
import static org.srs.datacat.rest.BaseResource.OPTIONAL_EXTENSIONS;
import org.srs.datacat.rest.SearchPluginProvider;
import org.srs.datacat.rest.RestException;
import org.srs.datacat.model.RecordType;

/**
 *
 * @author bvan
 */
@Path("/search" +  OPTIONAL_EXTENSIONS)
public class SearchResource extends BaseResource {
    private final String searchRegex = "{id: [^\\?]+}";
    @Inject SearchPluginProvider pluginProvider;
    
    private UriInfo ui;
    private List<PathSegment> pathSegments;
    private String requestPath;
    private HashMap<String, List<String>> requestMatrixParams = new HashMap<>();
    private HashMap<String, List<String>> requestQueryParams = new HashMap<>();
    
    public SearchResource(@PathParam("id") List<PathSegment> pathSegments, @Context UriInfo ui){
        this.pathSegments = pathSegments;
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
    @Path(searchRegex)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response find(
            @QueryParam("recurse") boolean recurse,
            @QueryParam("sites") List<String> sites,
            @QueryParam("filter") String filter,
            @QueryParam("containerFilter") String containerFilter,
            @QueryParam("sort") List<String> sortParams, 
            @QueryParam("show") List<String> metadata, 
            @QueryParam("checkFolders") Boolean checkFolders,
            @QueryParam("checkGroups") Boolean checkGroups,
            @DefaultValue("100000") @QueryParam("max") int max,
            @DefaultValue("0") @QueryParam("offset") int offset) {

        String pathPattern = requestPath;        
        String[] metafields= metadata.toArray( new String[0]);
        String[] sortFields = sortParams.toArray(new String[0]);
        
        DatasetView dv = null;
        try {
            RequestView rv = new RequestView(RecordType.DATASET, requestMatrixParams);
            if(rv.getPrimaryView() == RequestView.CHILDREN || rv.getPrimaryView() == RequestView.METADATA){
                throw new IllegalArgumentException("Children and Metadata views not available when searching");
            }
            dv = rv.getDatasetView(DatasetView.MASTER);
        } catch (IllegalArgumentException ex){
            throw new RestException(ex, 400, "Unable to process view", ex.getMessage());
        }
        
        DatasetResultSetModel searchResults = null;
        try (DirectoryStream<DatasetModel> stream = 
                    getProvider().search(pathPattern, buildCallContext(), checkFolders, checkGroups, 
                            dv, filter, containerFilter, metafields, sortFields)){
            List<DatasetModel> datasets = new ArrayList<>();
            int count = 0;
            Iterator<DatasetModel> iter = stream.iterator();
            for(int i = 0; iter.hasNext(); i++, count++){
                if(i >= offset && i < (offset + max)){
                    datasets.add(iter.next());
                } else {
                    iter.next();
                }
            }
            searchResults = getProvider().getModelProvider().getDatasetResultSetBuilder()
                    .results(datasets).count(count).build();
        } catch (IllegalArgumentException ex){
            throw new RestException(ex,400, "Unable to process query, see message", ex.getMessage());
        } catch (NoSuchFileException ex){
             throw new RestException(ex,404, "File doesn't exist", ex.getMessage());
        } catch(IOException ex) {
            Logger.getLogger(PermissionsResource.class.getName()).log(Level.WARNING, "Unknown exception", ex);
            throw new RestException(ex, 500);
        } catch(ParseException ex) {
            throw new RestException(ex, 422, "Unable to parse filter", ex.getMessage());
        }
        return Response
                .ok( new GenericEntity<DatasetResultSetModel>(searchResults) {})
                .build();
    }
    
    /*@POST
    @Path(searchRegex)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response find(@PathParam("id") List<PathSegment> pathSegments,
            @QueryParam("recurse") boolean recurse,
            @QueryParam("sites") List<String> sites,
            @QueryParam("filter") String filter,
            @QueryParam("sort") List<String> sortParams, 
            @QueryParam("show") List<String> metadata, 
            @QueryParam("checkFolders") Boolean checkFolders,
            @QueryParam("checkGroups") Boolean checkGroups,
            @DefaultValue("-1") @QueryParam("max") int max,
            @DefaultValue("0") @QueryParam("offset") int offset) {

        String pathPattern = "";
        
        HashMap<String, List<String>> matrixParams = new HashMap<>();
        for(PathSegment s: pathSegments){
            pathPattern = pathPattern + "/" + s.getPath();
            matrixParams.putAll(s.getMatrixParameters());
        }
        List<? super Dataset> datasets = new ArrayList<>();
        String[] metafields= metadata.toArray( new String[0]);
        String[] sortFields = sortParams.toArray(new String[0]);
        
        DatasetView dv = null;
        try {
            RequestView rv = new RequestView(DatacatObject.Type.DATASET, matrixParams);
            if(rv.getPrimaryView() == RequestView.CHILDREN || rv.getPrimaryView() == RequestView.METADATA){
                throw new IllegalArgumentException("Children and Metadata views not available when searching");
            }
            dv = rv.getDatasetView();
        } catch (IllegalArgumentException ex){
            throw new RestException(ex, 400, "Unable to process view", ex.getMessage());
        }
        
        try(Connection conn = getConnection()){
            DatasetSearch datacatSearch = new DatasetSearch(getProvider(), conn, pluginProvider.getPlugins());

            String queryString = filter;

            String searchBase = PathUtils.normalizeRegex(GlobToRegex.toRegex(pathPattern,"/"));
            java.nio.file.Path root = getProvider().getPath(DcUriUtils.toFsUri("/", null, "SRS"));
            java.nio.file.Path searchPath = root.resolve(searchBase);
            ContainerVisitor visitor = new ContainerVisitor(searchPath.getFileSystem(), pathPattern, checkGroups, checkFolders);
            DirectoryWalker walker = new DirectoryWalker(getProvider(), visitor, 100);
            walker.walk(searchPath);
            datacatSearch.compile(visitor.files, dv, queryString, metafields, sortFields,0,-1);
            datasets = datacatSearch.retrieveDatasets();
        } catch (IllegalArgumentException ex){
            throw new RestException(ex,400, "Unable to process query, see message", ex.getMessage());
        } catch (NoSuchFile ex){
             throw new RestException(ex,404, "File doesn't exist", ex.getMessage());
        } catch(SQLException | IOException ex) {
            throw new RestException(ex, 500);
        } catch(ParseException ex) {
            throw new RestException(ex, 422, "Unable to parse filter", ex.getMessage());
        }
        return Response
                .ok( new GenericEntity<List<DatacatObject>>((List<DatacatObject>) datasets) {})
                .build();
    }*/
    
}
