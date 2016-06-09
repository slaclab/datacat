package org.srs.datacat.dao.sql.search;

import java.util.Collection;
import java.util.HashMap;
import org.freehep.commons.lang.AST;
import org.freehep.commons.lang.bool.sym;
import org.zerorm.core.Column;
import org.zerorm.core.Expr;
import org.zerorm.core.Op;
import org.zerorm.core.Param;
import org.zerorm.core.Select;
import org.zerorm.core.interfaces.MaybeHasAlias;
import org.zerorm.core.interfaces.MaybeHasParams;
import org.zerorm.core.interfaces.SimpleTable;
import org.srs.datacat.dao.sql.search.plugins.DatacatPlugin;
import org.srs.datacat.dao.sql.search.plugins.DatacatPluginProvider;
import org.srs.datacat.dao.sql.search.tables.MetajoinedStatement;

/**
 *
 * @author bvan
 */
public class DatacatSearchContext implements SearchContext {

    /**
     * Class representing the keyword scope of all the plugins.
     */
    public static class PluginScope implements DatacatPluginProvider {
        HashMap<String, DatacatPlugin> pluginMap;

        public PluginScope(Class<? extends DatacatPlugin>[] plugins){
            this.pluginMap = new HashMap<>();
            for(Class<? extends DatacatPlugin> p: plugins){
                try {
                    DatacatPlugin plugin = p.newInstance();
                    this.pluginMap.put(plugin.getNamespace(), plugin);
                } catch(InstantiationException | IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        @Override
        public boolean hasPlugin(String ident){
            if(ident.contains(".")){
                String[] nsPlugin = ident.split("\\.");
                if(pluginMap.containsKey(nsPlugin[0])){
                    return pluginMap.get(nsPlugin[0]).containsKey(nsPlugin[1]);
                }
            }
            return false;
        }

        @Override
        public DatacatPlugin getPlugin(String ident){
            if(ident.contains(".")){
                String[] nsPlugin = ident.split("\\.");
                if(pluginMap.containsKey(nsPlugin[0])){
                    return pluginMap.get(nsPlugin[0]);
                }
            }
            return null;
        }
    }

    final MetajoinedStatement dsv;
    final PluginScope pluginScope;
    final MetanameContext metanameContext;
    private Expr evaluatedExpr;

    public DatacatSearchContext(MetajoinedStatement dsv, Class<? extends DatacatPlugin>[] plugins,
            MetanameContext context){
        this.dsv = dsv;
        this.pluginScope = new PluginScope(plugins);
        this.metanameContext = context;
    }

    @Override
    public boolean inSelectionScope(String ident){
        for(MaybeHasAlias selection: getStatement().getAvailableSelections()){
            if(selection.canonical().equals(ident)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void assertIdentsValid(AST ast){
        Collection<String> exprIdents = (Collection<String>) ast.getRoot().getMetadata("idents");
        for(String s: exprIdents){
            if(!inScope(s)){
                throw new IllegalArgumentException(
                        "Unable to resolve '" + s + "' in '" + SearchUtils.getErrorString(ast, s) + "'");
            }
        }
    }

    @Override
    public boolean inPluginScope(String ident){
        return pluginScope.hasPlugin(ident);
    }

    @Override
    public boolean inMetanameScope(String ident){
        return metanameContext.contains(ident);
    }

    @Override
    public boolean inScope(String ident){
        return inSelectionScope(ident) || inPluginScope(ident) || inMetanameScope(ident);
    }

    public Select getStatement(){
        return dsv;
    }

    public Expr getEvaluatedExpr(){
        return evaluatedExpr;
    }

    public void evaluate(AST.Node node){
        this.evaluatedExpr = evaluateNode(node, dsv);
    }

    private Expr evaluateNode(AST.Node node, MetajoinedStatement statement){
        Object tLeft = getTokenOrExpression(node.getLeft(), statement);
        Object tRight = getTokenOrExpression(node.getRight(), statement);
        Op tOper = null;
        if(node.getValue() != null){
            // Op translation
            String opName = node.getValue().toString();
            switch(opName){
                case "MATCHES":
                    opName = "LIKE";
                    break;
                case "NOT_MATCHES":
                    opName = "NOT_LIKE";
                    break;
                case "IN":
                    switch(node.getRight().getType()){
                        case sym.NUMRANGE:
                        case sym.STRINGRANGE:
                        case sym.DATERANGE:
                            opName = "BETWEEN";
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
            tOper = Op.valueOf(opName);
        }

        if(tLeft != null || tOper != null || tRight != null){
            if(tOper == Op.AND || tOper == Op.OR){
                return tOper.apply((Expr) tLeft, (Expr) tRight);
            }
            return preEvaluateExpression(statement, node, tLeft, tOper, tRight);
        }
        return null;
    }

    private Object getTokenOrExpression(AST.Node node, MetajoinedStatement statement){
        if(node == null){
            return null;
        }
        return node.isValueNode() ? getValueNode(node.getValue()) : evaluateNode(node, statement);
    }

    protected Object getValueNode(Object nodeValue){
        if(nodeValue instanceof String){
            String strVal = (String) nodeValue;
            if(inSelectionScope(strVal)){
                return getColumnFromSelectionScope(strVal);
            }
        }
        return nodeValue;
    }

    private Expr preEvaluateExpression(MetajoinedStatement statement,
            AST.Node leftNode, Object tLeft, Op tOper, Object tRight){
        String ident = leftNode.getLeft().getValue().toString();

        if(tLeft instanceof Column){
            Column c = (Column) tLeft;
            if(!(tRight instanceof MaybeHasParams)){
                tRight = c.checkedParam(c.getName(), tRight);
            }
            return tOper.apply((MaybeHasAlias) tLeft, tRight);
        }

        if(pluginScope.hasPlugin(ident)){
            DatacatPlugin plugin = pluginScope.getPlugin((String) tLeft);
            String fIdent = ((String) tLeft).split("\\.")[1];
            SimpleTable t = plugin.joinToStatement(fIdent, statement);
            Column c = null;
            for(Object o: t.getColumns()){
                if(o instanceof Column){
                    Column cc = (Column) o;
                    if(cc.canonical().equals(fIdent)){
                        c = cc;
                        break;
                    }
                }
            }
            //TODO: 
            if(!(tRight instanceof MaybeHasParams)){
                Param r = c.checkedParam(c.getName(), tRight);
                tRight = r;
            }
            return tOper.apply(c, tRight);
        }

        /* When tRight is null (denoted by Void.TYPE in freehep-lang),
           translate EQ/NOT_EQ null to IS_NULL/NOT_NULL
         */
        if(tRight == Void.TYPE){
            switch(tOper){
                case EQ:
                    tOper = Op.IS_NULL;
                    break;
                case NOT_EQ:
                    tOper = Op.NOT_NULL;
                    break;
                default:
                    throw new IllegalArgumentException("Fatal Error while parsing null");
            }
            if(metanameContext.getTypes(tLeft.toString()).size() == 1){
                tRight = metanameContext.getTypes(tLeft.toString()).toArray()[0];
            }
        }
        return statement.getMetadataExpression(tLeft, tOper, tRight);
    }

    private Column getColumnFromSelectionScope(String ident){
        for(MaybeHasAlias selection: getStatement().getAvailableSelections()){
            if(selection.canonical().equals(ident) && selection instanceof Column){
                getStatement().selection(selection);
                return (Column) selection;
            }
        }
        return null;
    }

}
