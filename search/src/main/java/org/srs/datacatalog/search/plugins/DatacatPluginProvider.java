
package org.srs.datacatalog.search.plugins;

/**
 *
 * @author bvan
 */
public interface DatacatPluginProvider {
    
    boolean hasPlugin(String plugin);
    DatacatPlugin getPlugin(String plugin);

}
