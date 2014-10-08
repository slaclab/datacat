
package org.srs.datacatalog.search;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.freehep.commons.lang.AST;
import org.freehep.commons.lang.bool.Lexer;
import org.freehep.commons.lang.bool.Parser;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcFileSystemProvider;
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
    
    //DcFileSystemProvider provider;
    HashMap<String, DatacatPlugin> pluginMap;
    MetanameContext dmc;
    ArrayList<String> metadataFields = new ArrayList<>();
    private DatasetView datasetView;
    private Connection conn;
    private Select selectStatement;
    
    public DatasetSearch(Connection conn, HashMap<String, DatacatPlugin> pluginMap) throws SQLException {
        this.pluginMap = pluginMap;
        this.dmc = SearchUtils.buildMetaInfoGlobalContext( conn );
        this.conn = conn;
    }
        
    public void compile(LinkedList<DcFile> containers, DatasetView datasetView, String queryString, String[] metaFieldsToRetrieve, String[] sortFields, int offset, int max) throws ParseException, IOException {
        try {
            compileStatement( containers, datasetView, queryString, 
                    metaFieldsToRetrieve, sortFields, offset, max );
        } catch (SQLException ex){
            throw new IOException("Error talking to database", ex);
        }
    }
    
    public List<Dataset> retrieveDatasets() throws IOException {
        try {
            return SearchUtils.getResults(conn, selectStatement, datasetView, metadataFields);
        } catch (SQLException ex) {
            throw new IOException("Error retrieving results", ex);
        }
    }
    
    public Select compileStatement(LinkedList<DcFile> containers, DatasetView datasetView, String queryString, String[] metaFieldsToRetrieve, String[] sortFields, int offset, int max) throws ParseException, SQLException, IOException {
        this.datasetView = datasetView;
        AST ast = parseQueryString(queryString);
        // Prepare DatasetVersions Selection 
        DatasetVersions dsv = prepareDatasetVersion(datasetView);
        
        // Reset any plugins
        for(DatacatPlugin p: pluginMap.values()){
            p.reset();
        }
        
        // Prepare Search Context
        DatacatSearchContext sd = new DatacatSearchContext(dsv, pluginMap, dmc);
        
        // Process AST
        if(ast != null){
            // Allows us to do any last minute translation
            doRewrite(ast);
            sd.assertIdentsValid(ast);
            sd.evaluate(ast.getRoot());
            // In case we want to do something else, go ahead here
            dsv.where(sd.getEvaluatedExpr());
        }
        
        SearchUtils.populateParentTempTable(conn, containers);

        HashMap<String, MaybeHasAlias> availableSelections = new HashMap<>();
        for(MaybeHasAlias a: dsv.getAvailableSelections()){
            availableSelections.put( a.canonical(), a);
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
        
        this.selectStatement = containerSearch
                .select( containerSearch.$("ContainerPath"))
                .join( dsv, 
                    Op.or( 
                        dsv.getSelection(dsv.ds.datasetlogicalfolder).eq(containerSearch.$("DatasetLogicalFolder")), 
                        dsv.getSelection(dsv.ds.datasetGroup).eq(containerSearch.$("DatasetGroup"))
                    )
                ).selection(dsv.getColumns());
        

        if(sortFields != null){
            for(String s: sortFields){
                boolean desc = s.startsWith("-") || s.endsWith("-");
                if(s.endsWith("-") || s.endsWith("+")){
                    s = s.substring( 0, s.length() - 1);
                }
                if(s.startsWith("-") || s.startsWith("+")){
                    s = s.substring(1);
                }

                Column orderBy = null;
                if(sd.inSelectionScope( s )){
                    orderBy = getColumnFromSelectionScope( dsv, s );
                } else if(sd.inPluginScope( s )){
                    // TODO: This should be cleaner
                    DatacatPlugin plugin = sd.pluginScope.getPlugin(s);
                    String fIdent = s.split( "\\.")[1];
                    for(Object o: plugin.joinToStatement(dsv).getColumns()){
                        if(o instanceof Column){
                            Column cc = (Column) o;
                            if( cc.canonical().equals( fIdent ) ){
                                orderBy = cc;
                                break;
                            }
                        }
                    }
                } else if(sd.inMetanameScope( s )){
                    if(dmc.getTypes( s ).size() > 1){
                        throw new IllegalArgumentException("Unable to sort on fields with multiple types");
                    }
                    String aliased = "\"" + s + "\"";
                    orderBy = getColumnFromAllScope( dsv, aliased);
                    if(orderBy == null){
                        Class type = dmc.getTypes( s ).toArray( new Class[0])[0];
                        dsv.setupMetadataJoin(s, type);
                        orderBy = getColumnFromAllScope( dsv, aliased);
                    }
                    metadataFields.add(s);
                } else {
                    orderBy = getColumnFromSelectionScope( dsv, s );
                    metadataFields.add(s);
                }
                if(orderBy == null){
                    throw new IllegalArgumentException("Unable to find sort field: " + s);
                }
                selectStatement.selection(orderBy);
                selectStatement.orderBy(orderBy, desc ? "DESC":"ASC");
            }
        }
        
        if(metaFieldsToRetrieve != null){
            for(String s: metaFieldsToRetrieve){
                Column retrieve = null;
                if(sd.inSelectionScope( s )){
                    retrieve = getColumnFromSelectionScope( dsv, s );
                } else if(sd.inPluginScope( s )){
                    // TODO: This should be cleaner
                    DatacatPlugin plugin = sd.pluginScope.getPlugin(s);
                    String fIdent = s.split( "\\.")[1];
                    for(Object o: plugin.joinToStatement(dsv).getColumns()){
                        if(o instanceof Column){
                            Column cc = (Column) o;
                            if( cc.canonical().equals( fIdent ) ){
                                retrieve = cc;
                                break;
                            }
                        }
                    }
                } else if(sd.inMetanameScope( s )){
                    String aliased = "\"" + s + "\"";
                    retrieve = getColumnFromAllScope( dsv, aliased);
                    if(retrieve == null){
                        Class type = dmc.getTypes( s ).toArray( new Class[0])[0];
                        dsv.setupMetadataOuterJoin( s, type);
                        retrieve = getColumnFromAllScope( dsv, aliased);
                    }
                    metadataFields.add( s );
                } else {
                    retrieve = getColumnFromSelectionScope( dsv, s );
                    metadataFields.add( s );
                }
                if(retrieve == null){
                    throw new IllegalArgumentException("Unable to find retrieval field: " + s);
                }
                selectStatement.selection(retrieve);
            }
        }
        
        if(offset > 0){
            Sql s = new Sql("rownum").as( "rnum");
            dsv.selection( s );
            selectStatement = new Select(selectStatement.getSelections()).from( selectStatement.as("ss") )
                    .where( Op.gt( new Sql(s.alias()), offset) );
        }
        return selectStatement;
    }
    
    protected String doRewriteIdent(String ident){
        switch(ident){
            case "resource":
                return "path";
            case "size":
                return "fileSizeBytes";
        }
        return ident;
    }
    
    protected void doRewrite(AST ast){
        AST.Node root = ast.getRoot();
        Set<String> idents = new HashSet<>();
        for(String ident: (Collection<String>) root.getMetadata( "idents")){
            idents.add(doRewriteIdent(ident));
        }
        root.setMetadata("idents", idents);
        AST.Visitor visitor = new AST.Visitor() {
            @Override
            public boolean visit(AST.Node n){
                boolean changed = false;
                if(n.getLeft() != null){
                    changed |= visit(n.getLeft());
                }
                if(n.isValueNode()){
                    String oldIdent = n.getValue().toString();
                    // Rewrite values here
                    String newIdent = doRewriteIdent(oldIdent);
                    if(!oldIdent.equals( newIdent)){
                        n.setValue(newIdent);
                        changed = true;
                    }
                }
                if(n.getRight() != null){
                    changed |= visit(n.getRight());
                }
                return changed;
            }
        };
        if(root.accept( visitor )){
            System.out.println("rewrote at least once");
        }
    }
    
    private DatasetVersions prepareDatasetVersion(DatasetView dsView){
        DatasetVersions sel = new DatasetVersions(dsView);        
        sel.as("dsv");
        Case maybeFolder = new Case( sel.ds.datasetlogicalfolder.not_null(), 
                sel.ds.datasetlogicalfolder, sel.ds.datasetGroup);
        sel.selection( new Val<>("D").as( "type" ) );
        sel.selection( maybeFolder.as( "parent" ) );
        sel.selection( sel.ds.dataset.as( "pk" ) );
        return sel;
    }
   
    private AST parseQueryString(String queryString) throws ParseException {
        if(queryString == null || queryString.isEmpty()){
            return null;
        }
        Lexer scanner = new Lexer( new StringReader(queryString) );
        Parser p = new Parser( scanner );
        try {
            return (AST) p.parse().value;
        } catch(Exception ex) {
            if(ex instanceof RuntimeException){
                if(ex.getCause() instanceof ParseException){
                    throw (ParseException) ex.getCause();
                }
                throw (RuntimeException) ex;
            }
            Logger.getLogger( DatasetSearch.class.getName() )
                    .log( Level.WARNING, "Error parsing", ex);
            throw new RuntimeException(ex);
        }
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
    
    private Column getColumnFromAllScope(DatasetVersions dsv, String ident){
        for(MaybeHasAlias selection: dsv.getColumns()){
            if(selection.canonical().equals( ident ) && selection instanceof Column){
                return (Column) selection;
            }
        }
        return null;
    }

}
