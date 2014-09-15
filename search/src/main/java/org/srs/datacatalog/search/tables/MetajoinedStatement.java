
package org.srs.datacatalog.search.tables;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.srs.datacatalog.search.SearchUtils;
import org.zerorm.core.Column;
import org.zerorm.core.Expr;
import org.zerorm.core.Op;
import org.zerorm.core.Param;
import org.zerorm.core.Select;
import org.zerorm.core.interfaces.MaybeHasAlias;

/**
 *
 * @author bvan
 */
public abstract class MetajoinedStatement extends Select {
    protected HashMap<String, Select> metaJoins = new LinkedHashMap<>();
    
    public abstract Map<String, Select> getMetajoins();

    public abstract Column getMetajoinColumn();
    
    public abstract Metatable getMetatableForType(String alias, Class type);
    
    public abstract String getMetanamePrefix();
    
    public Column setupMetadataJoin(String metaName,
        Class<?> type){
        Metatable ms = null;
        String sName = getMetanamePrefix() + getMetajoins().size();

        // Return the current joined table if we already added it
        if(getMetajoins().containsKey( metaName )){
            ms = (Metatable) getMetajoins().get( metaName ).getFrom();
            return ms.metaValue;
        }
        ms = getMetatableForType( sName, type );

        Select mSelect = ms.selectAllColumns()
                .where( ms.metaName.eq( metaName ) )
                .as( sName );

        getMetajoins().put( metaName, mSelect );
        selection( new Column( ms.metaValue.getName(), mSelect ).asExact( metaName ) )
                .leftOuterJoin(mSelect, getMetajoinColumn().eq( ms.datacatKey ) );
        return ms.metaValue;
    }
    
    public Expr getMetadataExpression(Object tLeft, Op tOper, Object tRight){
        for(MaybeHasAlias c: getColumns()){
            if(c.canonical().equals( tLeft )){
                return tOper.apply( c, tRight );
            }
        }
        
        if(tRight == null){
            throw new IllegalArgumentException("Unable to process null queries at this time");
        }
        Class<?> type = SearchUtils.getParamType(tRight);

        Object p = tRight instanceof List ? tRight : new Param<>( tLeft.toString(), tRight );
        if(tRight instanceof List && tOper == Op.BETWEEN){
            List r = (List) tRight;
            Object lower = new Param<>( tLeft.toString(), r.get(0));
            Object upper = new Param<>( tLeft.toString(), r.get(1));
            p = new Expr(lower, Op.AND, upper, false);
        }
        
        if(getMetajoins().get( tLeft ) == null){  // join not yet set up
            return tOper.apply( setupMetadataJoin(tLeft.toString(), type), p );
        }
        
        Metatable ms = (Metatable) getMetajoins().get( tLeft ).getFrom();
        return tOper.apply( ms.metaValue, p );
    }
}
