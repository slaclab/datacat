package org.srs.datacat.dao.sql.search;

import com.google.common.base.Optional;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.file.DirectoryStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.freehep.commons.lang.AST;
import org.freehep.commons.lang.bool.Lexer;
import org.freehep.commons.lang.bool.Parser;
import org.freehep.commons.lang.bool.sym;
import static org.srs.datacat.dao.sql.search.DatasetSearch.sqlEscape;
import org.srs.datacat.dao.sql.search.plugins.DatacatPlugin;
import org.srs.datacat.dao.sql.search.tables.DatasetContainers;
import org.srs.datacat.dao.sql.search.tables.MetajoinedStatement;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.ModelProvider;
import org.zerorm.core.Column;
import static org.zerorm.core.Op.$;
import org.zerorm.core.Select;
import org.zerorm.core.Table;
import org.zerorm.core.interfaces.MaybeHasAlias;

/**
 *
 * @author bvan
 */
public class ContainerSearch {

    private Class<? extends DatacatPlugin>[] plugins;
    protected MetanameContext dmc;
    private ArrayList<String> metadataFields = new ArrayList<>();
    private Connection conn;
    private Select selectStatement;
    private ModelProvider modelProvider;

    public ContainerSearch(Connection conn, ModelProvider modelProvider,
            Class<? extends DatacatPlugin>... plugins) throws IOException{
        this.plugins = plugins;
        this.dmc = SearchUtils.buildContainerMetaInfoGlobalContext(conn);
        this.conn = conn;
        this.modelProvider = modelProvider;
    }

    public DirectoryStream<DatasetContainer> search(DirectoryStream<DatacatNode> containers, String query,
            String[] metaFieldsToRetrieve, String[] sortFields) throws ParseException, IOException{
        try {
            compileStatement(containers,
                    Optional.fromNullable(query),
                    Optional.fromNullable(metaFieldsToRetrieve),
                    Optional.fromNullable(sortFields));
            return retrieveContainers();
        } catch(SQLException ex) {
            throw new IOException("Error retrieving results", ex);
        }
    }

    protected DirectoryStream<DatasetContainer> retrieveContainers() throws IOException{
        try {
            return SearchUtils.getContainers(conn, modelProvider, selectStatement, metadataFields);
        } catch(SQLException ex) {
            throw new IOException("Error retrieving results", ex);
        }
    }

    protected Select compileStatement(DirectoryStream<DatacatNode> containers,
            Optional<String> query,
            Optional<String[]> retrieveFields,
            Optional<String[]> sortFields) throws ParseException, SQLException, IOException{
        DatasetContainers dsc = prepareDatasetContainers();
        // Prepare Search Context
        DatacatSearchContext sd = new DatacatSearchContext(dsc, plugins, dmc);

        // Process AST if there's a query
        if(query.isPresent() && !query.get().isEmpty()){
            AST ast = parseQueryString(query.get());
            // Allows us to do any last minute translation
            doRewrite(ast);
            sd.assertIdentsValid(ast);
            sd.evaluate(ast.getRoot());
            // In case we want to do something else, go ahead here
            dsc.where(sd.getEvaluatedExpr());
        }

        SearchUtils.populateParentTempTable(conn, containers);

        HashMap<String, MaybeHasAlias> availableSelections = new HashMap<>();
        for(MaybeHasAlias a: dsc.getAvailableSelections()){
            availableSelections.put(a.canonical(), a);
        }

        Table containerSearch = new Table("ContainerSearch", "cp");

        this.selectStatement = containerSearch
                .select(containerSearch.$("ContainerPath"))
                .join(dsc, $("dsc.pk").eq(containerSearch.$("DatasetLogicalFolder"))) // TODO: fix bug in zerorm
                .selection(dsc.getColumns());

        //handleSortFields(sd, dsc, sortFields);
        handleRetrieveFields(sd, dsc, retrieveFields);

        return selectStatement;
    }

    private DatasetContainers prepareDatasetContainers(){
        DatasetContainers containers = new DatasetContainers();
        containers.as("dsc");
        containers.selection(containers.lf.datasetLogicalFolder.as("pk"));
        return containers;
    }

