
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
    private Class<? extends DatacatPlugin>[] plugins;
    
    public SearchPluginProvider(Class<? extends DatacatPlugin>... plugins){
        this.plugins = plugins;
    }
    
    public Class<? extends DatacatPlugin>[] getPlugins(){
        return this.plugins;
    }

}
