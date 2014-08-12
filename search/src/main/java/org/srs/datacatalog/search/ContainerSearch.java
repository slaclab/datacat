
package org.srs.datacatalog.search;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import org.srs.datacatalog.search.plugins.DatacatPlugin;

/**
 *
 * @author bvan
 */
public class ContainerSearch {
    
    Map<String, DatacatPlugin> pluginMap;
    MetanameContext dmc;
    
    public ContainerSearch(Map<String, DatacatPlugin> pluginMap,  MetanameContext dmc) throws Exception {
        this.pluginMap = pluginMap;
        this.dmc = dmc;
    }

}
