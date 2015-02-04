package org.srs.datacat.rest;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.srs.datacat.dao.DAOFactory;
import org.srs.datacat.dao.sql.mysql.DAOFactoryMySQL;
import org.srs.datacat.model.ModelProvider;
import org.srs.datacat.rest.security.GroupManagerLookupService;
import org.srs.datacat.security.DcUserLookupService;    
import org.srs.datacat.shared.Provider;
import org.srs.datacat.vfs.DcFileSystemProvider;
import org.srs.datacatalog.search.plugins.EXODatacatSearchPlugin;
import org.srs.datacatalog.search.plugins.LsstFilesSearchPlugin;

/**
 *
 * @author bvan
 */
public class App extends ResourceConfig {
    private static boolean classesLoaded = false;
    public static DcFileSystemProvider fsProvider;
    private DataSource dataSource;
    private DcUserLookupService lookup;

    public static class JacksonFeature implements Feature {
        static JacksonJsonProvider jsonProvider;
        static JacksonXMLProvider xmlProvider;
        
        public JacksonFeature(){
            if(jsonProvider == null){
                ObjectMapper jsonMapper = new ObjectMapper();
                jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                XmlMapper xmlMapper = new XmlMapper();
                for(Entry<Class, Class> e
                        : fsProvider.getModelProvider().modelProviders().entrySet()){
                    jsonMapper.addMixIn(e.getKey(), e.getValue());
                    xmlMapper.addMixIn(e.getKey(), e.getValue());
                }

                jsonProvider = new JacksonJsonProvider(jsonMapper);
                xmlProvider = new JacksonXMLProvider(xmlMapper);
            }
        }

        @Override
        public boolean configure(final FeatureContext context){
            final String disableMoxy = CommonProperties.MOXY_JSON_FEATURE_DISABLE + '.'
                    + context.getConfiguration().getRuntimeType().name().toLowerCase();
            context.property( disableMoxy, true );

            context.register( xmlProvider, MessageBodyReader.class, 
                    MessageBodyWriter.class );
            
            context.register( jsonProvider, MessageBodyReader.class, 
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
        this(initDatasource("jdbc/datacat-prod"), new GroupManagerLookupService());
    }
        
    public App(DataSource dataSource, DcUserLookupService lookupService){
        super();
        this.dataSource = dataSource;
        this.lookup = lookupService;
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
        DAOFactory factory = new DAOFactoryMySQL(dataSource);
        ModelProvider modelProvider = new Provider();
        fsProvider = new DcFileSystemProvider(factory, modelProvider, lookup);
        register(new DataSourceBinder(dataSource));
        register(new FsBinder(fsProvider));
        
        SearchPluginProvider provider = new SearchPluginProvider(
                new EXODatacatSearchPlugin(),
                new LsstFilesSearchPlugin());

        register(provider.binder);
        
        if(classesLoaded)
            return;
        // Classloader block
        
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
