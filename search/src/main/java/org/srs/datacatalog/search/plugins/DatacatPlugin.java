package org.srs.datacatalog.search.plugins;

import org.zerorm.core.Select;
import org.zerorm.core.interfaces.SimpleTable;

/**
 *
 * @author bvan
 */
public interface DatacatPlugin {

    public String getNamespace();
    public SimpleTable joinToStatement(String key, Select statement);
    public boolean containsKey(String key);
    
}
