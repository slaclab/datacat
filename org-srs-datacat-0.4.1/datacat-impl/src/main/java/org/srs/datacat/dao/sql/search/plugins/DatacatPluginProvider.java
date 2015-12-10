
package org.srs.datacat.dao.sql.search.plugins;

/**
 *
 * @author bvan
 */
public interface DatacatPluginProvider {
    
    boolean hasPlugin(String plugin);
    DatacatPlugin getPlugin(String plugin);

}
