package org.srs.datacat.dao.sql.search.plugins;

import org.srs.datacat.dao.sql.search.tables.MetajoinedStatement;
import org.zerorm.core.interfaces.SimpleTable;

/**
 *
 * @author bvan
 */
public interface DatacatPlugin {

    String getNamespace();
    SimpleTable joinToStatement(String key, MetajoinedStatement statement);
    boolean containsKey(String key);
    
}
