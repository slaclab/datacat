
package org.srs.datacatalog.search;

import java.io.StringReader;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.freehep.commons.lang.AST;
import org.freehep.commons.lang.bool.Lexer;
import org.freehep.commons.lang.bool.Parser;
import org.zerorm.core.Column;
import org.zerorm.core.Expr;
import org.zerorm.core.Op;
import org.zerorm.core.Select;
import org.zerorm.core.Sql;
import org.zerorm.core.Val;
import org.zerorm.core.primaries.Case;
import org.zerorm.core.interfaces.MaybeHasAlias;
import org.srs.datacatalog.search.MetanameContext.Entry;
import org.srs.datacatalog.search.plugins.DatacatPlugin;
import org.srs.datacatalog.search.tables.DatasetContainerUnion;
import org.srs.datacatalog.search.tables.DatasetVersions;
import org.srs.datacatalog.search.tables.DatasetVersions.LatestDatasetVersions;
import org.srs.datacatalog.search.tables.DatasetVersions.SpecificDatasetVersions;
import org.srs.datacatalog.search.tables.Folder;
import org.srs.rest.datacat.shared.DatacatObject;
import org.srs.rest.datacat.shared.Dataset;
import org.srs.rest.datacat.shared.DatasetGroup;
import org.srs.rest.datacat.shared.DatasetLocation;
import org.srs.rest.datacat.shared.DatasetVersion;
//import org.srs.rest.datacat.shared.ExtendedDataset;
import org.srs.rest.datacat.shared.LogicalFolder;
import org.srs.rest.datacat.shared.sql.DatacatDAO;

/**
 *
 * @author bvan
 */
public class DatacatSearch {
    
    private DatasetVersions dsv;
    private Select selectStatement;
    HashMap<String, DatacatPlugin> pluginMap;
    MetanameContext dmc;
    public DatacatSearch(Connection conn, HashMap<String, DatacatPlugin> pluginMap) throws Exception {
        this.pluginMap = pluginMap;
        dmc = buildMetanameGlobalContext( conn );
    }
    
    public DatacatSearch(HashMap<String, DatacatPlugin> pluginMap, 
            MetanameContext dmc) throws Exception {
        this.pluginMap = pluginMap;
        this.dmc = dmc;
    }
    
    public DatacatSearchContext prepareSelection(String queryString) throws Exception {
        dsv = prepareDatasetVersion(null, null);
        return prepareSelection(parseQueryString(queryString), dsv);
    }
    
    private DatacatSearchContext prepareSelection(AST ast, DatasetVersions stmt) throws Exception {
        for(DatacatPlugin p: pluginMap.values()){
            p.reset();
        }
        DatacatSearchContext sd = new DatacatSearchContext(stmt, pluginMap, dmc);
        if(ast != null){
            sd.assertIdentsValid( ast );
            Expr e = sd.evaluateNode(ast.getRoot(),stmt);
            // In case we want to do something else, go ahead here
            stmt.where( e );
            return sd;
        }
        return null;
    }

    public ArrayList<Dataset> searchForDatasets(Connection conn, String queryString) throws Exception {
        prepareSelection(queryString);
        return getResults( conn );
    }
    
    public ArrayList<Dataset> searchForDatasets(Connection conn, String datasetsFilter, String parentFilter) throws Exception {
        prepareSelection(datasetsFilter);
        return getResults( conn );
    }
    
    public List<Dataset> searchForDatasetsInParent(Connection conn, boolean keepAlive, Path basePath, 
            boolean recurseFolders, boolean searchFolders, boolean searchGroups, 
            String queryString, String[] sites, String[] metaFieldsToRetrieve,  String[] sortFields) throws Exception{
        DatacatDAO dao = new DatacatDAO(conn);
        DatacatObject parent = dao.findAnyObject( basePath );
        return searchForDatasetsInParent( conn, keepAlive, parent, recurseFolders, searchFolders, searchGroups, 
                queryString, sites, metaFieldsToRetrieve, sortFields );
    }
    
