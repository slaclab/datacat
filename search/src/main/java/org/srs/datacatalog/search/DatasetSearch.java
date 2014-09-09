
package org.srs.datacatalog.search;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.freehep.commons.lang.AST;
import org.freehep.commons.lang.bool.Lexer;
import org.freehep.commons.lang.bool.Parser;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.vfs.DcFileSystemProvider;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DirectoryWalker;
import org.srs.datacat.vfs.DirectoryWalker.ContainerVisitor;
import org.srs.datacatalog.search.plugins.DatacatPlugin;
import org.srs.datacatalog.search.tables.DatasetVersions;
import org.zerorm.core.Column;
import org.zerorm.core.Expr;
import org.zerorm.core.Op;
import org.zerorm.core.Select;
import org.zerorm.core.Sql;
import org.zerorm.core.Table;
import org.zerorm.core.Val;
import org.zerorm.core.interfaces.MaybeHasAlias;
import org.zerorm.core.primaries.Case;

/**
 *
 * @author bvan
 */
public class DatasetSearch {
    
    DcFileSystemProvider provider;
    HashMap<String, DatacatPlugin> pluginMap;
    MetanameContext dmc;
    
    public DatasetSearch(DcFileSystemProvider provider, Connection conn, HashMap<String, DatacatPlugin> pluginMap) throws Exception {
        this.provider = provider;
        this.pluginMap = pluginMap;
        this.dmc = SearchUtils.buildMetanameGlobalContext( conn );
    }
    
    public List<Dataset> searchForDatasetsInParent(Connection conn, Select statement, boolean keepAlive) throws Exception{
        return SearchUtils.getResultsDeferred( conn, statement, keepAlive );
    }
    
    public Select compileStatement(Connection conn, DcPath parent, 
            ContainerVisitor visitor, boolean checkParent, int maxDepth,
            String queryString, String[] sites, String[] metaFieldsToRetrieve,  String[] sortFields, int offset, int max) throws Exception{
        AST ast = parseQueryString(queryString);
        DatasetVersions dsv = prepareDatasetVersion(sites, null);
        DatacatSearchContext sd = prepareSelection(ast, dsv);
        
        if(ast != null){
            sd.evaluateNode(  ast.getRoot(), dsv);
        }
        
        DirectoryWalker walker = new DirectoryWalker(provider, visitor, maxDepth);
        walker.walk(parent);
        SearchUtils.populateParentTempTable(conn, visitor);     
        System.out.println("checking...");
        try (PreparedStatement stmt = conn.prepareStatement( "SELECT * FROM ContainerSearch")){
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                System.out.println(rs.getString("ContainerPath"));
            }
        }

        HashMap<String, MaybeHasAlias> availableSelections = new HashMap<>();
        for(MaybeHasAlias a: dsv.getAvailableSelections()){
            availableSelections.put( a.canonical(), a);
        }
        if(sortFields != null){
            for(String s: sortFields){
                if(availableSelections.containsKey( s )){
                    dsv.orderBy( getColumnFromSelectionScope( dsv, s ) );
                }
            }
        }
        
        List<MaybeHasAlias> selList = new ArrayList<>();
        for(MaybeHasAlias a: dsv.getAvailableSelections()){
            selList.add(new Column(a.canonical(), dsv));
        }
        
        Expr paging = null;
        if(max > 0){
            paging = Op.lteq( new Sql("rownum"), offset + max );
        }
        if(paging != null){
            dsv.where( paging );
        }
        
        Table containerSearch = new Table("ContainerSearch", "cp");
        
        System.out.println(dsv.getSelections());
        System.out.println(dsv.getColumns());
        
        Select selectStatement = containerSearch
                .select( containerSearch.$("ContainerPath"))
                .join( dsv, 
                    Op.or( 
                        dsv.getSelection(dsv.ds.datasetlogicalfolder).eq(containerSearch.$("DatasetLogicalFolder")), 
                        dsv.getSelection(dsv.ds.datasetGroup).eq(containerSearch.$("DatasetGroup"))
                    )
                ).selection(dsv.getColumns());
        
        if(offset > 0){
            Sql s = new Sql("rownum").as( "rnum");
            dsv.selection( s );
            selectStatement = new Select(selectStatement.getSelections()).from( selectStatement.as("ss") )
                    .where( Op.gt( new Sql(s.alias()), offset) );
        }
        return selectStatement;
    }
    
    private DatasetVersions prepareDatasetVersion(String[] sites, Long version){
        DatasetVersions sel = null;
        if(version == null){
            sel = new DatasetVersions.LatestDatasetVersions();
        } else {
            sel = new DatasetVersions.SpecificDatasetVersions(version);
        }
        
        sel.as("dsv");
        Case maybeFolder = new Case( sel.ds.datasetlogicalfolder.not_null(), 
                sel.ds.datasetlogicalfolder, sel.ds.datasetGroup);
        sel.selection( new Val<>("D").as( "type" ) );
        sel.selection( maybeFolder.as( "parent" ) );
        sel.selection( sel.ds.dataset.as( "pk" ) );
        return sel;
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
            Logger.getLogger( DatasetSearch.class.getName() )
                    .log( Level.WARNING, "Error parsing", ex);
            throw ex;
        }
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
    
    private Column getColumnFromSelectionScope(DatasetVersions dsv, String ident){
        for(MaybeHasAlias selection: dsv.getAvailableSelections()){
            if(selection.canonical().equals( ident ) && selection instanceof Column){
                dsv.selection( selection );
                return (Column) selection;
            }
        }
        return null;
    }

}
