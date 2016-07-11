package org.srs.datacat.rest;

import java.io.IOException;
import javax.sql.DataSource;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.srs.datacat.dao.DAOFactory;
import org.srs.datacat.dao.sql.mysql.DAOFactoryMySQL;
import org.srs.datacat.model.ModelProvider;
import org.srs.datacat.security.DcUserLookupService;
import org.srs.datacat.shared.Provider;
import org.srs.datacat.vfs.DcFileSystemProvider;
import org.srs.datacat.dao.sql.search.plugins.EXODatacatSearchPlugin;
import org.srs.datacat.dao.sql.search.plugins.LsstFilesSearchPlugin;
import org.srs.datacat.dao.sql.search.plugins.LsstKVSearchPlugin;
import org.srs.datacat.dao.sql.search.plugins.LsstPositionsSearchPlugin;
import org.srs.datacat.rest.JerseyBinders.*;

/**
 *
 * @author bvan
 */
public class App extends ResourceConfig {
    private static boolean classesLoaded = false;
    public DcFileSystemProvider fsProvider;
    private DataSource dataSource;
    private DcUserLookupService lookup;

    public App(DataSource dataSource, ModelProvider modelProvider, DcUserLookupService lookupService){
        super();
        this.dataSource = dataSource;
        this.lookup = lookupService;
        registerInstances(new Reloader());
        init();
    }
    
    private void init(){
        ModelProvider modelProvider;
        try {
            DAOFactory factory = new DAOFactoryMySQL(dataSource);
            modelProvider = new Provider();
            fsProvider = new DcFileSystemProvider(factory, modelProvider);
        } catch(IOException ex) {
            throw new IllegalStateException(ex);
        }
        register(new JacksonFeature(modelProvider));
        register(RequestAcceptFilter.class);
        register(ListPlainTextProvider.class);
        register(ErrorResponse.ErrorTextProvider.class);
        register(DatacatObjectTextProvider.class);
        register(JacksonExceptionMapper.class);
        initPlugins();
    }

    private void initPlugins(){

        register(new DataSourceBinder(dataSource));
        register(new FsBinder(fsProvider));
        register(new LookupServiceBinder(lookup));

        SearchPluginProvider provider = new SearchPluginProvider(
                EXODatacatSearchPlugin.class,
                LsstFilesSearchPlugin.class,
                LsstKVSearchPlugin.class,
                LsstPositionsSearchPlugin.class
        );

        register(provider.binder);

        if(classesLoaded){
            return;
        }
        // Classloader block

        classesLoaded = true;
    }

    // We can do some cleanup possibly with this?
    public class Reloader implements ContainerLifecycleListener {
        Container container;

        public void reload(ResourceConfig newConfig){
            container.reload(newConfig);
        }

        public void reload(){
            container.reload();
        }

        @Override
        public void onStartup(Container container){
            System.out.println("onStartup: " + container.getConfiguration().toString());
            this.container = container;
        }

        @Override
        public void onReload(Container container){
            // ignore or do whatever you want after reload has been done
            System.out.println("onReload: " + container.getConfiguration().toString());
        }

        @Override
        public void onShutdown(Container container){
            // ignore or do something after the container has been shutdown
            System.out.println("onShutdown: " + container.getConfiguration().toString());
        }
    }

}
