
package org.srs.datacat.rest;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.srs.datacat.dao.sql.search.plugins.DatacatPlugin;

/**
 *
 * @author bvan
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
