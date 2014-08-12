
package org.srs.datacatalog.search;

import com.google.common.collect.Multiset;
import java.util.HashMap;
import org.freehep.commons.lang.AST;
import org.zerorm.core.Column;
import org.zerorm.core.Expr;
import org.zerorm.core.Op;
import org.zerorm.core.Param;
import org.zerorm.core.Select;
import org.zerorm.core.interfaces.MaybeHasAlias;
import org.zerorm.core.interfaces.MaybeHasParams;
import org.zerorm.core.interfaces.SimpleTable;
import org.srs.datacatalog.search.plugins.DatacatPlugin;
import org.srs.datacatalog.search.tables.MetajoinedStatement;

/**
 *
 * @author bvan
 */
public class DatacatSearchContext implements SearchContext {

    public static class PluginScope {
        HashMap<String, DatacatPlugin> pluginMap;

        public PluginScope(HashMap<String, DatacatPlugin> pluginMap){
            this.pluginMap = pluginMap;
        }

        public boolean contains(String ident){
            if(ident.contains( "." )){
                String[] ns_plugin = ident.split( "\\." );
                if(pluginMap.containsKey( ns_plugin[0] )){
                    return pluginMap.get( ns_plugin[0] ).containsKey( ns_plugin[1] );
                }
            }
            return false;
        }
        
        public DatacatPlugin getPlugin(String ident){
            if(ident.contains( "." )){
                String[] ns_plugin = ident.split( "\\." );
                if(pluginMap.containsKey( ns_plugin[0] )){
                    return pluginMap.get( ns_plugin[0] );
                }
            }
            return null;
        }
    }
    
    public static class MetavalueScope {
        MetanameContext context;

        public MetavalueScope(MetanameContext context){
            this.context = context;
        }

        public boolean contains(String ident){
            return context.contains( ident );
        }
    }
    
    final MetajoinedStatement dsv;
    final PluginScope pluginScope;
    final MetanameContext metanameContext;
    
    public DatacatSearchContext(MetajoinedStatement dsv, HashMap<String, DatacatPlugin> pluginMap, 
            MetanameContext context){
        this.dsv = dsv;
        this.pluginScope = new PluginScope( pluginMap );
        this.metanameContext = context;
    }
    
