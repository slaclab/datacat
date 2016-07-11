package org.srs.datacat.rest;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.srs.datacat.model.ModelProvider;
import org.srs.datacat.security.DcUserLookupService;

/**
 *
 * @author bvan
 */
public final class Utils {

    public static DataSource initDatasource(String jndiName){
        try {
            javax.naming.Context ctx = new InitialContext();
            // relative to standard JNDI root for J2EE app
            javax.naming.Context envCtx = (javax.naming.Context) ctx.lookup("java:comp/env");
            final DataSource ds = (DataSource) envCtx.lookup(jndiName);
            return new DataSource() {

                @Override
                public Connection getConnection() throws SQLException{
                    Connection conn = ds.getConnection();
                    conn.setAutoCommit(false);
                    return conn;
                }

                @Override
                public Connection getConnection(String username, String password) throws SQLException{
                    Connection conn = ds.getConnection(username, password);
                    conn.setAutoCommit(false);
                    return conn;
                }

                @Override
                public PrintWriter getLogWriter() throws SQLException{
                    return ds.getLogWriter();
                }

                @Override
                public void setLogWriter(PrintWriter out) throws SQLException{
                    ds.setLogWriter(out);
                }

                @Override
                public void setLoginTimeout(int seconds) throws SQLException{
                    ds.setLoginTimeout(seconds);
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
                    return ds.unwrap(iface);
                }

                @Override
                public boolean isWrapperFor(Class<?> iface) throws SQLException{
                    return ds.isWrapperFor(iface);
                }

            };
        } catch(NamingException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    public static DcUserLookupService initModelProvider(String clazz){
        try {
            return (DcUserLookupService) Class.forName(clazz).newInstance();
        } catch(ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            throw new IllegalStateException("Unable to instantiate Model Provider", ex);
        }
    }


    public static ModelProvider initModelProvider(Class<? extends ModelProvider> clazz){
        try {
            return clazz.newInstance();
        } catch(InstantiationException | IllegalAccessException ex) {
            throw new IllegalStateException("Unable to instantiate Model Provider", ex);
        }
    }

    public static DcUserLookupService initUserLookupService(String clazz){
        try {
            return (DcUserLookupService) Class.forName(clazz).newInstance();
        } catch(ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            throw new IllegalStateException("Unable to instantiate User Lookup Service", ex);
        }
    }
    
    public static DcUserLookupService initUserLookupService(
            Class<? extends DcUserLookupService> clazz){
        try {
            return clazz.newInstance();
        } catch(InstantiationException | IllegalAccessException ex) {
            throw new IllegalStateException("Unable to instantiate User Lookup Service", ex);
        }
    }

}
