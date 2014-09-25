
package org.srs.datacat.rest;

import java.util.HashMap;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.srs.datacatalog.search.plugins.DatacatPlugin;

/**
 *
 * @author Brian Van Klaveren<bvan@slac.stanford.edu>
 */
public class SearchPluginProvider {
    
    public class SearchPluginBinder extends AbstractBinder {
        @Override
        protected void configure(){
            bind(SearchPluginProvider.this).to(SearchPluginProvider.class);
        }
    }
    protected SearchPluginBinder binder = new SearchPluginBinder();
    private HashMap<String, DatacatPlugin> pluginMap = new HashMap<>();
    
    public SearchPluginProvider(DatacatPlugin... plugins){
        for(DatacatPlugin plugin : plugins){
            pluginMap.put(plugin.getNamespace(), plugin);
        }
    }
    
    public HashMap<String, DatacatPlugin> getPlugins(){
        return this.pluginMap;
    }

}