    @Override
    public boolean inSelectionScope(String ident){
        for(MaybeHasAlias selection: getStatement().getAvailableSelections()){
            if(selection.canonical().equals( ident ) && selection instanceof MaybeHasAlias){
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void assertIdentsValid(AST ast){
        Multiset<String> exprIdents = (Multiset<String>) ast.getRoot().getMetadata( "idents" );
        for(String s: exprIdents.elementSet()){
            if(!inScope( s )){
                throw new IllegalArgumentException(
                        "Unable to resolve '" + s + "' in '" + getErrorString( ast, s ) + "'" );
            }
        }
    }

    @Override
    public boolean inPluginScope(String ident){
        return pluginScope.contains( ident );
    }

    @Override
    public boolean inMetanameScope(String ident){
        return metanameContext.contains( ident );
    }
    
    @Override
    public boolean inScope(String ident){
        return inSelectionScope(ident) || inPluginScope(ident) || inMetanameScope(ident);
    }
    
    @Override
    public Select getStatement(){
        return dsv;
    }

    @Override
    public Expr evaluateNode(AST.Node node){

        Object tLeft = getTokenOrExpression( node.getLeft() );
        Object tRight = getTokenOrExpression( node.getRight() );
        Op tOper = node.getValue() != null ? Op.valueOf( node.getValue().toString() ) : null;
        
        if(tLeft != null || tOper != null || tRight != null){
            
            if( tOper == Op.AND || tOper == Op.OR){
                return tOper.apply( (Expr) tLeft, (Expr) tRight );
            }
            
            return preEvaluateExpression( (MetajoinedStatement)dsv, node, tLeft, tOper, tRight);
        }
        return null;
    }
    
    public Expr evaluateNode(AST.Node node, MetajoinedStatement statement){
        Object tLeft = getTokenOrExpression( node.getLeft(), statement );
        Object tRight = getTokenOrExpression( node.getRight(), statement );
        Op tOper = node.getValue() != null ? Op.valueOf( node.getValue().toString() ) : null;
        
        if(tLeft != null || tOper != null || tRight != null){
            
            if( tOper == Op.AND || tOper == Op.OR){
                return tOper.apply( (Expr) tLeft, (Expr) tRight );
            }
            return preEvaluateExpression(statement, node, tLeft, tOper, tRight);
        }
        return null;
    }
    
    private Object getTokenOrExpression(AST.Node node){
        if(node == null) {
            return null;
        }
        // If it's a value node, try to find the value (Timestamp, String, Number)
        if(node.getLeft() == null && node.getRight() == null){
            Object val = node.getValue();
            
            if(val instanceof String) {
                if(inSelectionScope( val.toString() )){
                    return getColumnFromSelectionScope( val.toString() );
                }

                if(pluginScope.contains( val.toString() )){
                    // Optionally, Join plugin/ foreign table here
                }
            }
            return val;
        }
        // Otherwise, evaluate it
        return evaluateNode( node );
    }
    
    private Object getTokenOrExpression(AST.Node node, MetajoinedStatement statement){
        if(node == null) {
            return null;
        }
        // If it's a value node, try to find the value (Timestamp, String, Number)
        if(node.getLeft() == null && node.getRight() == null){
            Object val = node.getValue();
            
            if(val instanceof String) {
                if(inSelectionScope( val.toString() )){
                    return getColumnFromSelectionScope( val.toString() );
                }

                if(pluginScope.contains( val.toString() )){
                    // Optionally, Join plugin/ foreign table here
                }
            }
            return val;
        }
        // Otherwise, evaluate it
        return evaluateNode( node, statement );
    }
    
    private Expr preEvaluateExpression(MetajoinedStatement statement, AST.Node leftNode, Object tLeft, Op tOper, Object tRight){
        String ident = leftNode.getLeft().getValue().toString();
        
        if( tLeft instanceof Column){
            Column c = (Column) tLeft;
            if( !(tRight instanceof MaybeHasParams) ){
                tRight = c.checkedParam( c.getName(), tRight);
            }
            return tOper.apply( (MaybeHasAlias) tLeft,  tRight );
        }
        
        if( pluginScope.contains( ident ) ){
            DatacatPlugin plugin = pluginScope.getPlugin( (String) tLeft );
            String fIdent = ((String) tLeft).split( "\\.")[1];
            SimpleTable t = plugin.joinToStatement( statement );
            Column c = null;
            for(Object o: t.getColumns()){
                if(o instanceof Column){
                    Column cc = (Column) o;
                    if( cc.canonical().equals( fIdent ) ){
                        c = cc;
                        break;
                    }
                } 
            }
            //TODO: 
            if( !(tRight instanceof MaybeHasParams)){
                Param r = c.checkedParam( c.getName(), tRight);
                tRight = r;
            }
            return tOper.apply( c, tRight );
        }
        return statement.getMetadataExpression( tLeft, tOper, tRight);
    }
   
    private Column getColumnFromSelectionScope(String ident){
        for(MaybeHasAlias selection: getStatement().getAvailableSelections()){
            if(selection.canonical().equals( ident ) && selection instanceof Column){
                getStatement().selection( selection );
                return (Column) selection;
            }
        }
        return null;
    }
    
    private String getErrorString(AST ast, final String ident){
        if(ast.toString().length() < 32){
            return ast.toString();
        }
        
        final StringBuilder startOfError = new StringBuilder();
        AST.Visitor errorVisitor = new AST.Visitor() {
            public boolean visit(AST.Node n){
                if(n.getLeft() != null && n.getRight() != null) {
                    startOfError.append( "( " );
                }
                boolean continueVisit = true;
                if(n.getLeft() != null) {
                    continueVisit = n.getLeft().accept( this );
                }
                if(continueVisit && n.getValue() != null){
                    continueVisit = !ident.equals( n.getValue() );
                    startOfError.append( " " + n.getValue().toString() + " " );
                }
                if(continueVisit && n.getRight() != null){
                    continueVisit = n.getRight().accept( this );
                    if(continueVisit && n.getLeft() != null && n.getRight() != null){
                        startOfError.append( " )" );
                    }
                }

                if(!continueVisit){
                    int partial = ident.length() + 25;
                    startOfError.delete( 0, startOfError.length() - partial );
                    startOfError.insert( 0, "..." );
                }
                return continueVisit;
            }
        };
        ast.getRoot().accept( errorVisitor );
        return startOfError.toString();
    }

}
