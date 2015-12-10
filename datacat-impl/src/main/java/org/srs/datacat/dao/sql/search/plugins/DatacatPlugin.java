package org.srs.datacat.dao.sql.search.plugins;

import org.srs.datacat.dao.sql.search.tables.DatasetVersions;
import org.zerorm.core.interfaces.SimpleTable;

/**
 *
 * @author bvan
 */
public interface DatacatPlugin {

    String getNamespace();
    SimpleTable joinToStatement(String key, DatasetVersions statement);
    boolean containsKey(String key);
    
}