    private AST parseQueryString(String queryString) throws ParseException{
        Lexer scanner = new Lexer(new StringReader(queryString));
        Parser p = new Parser(scanner);
        try {
            return (AST) p.parse().value;
        } catch(Exception ex) {
            if(ex instanceof RuntimeException){
                Throwable cause = ex.getCause();
                if(cause instanceof ParseException){
                    throw (ParseException) cause;
                }
                throw (RuntimeException) ex;
            }
            Logger.getLogger(DatasetSearch.class.getName())
                    .log(Level.WARNING, "Error parsing", ex);
            throw new RuntimeException(ex);
        }
    }

    protected String doRewriteIdent(String ident){
        switch(ident){
            case "resource":
                return "path";
            case "size":
                return "fileSizeBytes";
            default:
                return ident;
        }
    }

    protected void doRewrite(AST ast){
        AST.Node root = ast.getRoot();
        Set<String> idents = new HashSet<>();
        for(String ident: (Collection<String>) root.getMetadata("idents")){
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
                    if(!oldIdent.equals(newIdent)){
                        n.setValue(newIdent);
                        changed = true;
                    }
                }
                if(n.getRight() != null){
                    if(n.getType() == sym.NOT_MATCHES || n.getType() == sym.MATCHES){
                        AST.Node r = n.getRight();
                        r.setValue(sqlEscape((String) r.getValue()));
                    }
                    // If we are searching the checksum, make sure to convert a string back to a number
                    if("checksum".equals(n.getLeft().getValue()) && n.getRight().getValue() instanceof String){
                        AST.Node r = n.getRight();
                        r.setValue(new BigInteger(n.getRight().getValue().toString(), 16));
                    }
                    changed |= visit(n.getRight());
                }
                return changed;
            }
        };
        if(root.accept(visitor)){
            System.out.println("rewrote at least once");
        }
    }

    private void handleRetrieveFields(DatacatSearchContext sd, MetajoinedStatement dsv,
            Optional<String[]> retrieveFields){

        if(retrieveFields.isPresent()){
            for(String s: retrieveFields.get()){
                Column retrieve = null;
                if(sd.inSelectionScope(s)){
                    retrieve = getColumnFromSelectionScope(dsv, s);
                    metadataFields.add(s);
                } else if(sd.inPluginScope(s)){
                    // TODO: This should be cleaner
                    DatacatPlugin plugin = sd.pluginScope.getPlugin(s);
                    String fIdent = s.split("\\.")[1];
                    if(!sd.inSelectionScope(fIdent)){
                        for(Object o: plugin.joinToStatement(fIdent, dsv).getColumns()){
                            if(o instanceof Column){
                                Column cc = (Column) o;
                                if(cc.canonical().equals(fIdent)){
                                    retrieve = cc;
                                    break;
                                }
                            }
                        }
                    }
                    retrieve = getColumnFromSelectionScope(dsv, fIdent);
                    metadataFields.add(fIdent);
                } else if(sd.inMetanameScope(s)){
                    String aliased = "\"" + s + "\"";
                    retrieve = getColumnFromAllScope(dsv, aliased);
                    if(retrieve == null){
                        Iterator<Class> typeIter = dmc.getTypes(s).iterator();
                        Class type = typeIter.hasNext() ? typeIter.next() : null;
                        dsv.setupMetadataOuterJoin(s, type);
                        retrieve = getColumnFromAllScope(dsv, aliased);
                    }
                    metadataFields.add(s);
                } else {
                    retrieve = getColumnFromSelectionScope(dsv, s);
                    metadataFields.add(s);
                }
                if(retrieve == null){
                    throw new IllegalArgumentException("Unable to find retrieval field: " + s);
                }
                selectStatement.selection(retrieve);
            }
        }
    }
    
    private Column getColumnFromSelectionScope(MetajoinedStatement dsv, String ident){
        for(MaybeHasAlias selection: dsv.getAvailableSelections()){
            if(selection.canonical().equals( ident ) && selection instanceof Column){
                dsv.selection( selection );
                return (Column) selection;
            }
        }
        return null;
    }
    
    private Column getColumnFromAllScope(MetajoinedStatement dsv, String ident){
        for(MaybeHasAlias selection: dsv.getColumns()){
            if(selection.canonical().equals( ident ) && selection instanceof Column){
                return (Column) selection;
            }
        }
        return null;
    }

}
