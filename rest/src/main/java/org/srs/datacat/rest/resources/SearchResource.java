
package org.srs.datacat.rest.resources;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.srs.datacat.rest.BaseResource;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcUriUtils;
import org.srs.datacat.vfs.DirectoryWalker;
import org.srs.datacat.vfs.DirectoryWalker.ContainerVisitor;
import org.srs.datacatalog.search.DatasetSearch;
import org.srs.datacatalog.search.plugins.DatacatPlugin;
import org.srs.vfs.GlobToRegex;
import org.srs.vfs.PathUtils;
import org.zerorm.core.Select;

/**
 *
 * @author Brian Van Klaveren<bvan@slac.stanford.edu>
 */
@Path("/search")
public class SearchResource extends BaseResource {
    private final String searchRegex = "{id: [^\\?]+}";
    
    @GET
    @Path(searchRegex)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public List<DatacatObject> find(@PathParam("id") String pathPattern,
            @QueryParam("recurse") boolean recurse,
            @QueryParam("sites") List<String> sites,
            @QueryParam("filter") String filter,
            @QueryParam("sort") List<String> sortParams, 
            /*@DefaultValue("false") @QueryParam("unscanned") boolean unscanned,*/
            /*@DefaultValue("false") @QueryParam("nonOk") boolean nonOk,*/
            @QueryParam("checkFolders") Boolean checkFolders,
            @QueryParam("checkGroups") Boolean checkGroups,
            /*@DefaultValue("false") @QueryParam("allMetadata") boolean metadata,*/
            @DefaultValue("-1") @QueryParam("max") int max,
            @DefaultValue("0") @QueryParam("offset") int offset) {

        pathPattern = "/" + pathPattern;
        List<? super Dataset> datasets = new ArrayList<>();
        String[] metafields= null;
        String[] sortFields = sortParams.toArray(new String[0]);
        
        try(Connection conn = getConnection()){
            DatasetSearch datacatSearch = new DatasetSearch(getProvider(), conn, new HashMap<String, DatacatPlugin>());

            String queryString = filter;

            String searchBase = PathUtils.normalizeRegex(GlobToRegex.toRegex(pathPattern,"/"));
            DcPath root = getProvider().getPath(DcUriUtils.toFsUri("/", null, "SRS"));
            DcPath searchPath = root.resolve(searchBase);
            ContainerVisitor visitor = new ContainerVisitor(searchPath.getFileSystem(), pathPattern, checkGroups, checkFolders);
            Select stmt = datacatSearch.compileStatement( conn, searchPath, visitor, 
                            false, 100, queryString, null, metafields, sortFields,0,-1);
            datasets = datacatSearch.searchForDatasetsInParent(conn, stmt);
            System.out.println(datasets.size());
        } catch(SQLException | IOException | ParseException ex) {
        //} catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        return (List<DatacatObject>) datasets;
    }
    
}
