package org.srs.datacat.dao.sql.search.plugins;

import org.zerorm.core.Select;
import org.zerorm.core.interfaces.SimpleTable;

/**
 *
 * @author bvan
 */
public interface DatacatPlugin {

    String getNamespace();
    SimpleTable joinToStatement(String key, Select statement);
    boolean containsKey(String key);
    
}