    public List<Dataset> searchForDatasetsInParent(Connection conn, boolean keepAlive, DatacatObject parent, 
            boolean recurseFolders, boolean searchFolders, boolean searchGroups, 
            String queryString, String[] sites, String[] metaFieldsToRetrieve,  String[] sortFields) throws Exception{
        return searchForDatasetsInParent( conn, keepAlive, parent, recurseFolders, 
                searchFolders, searchGroups, queryString, sites, metaFieldsToRetrieve, sortFields, 0, 0 );
    }
    
    public List<Dataset> searchForDatasetsInParent(Connection conn, boolean keepAlive, DatacatObject parent, 
            boolean recurseFolders, boolean searchFolders, boolean searchGroups, 
            String queryString, String[] sites, String[] metaFieldsToRetrieve,  String[] sortFields, int offset, int max) throws Exception{
        AST ast = parseQueryString(queryString);
        dsv = prepareDatasetVersion(sites, null);
        DatacatSearchContext sd = prepareSelection(ast, dsv);
        if(parent instanceof DatasetGroup){
            searchFolders = false;
        }
        
        if(!searchGroups && !(parent instanceof DatasetGroup)){
            throw new Exception("Option to not search groups conflicts with the parent");
        }
        if(recurseFolders && !(parent instanceof LogicalFolder)){
            throw new Exception("Recursive search only allowed on folder");
        }
        if(!searchGroups && !searchFolders){
            throw new Exception("Nothing to search");
        }
        
        if(ast != null){
            sd.evaluateNode(  ast.getRoot(), dsv);
        }
        
        DatasetContainerUnion union = new DatasetContainerUnion();
        if(parent instanceof LogicalFolder){
            if(recurseFolders){
                union.parentIn( Folder.recursiveFoldersFrom( parent.getPk() ) );
            } else {
                union.folderIs( parent.getPk() );
            }

            ArrayList<String> typeList = new ArrayList<>();
            if(searchGroups){
                typeList.add( "GROUP" );
            }
            if(searchFolders){
                typeList.add( "FOLDER" );
            }
            union.where( union.type.in( typeList ) );
            union.selection( new Column("pk", union));
            dsv.where( Op.or(dsv.ds.datasetlogicalfolder.in( union),
                    dsv.ds.datasetGroup.in( union )) );
        } else if(parent instanceof DatasetGroup){
            dsv.where( dsv.ds.datasetGroup.eq( parent.getPk()) );
        }
        
        HashMap<String, MaybeHasAlias> availableSelections = new HashMap<>();
        for(MaybeHasAlias a: dsv.getAvailableSelections()){
            availableSelections.put( a.canonical(), a);
        }
        if(sortFields != null){
            for(String s: sortFields){
                if(availableSelections.containsKey( s )){
                    dsv.orderBy( getColumnFromSelectionScope( s ) );
                }
            }
        }
        
        Expr paging = null;
        if(max > 0){
            paging = Op.lteq( new Sql("rownum"), offset + max );
        }
        if(paging != null){
            dsv.where( paging );
        }
        if(offset > 0){
            Sql s = new Sql("rownum").as( "rnum");
            dsv.selection( s );
            selectStatement = new Select(dsv.getColumns()).from( dsv.as( "dsv") )
                    .where( Op.gt( new Sql(s.alias()), offset) );
        }
        
        List<Dataset> rs = getResultsDeferred( conn, keepAlive );
        return rs;
    }

    public List<Dataset> searchForDatasetsInParent(Connection conn, DatacatObject parent, String queryString, List<String> orderBys) 
            throws Exception {
        prepareSelection(queryString);
        if(parent instanceof LogicalFolder){
            dsv.where( dsv.ds.datasetlogicalfolder.eq( parent.getPk() ) );
        } else if(parent instanceof DatasetGroup){
            dsv.where( dsv.ds.datasetGroup.eq( parent.getPk() ) );
        }
        
        HashMap<String, MaybeHasAlias> availableSelections = new HashMap<>();
        for(MaybeHasAlias a: dsv.getAvailableSelections()){
            availableSelections.put( a.canonical(), a);
        }
        for(String s: orderBys){
            if(availableSelections.containsKey( s )){
                dsv.orderBy( getColumnFromSelectionScope( s ) );
            }
        }
        return getResults( conn );
    }
    
