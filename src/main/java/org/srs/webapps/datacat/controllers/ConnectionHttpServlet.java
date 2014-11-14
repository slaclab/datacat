
package org.srs.webapps.datacat.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.sql.DataSource;
import org.srs.datacat.rest.security.GroupManagerLookupService;
import org.srs.datacat.vfs.DcFileSystemProvider;

/**
 *
 * @author bvan
 */
public abstract class ConnectionHttpServlet extends HttpServlet {

    private DataSource dataSource;
    private DcFileSystemProvider provider;
    

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
    
    public DcFileSystemProvider getProvider(){
        if(provider == null){
            try {
                provider = new DcFileSystemProvider(initDatasource("jdbc/datacat-prod"), new GroupManagerLookupService());
            } catch(IOException ex) {
                //
            }
        }
        return provider;
    }
    
}
