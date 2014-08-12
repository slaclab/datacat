package org.srs.datacat.rest;


import org.srs.datacat.model.RequestAcceptFilter;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.srs.datacat.vfs.DcFileSystemProvider;
import org.srs.rest.shared.ListPlainTextProvider;
import org.srs.rest.shared.plugins.ResourcePlugin;

/**
 *
 * @author bvan
 */
public class App extends ResourceConfig {
    private static boolean classesLoaded = false;
    public static DcFileSystemProvider fsProvider;

    public static class JacksonFeature implements Feature {

        @Override
        public boolean configure(final FeatureContext context){
            final String disableMoxy = CommonProperties.MOXY_JSON_FEATURE_DISABLE + '.'
                    + context.getConfiguration().getRuntimeType().name().toLowerCase();
            context.property( disableMoxy, true );
            context.register( JacksonJaxbJsonProvider.class, MessageBodyReader.class, 
                    MessageBodyWriter.class );
            return true;
        }
    }

    public App(){
        super();
        registerInstances( new Reloader() );
        register( JacksonFeature.class );
        register( RequestAcceptFilter.class );
        register( ListPlainTextProvider.class );
        register( DatacatObjectTextProvider.class );
        
        try {
            initPlugins();
        } catch(IOException ex) {
            Logger.getLogger( App.class.getName() ).log( Level.SEVERE, "Error while attempting to initiate plugins");
        }
    }
    
    public DcFileSystemProvider getFsProvider(){
        return this.fsProvider;
    }
    
    void initPlugins() throws IOException {
        if(fsProvider == null){
            fsProvider = new DcFileSystemProvider();
        }
        
        if(classesLoaded)
            return;
        File loc = new File( "/Users/bvan/.m2/repository/exo/exo-datacat-plugins/1.0-SNAPSHOT/exo-datacat-plugins-1.0-SNAPSHOT.jar" );

        URL[] urls = { loc.toURI().toURL() };
        URLClassLoader ucl = new URLClassLoader( urls, getClassLoader() );

        ServiceLoader<ResourcePlugin> sl = ServiceLoader.load( ResourcePlugin.class, ucl );
        Iterator<ResourcePlugin> apit = sl.iterator();
        
        while(apit.hasNext()){
            ResourcePlugin plugin = apit.next();
            System.out.println( plugin.getPath() );
            Resource r = Resource.builder( plugin.getClass(), true).build();
            System.out.println( r.getPath() );
            this.registerClasses( plugin.getClass() );

        }
        classesLoaded = true;
    }
    
    // We can do some cleanup possibly with this?
    public class Reloader implements ContainerLifecycleListener {
        Container container;

        public void reload(ResourceConfig newConfig){
            container.reload( newConfig );
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