    private AST parseQueryString(String queryString) throws Exception{
        if(queryString == null || queryString.isEmpty()){
            return null;
        }
        Lexer scanner = new Lexer( new StringReader(queryString) );
        Parser p = new Parser( scanner );
        try {
            return (AST) p.parse().value;
        } catch(Exception ex) {
            Logger.getLogger( DatacatSearch.class.getName() )
                    .log( Level.WARNING, "Error parsing", ex);
            throw ex;
        }
    }
    
    public ArrayList<Dataset> getResults(Connection conn) throws SQLException{
        ArrayList<Dataset> datasets = new ArrayList<>();
        
        if(selectStatement == null){
            selectStatement = dsv;
        }
        
        try (PreparedStatement stmt = selectStatement.prepareAndBind( conn )) {
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                datasets.add( datasetFactory(rs));
            }
        }
        selectStatement = null;
        return datasets;
    }
    
    // Support for a resultset backed list
    public List<Dataset> getResultsDeferred(final Connection conn, final boolean keepAlive) throws SQLException{
     
        if(selectStatement == null){
            selectStatement = dsv;
        }
        final Select sel = selectStatement;
        
        final Iterator<Dataset> iter = new Iterator<Dataset>() {
            PreparedStatement stmt = sel.prepareAndBind( conn );
            ResultSet rs = stmt.executeQuery();
            boolean advance = true;
            boolean hasNext = false;

            @Override
            public boolean hasNext(){
                try {
                    if(advance){
                        hasNext = rs.next();
                        advance = false;
                    }
                    if(!hasNext){
                        if(stmt != null){ stmt.close(); }
                        if(conn != null && !keepAlive){ conn.close(); }
                    }
                } catch(SQLException ex) { throw new RuntimeException("Error handling list",ex);}
                return hasNext;
            }

            @Override
            public Dataset next(){
                advance = hasNext() == true ? true : false;
                try {
                    return datasetFactory( rs );
                } catch(SQLException ex) { throw new RuntimeException("Error handling list", ex);}
            }

            @Override
            public void remove(){ throw new UnsupportedOperationException( "Not implemented" ); }

            // Last chance effort to try to close the statement
            @Override
            protected void finalize() throws Throwable{
                try {
                    if(stmt != null){ stmt.close(); }
                    if(conn != null && !keepAlive){ conn.close(); }
                } catch(SQLException ex) { }
                super.finalize();
            }
        };
        selectStatement = null;
        
        return new ArrayList<Dataset>(){
            boolean initialized = false;
            @Override
            public Iterator iterator(){ return !initialized ? iter: super.iterator(); }
            
            @Override
            public Dataset get(int index){
                verifyInitialized();
                return super.get( index );
            }

            @Override
            public int size(){
                verifyInitialized();
                return super.size();
            }

            @Override
            public Object[] toArray(){
                verifyInitialized();
                return super.toArray();
            }
            
            public void verifyInitialized(){
                if(!initialized){
                    while(iter.hasNext()){
                        add(iter.next());
                    }
                }
                initialized = true;
            }
        };
    }

    private DatasetVersions prepareDatasetVersion(String[] sites, Long version){
        DatasetVersions sel = null;
        if(version == null){
            sel = new LatestDatasetVersions();
        } else {
            sel = new SpecificDatasetVersions(version);
        }
        
        Case maybeFolder = new Case( sel.ds.datasetlogicalfolder.not_null(), 
                sel.ds.datasetlogicalfolder, sel.ds.datasetGroup);
        sel.selection( new Val<>("DATASET").as( "type" ) );
        sel.selection( maybeFolder.as( "parent" ) );
        sel.selection( sel.ds.dataset.as( "pk" ) );
        return sel;
    }
    
    public DatasetVersions getSelectStatement(){
        return this.dsv;
    }
     
    private MetanameContext buildMetanameGlobalContext(Connection conn) throws SQLException {
        
        String sql = "select metaname, prefix " +
                        "    from datasetmetaname vx " +
                        "    left outer join ( " +
                        "            select substr(v1.metaname,0,4) prefix,  " +
                        "                    count(v1.metaname) prefixcount " +
                        "            from  " +
                        "            datasetmetaname v1 " +
                        "            group by substr(v1.metaname,0,4) " +
                        "            having count(v1.metaname) > 5 " +
                        "    ) v0 on substr(vx.metaname,0,4) = prefix " +
                        "    order by prefix asc ";
        try (PreparedStatement stmt = conn.prepareStatement( sql )) {
            ResultSet rs = stmt.executeQuery();

            dmc = new MetanameContext();
            ArrayList<String> postfixes = new ArrayList<>();
            String lastPrefix = null;
            while(rs.next()){
                String maybePrefix = rs.getString( "prefix");
                String metaname = rs.getString( "metaname");
                if(maybePrefix != null){
                    if (lastPrefix != null && !lastPrefix.equals( maybePrefix) ){
                        dmc.add( new Entry(lastPrefix, postfixes, lastPrefix.length() ));
                        postfixes.clear();
                    }
                    lastPrefix = maybePrefix;
                    postfixes.add( metaname );
                } else {
                        dmc.add( new Entry( metaname ));
                }
            }
            return dmc;
        }
    }
    
    
    private MetanameContext buildGroupMetanameContext(Connection conn)throws SQLException {
        String sql = "select metaname from datasetgroupmetaname";
        return buildContainerMetanameContext(conn, sql);
    }
    
    private MetanameContext buildFolderMetanameContext(Connection conn)throws SQLException {
        String sql = "select metaname from logicalfoldermetaname";
        return buildContainerMetanameContext(conn, sql);
    }
    
    private MetanameContext buildContainerMetanameContext(Connection conn, String sql) 
            throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement( sql )) {
            ResultSet rs = stmt.executeQuery();
            MetanameContext mnc = new MetanameContext();
            while(rs.next()){
                mnc.add( new Entry(rs.getString( "metaname") ));
            }
            return mnc;
        }
    }
    
    public static Dataset datasetFactory(ResultSet rs) throws SQLException{
        ExtendedDataset d = new ExtendedDataset(DatacatObject.asType( Dataset.class) );
        DatasetVersion dsv = new DatasetVersion();
        DatasetLocation vdl = new DatasetLocation();
        
        d.setPk(rs.getLong("pk"));
        d.setParentPk(rs.getLong("parent"));
        d.setName(rs.getString("name"));
        d.setFileFormat(rs.getString("datasetfileformat"));
        d.setDataType(rs.getString("datasetdatatype"));
        d.setLatestVersionPk(rs.getLong("latestversion"));
        dsv.setVersionId(rs.getInt("versionid"));
        //dsv.setDatasetSource(rs.getString("datasetsource"));
        dsv.setLocationPk(rs.getLong("masterlocation"));
        vdl.setSite(rs.getString("datasetsite"));
        vdl.setFileSystemPath(rs.getString("path"));
        vdl.setRunMin(rs.getLong("runmin"));
        vdl.setRunMax(rs.getLong("runmax"));
        vdl.setEventCount(rs.getLong("numberevents"));
        vdl.setFileSize(rs.getLong("filesizebytes"));
        vdl.setCheckSum(rs.getLong("checksum"));
        dsv.setLocation(vdl);
        d.setVersion(dsv);

        return d;
    }

    private Column getColumnFromSelectionScope(String ident){
        for(MaybeHasAlias selection: dsv.getAvailableSelections()){
            if(selection.canonical().equals( ident ) && selection instanceof Column){
                dsv.selection( selection );
                return (Column) selection;
            }
        }
        return null;
    }
}
