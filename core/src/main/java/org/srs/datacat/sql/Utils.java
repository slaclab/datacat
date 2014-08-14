
package org.srs.datacat.sql;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.srs.datahandling.common.util.sql.DataCatConnectionManager;

/**
 *
 * @author bvan
 */
public class Utils {
    
    private static DataSource dataSource;

    public static DataSource getDataSource() throws SQLException {
        if(dataSource == null){
            try {
                initDatasource("jdbc/datacat-prod");
            } catch(NamingException ex) {
                return new DataSource(){

                    @Override
                    public Connection getConnection() throws SQLException{
                        return DataCatConnectionManager.instance( DataCatConnectionManager.DatabaseServerAlias.PROD ).getConnection();
                    }

                    @Override
                    public Connection getConnection(String username, String password) throws SQLException{
                        throw new UnsupportedOperationException( ""); }

                    @Override
                    public PrintWriter getLogWriter() throws SQLException{
                        throw new UnsupportedOperationException( ""); }

                    @Override
                    public void setLogWriter(PrintWriter out) throws SQLException{
                        throw new UnsupportedOperationException( ""); }

                    @Override
                    public void setLoginTimeout(int seconds) throws SQLException{
                        throw new UnsupportedOperationException( ""); }

                    @Override
                    public int getLoginTimeout() throws SQLException{
                        throw new UnsupportedOperationException( ""); }

                    @Override
                    public Logger getParentLogger() throws SQLFeatureNotSupportedException{
                        throw new UnsupportedOperationException( ""); }

                    @Override
                    public <T> T unwrap(Class<T> iface) throws SQLException{
                        throw new UnsupportedOperationException( ""); }

                    @Override
                    public boolean isWrapperFor(
                            Class<?> iface) throws SQLException{
                        throw new UnsupportedOperationException( ""); }
                    
                };
            }
        }
        return dataSource;
    }
    
    private static void initDatasource(String jndi) throws NamingException {
        javax.naming.Context ctx = new InitialContext();
        // relative to standard JNDI root for J2EE app
        javax.naming.Context envCtx = (javax.naming.Context) ctx.lookup( "java:comp/env" );
        dataSource = (DataSource) envCtx.lookup( jndi );
    }

}
