package org.srs.datacat.rest;


import org.srs.datacat.model.RequestAcceptFilter;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.srs.datacat.vfs.DcFileSystemProvider;
import org.srs.datacatalog.search.plugins.EXODatacatSearchPlugin;
import org.srs.rest.shared.ErrorResponse;
import org.srs.rest.shared.ListPlainTextProvider;
import org.srs.rest.shared.plugins.ResourcePlugin;

/**
 *
 * @author bvan
 */
public class App extends ResourceConfig {
    private static boolean classesLoaded = false;
    public static DcFileSystemProvider fsProvider;
    private DataSource dataSource;

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
    
    public static class FsBinder extends AbstractBinder {
        private final DcFileSystemProvider provider;
        
        FsBinder(DcFileSystemProvider provider){
            this.provider = provider;
        }

        @Override
        protected void configure(){
            bind(provider).to( DcFileSystemProvider.class);
        }
    }
    
    public static class DataSourceBinder extends AbstractBinder {
        private final DataSource dataSource;
        
        DataSourceBinder(DataSource dataSource){
            this.dataSource = dataSource;
        }

        @Override
        protected void configure(){
            bind(dataSource).to(DataSource.class);
        }
    }

    public App(){
        this(initDatasource("jdbc/datacat-prod"));
    }
        
    public App(DataSource dataSource){
        super();
        this.dataSource = dataSource;
        registerInstances( new Reloader() );
        register( JacksonFeature.class );
        register( RequestAcceptFilter.class );
        register( ListPlainTextProvider.class );
        register( ErrorResponse.ErrorTextProvider.class );
        register( DatacatObjectTextProvider.class );
                
        try {
            initPlugins();
        } catch(IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    public DcFileSystemProvider getFsProvider(){
        return this.fsProvider;
    }
    
    private static DataSource initDatasource(String jndi){
        try {
            javax.naming.Context ctx = new InitialContext();
            // relative to standard JNDI root for J2EE app
            javax.naming.Context envCtx = (javax.naming.Context) ctx.lookup( "java:comp/env" );
            final DataSource ds = (DataSource) envCtx.lookup( jndi );
            return new DataSource(){

                @Override
                public Connection getConnection() throws SQLException{
                    Connection conn = ds.getConnection();
                    conn.setAutoCommit(false);
                    return conn;
                }

                @Override
                public Connection getConnection(String username, String password) throws SQLException{
                    Connection conn = ds.getConnection( username, password );
                    conn.setAutoCommit(false);
                    return conn;
                }

                @Override
                public PrintWriter getLogWriter() throws SQLException{
                    return ds.getLogWriter();
                }

                @Override
                public void setLogWriter(PrintWriter out) throws SQLException{
                    ds.setLogWriter( out );
                }

                @Override
                public void setLoginTimeout(int seconds) throws SQLException{
                    ds.setLoginTimeout( seconds );
                }

                @Override
                public int getLoginTimeout() throws SQLException{
                    return ds.getLoginTimeout();
                }

                @Override
                public Logger getParentLogger() throws SQLFeatureNotSupportedException{
                    return ds.getParentLogger();
                }

                @Override
                public <T> T unwrap(Class<T> iface) throws SQLException{
                    return ds.unwrap( iface );
                }

                @Override
                public boolean isWrapperFor( Class<?> iface) throws SQLException{
                    return ds.isWrapperFor( iface );
                }
                
            };
        } catch(NamingException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    void initPlugins() throws IOException {
        fsProvider = new DcFileSystemProvider(dataSource);
        register(new DataSourceBinder(dataSource));
        register(new FsBinder(fsProvider));

        if(classesLoaded)
            return;
        
        SearchPluginProvider provider = new SearchPluginProvider(new EXODatacatSearchPlugin());
        register(provider.binder);
        
        /*File loc = new File( "/Users/bvan/.m2/repository/exo/exo-datacat-plugins/1.0-SNAPSHOT/exo-datacat-plugins-1.0-SNAPSHOT.jar" );

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

        }*/
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
