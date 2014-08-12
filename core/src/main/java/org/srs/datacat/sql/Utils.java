
package org.srs.datacat.sql;

import java.sql.Connection;
import java.sql.SQLException;
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
                throw new SQLException("Unable to initialize datasource", ex);
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
    
    public static Connection getConnection() throws SQLException {
        if(dataSource == null){
            try {
                initDatasource("jdbc/datacat-prod");
            } catch(NamingException ex) {
                return DataCatConnectionManager.instance( DataCatConnectionManager.DatabaseServerAlias.PROD ).getConnection();
            }
        }
        return dataSource.getConnection();
    }


}
