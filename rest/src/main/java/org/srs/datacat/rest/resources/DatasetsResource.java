/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacat.rest.resources;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.srs.datacat.rest.App;
import org.srs.datacat.rest.ConnectionResource;
import org.srs.datacat.rest.resources.PathResource.StatTypeWrapper;
import org.srs.datacat.rest.search.SearchHelper;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.dataset.DatasetBuilder;
import org.srs.datacat.shared.dataset.FlatDataset;
//import org.srs.datacat.sql.DatacatDAO;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcUriUtils;
import org.srs.rest.shared.HumanPath;
import org.srs.rest.shared.RestException;

/**
 * The datasets resource will return all datasets under a given path.
 * You can recurse a path to find all datasets that will fall underneath that
 * path.
 * @author bvan
 */
@Path("/datasets")
public class DatasetsResource extends ConnectionResource  { 
    private final String idRegex = "{id: [\\w\\d\\-_\\./]+}";
    private final String idPath = "[/path]*/{id}";
    
    @Context private HttpServletResponse response;
    
    /**
     * Interface to SeachHelper/FindDatasets.
     * 
     * included: private BooleanOption recurseFoldersOpt = new BooleanOption("--recurse", "-R", "Recurse sub-folders");
     * n/a: private BooleanOption searchFoldersOpt = new BooleanOption("--search-folders", "-f", "Search for datasets inside folder(s)");
     * n/a: private BooleanOption searchGroupsOpt = new BooleanOption("--search-groups", "-g", "Search in groups.  This option is superseded by the -G (--group) option if they are both supplied.");
     * implicit: private StringOption datasetGroupOpt = new StringOption("--group", "-G", "group name", null, "Dataset Group under which to search for datasets.");
     * included: private MultiStringOption siteOpt = new MultiStringOption("--site", "-S", "site name", "Name of Site to search.  May be used multiple times to specify a list of sites in which case order is taken as preference.  Defaults to the Master-location if not provided.");
     * included: private StringOption filterOpt = new StringOption("--filter", "-F", "filter expression", null, "Criteria by which to filter datasets.  ie: \'DatasetDataType==\"MERIT\" && nMetStart>=257731220 && nMetStop &lt;=257731580\'");
     * omitted:  private MultiStringOption displayOpt = new MultiStringOption("--display", "-d", "meta name", "Name of meta-data field to display in output.  Default is to display only the file location.  May be used multiple times to specify an ordered list of fields to display.");
     * included: private MultiStringOption sortOpt = new MultiStringOption("--sort", "-s", "meta name", "Name of meta-data field to sort on.  May be used multiple times to specify a list of fields to sort on.  Order determines precedence.");
     * included: private BooleanOption showUnscannedLocationsOpt = new BooleanOption("--show-unscanned-locations", "If no \"OK\" (ie: verified by file crawler) location exists, display first location (if any) which has not yet been scanned.  If this option and '--show-non-ok-locations' are both specified, an unscanned location will be returned before a non-ok location regardless of their sequence in the ordered site list.");
     * included: private BooleanOption showNonOkLocationsOpt = new BooleanOption("--show-non-ok-locations", "If no \"OK\" (ie: verified by file crawler) location exists, display first location (if any) which exists in the list of sites.");
     * implicit: private StringParameter logicalFolderPar = new StringParameter("logical folder", "Logical Folder Path at which to begin performing the search.");
     * 
     
    @GET
    @Path(idRegex)
    @HumanPath(idPath)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public ArrayList<DatacatObject> find(@PathParam("id") String path,
            @QueryParam("recurse") boolean recurse, 
            @QueryParam("sites") List<String> sites, 
            @QueryParam("filter") String filter, 
            @QueryParam("sort") List<String> sortParams, 
            @DefaultValue("false") @QueryParam("unscanned") boolean unscanned, 
            @DefaultValue("false") @QueryParam("nonOk") boolean nonOk,
            @DefaultValue("true") @QueryParam("checkFolders") boolean checkFolders,
            @DefaultValue("true") @QueryParam("checkGroups") boolean checkGroups,
            @DefaultValue("false") @QueryParam("allMetadata") boolean metadata,
            @DefaultValue("-1") @QueryParam("max") int max,
            @DefaultValue("0") @QueryParam("offset") int offset) throws IOException {

        ArrayList<DatacatObject> d = new ArrayList<>();
        DatacatObject o = null;
        
        if (path != null && !path.isEmpty()) {
            PathResource pr = new PathResource();
            o = pr.getBean(path, StatTypeWrapper.valueOf( "none"));
        }

        // Hack for quality == "GOOD" to include GOLDEN
        if (filter != null) {
        String regex = "((quality) *(==) *\"(GOOD)\")";
        Pattern pattern = Pattern.compile(regex);
            Matcher m = pattern.matcher(filter);
            if (m.find()) {
                filter = filter.replaceAll(regex, "(" + m.group(1) + " || quality == \"GOLDEN\")");
                System.out.println(filter);
            }
        }
        SearchHelper sh = new SearchHelper();
        
        try (Connection conn =  getConnection() ){
            DatacatDAO dc = new DatacatDAO( conn );
            List<Dataset> datasets = sh.search(conn, o, sortParams, recurse, checkFolders, checkGroups, unscanned, nonOk, sites, filter);
            d.addAll(datasets);
            /*
            if(metadata){
                dc.setDatasetMetadataFromDB(datasets);
            }
        } catch (RuntimeException ce){
            Logger.getLogger(PathResource.class.getName()).log(Level.SEVERE, "Unable to complete. The filter was likely malformed");
            throw new RestException(new RuntimeException("Unable to complete. The filter was likely malformed"),400);
        } catch (Exception ex) {
            Logger.getLogger(PathResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new RestException(ex,500);
        }

        return d;
    }*/
    
    
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
    
    
    @PUT
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    public Dataset reflectDatasetPut(@PathParam("id") String path, 
            MultivaluedMap<String, String> formParams,
            @MatrixParam("v") List<String> versions,
            @MatrixParam("l") List<String> locations){
        
        boolean createVersion = versions.size() != 1;
        if(versions.size() == 0){
            
        }
        
        HashMap<String, Method> jsonSetterMethods = new HashMap<>();
        ObjectMapper mapper =  new ObjectMapper();
        DatasetBuilder builder = DatasetBuilder.create( FlatDataset.Builder.class );
        
        for(Method m: builder.getClass().getMethods()){
            if( m.getAnnotation( JsonSetter.class ) != null ){
                jsonSetterMethods.put( m.getName(), m);
            }
        }
        
        try {
            for(String key: formParams.keySet()){
                Method m = jsonSetterMethods.get( key );
                List<String> lValue = formParams.get( key );
                if(lValue.size() != 1){
                    throw new RuntimeException( "Only one value per parameter is supported" );
                }
                Class<?> targetValueType = m.getParameterTypes()[0]; // Should only be one
                Object value = mapper.convertValue( lValue.get( 0 ), targetValueType );
                m.invoke( builder, value );
            }
        } catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger( ValidateResource.class.getName() ).log( Level.SEVERE, null, ex );
        }
        
        FlatDataset dsReq = (FlatDataset) builder.build();
        
        path = "/" + path;
        
        DcPath dcp = App.fsProvider.getPath(DcUriUtils.toFsUri(path, null, "SRS"));        
        
        
        
        return builder.build();
    }


}
