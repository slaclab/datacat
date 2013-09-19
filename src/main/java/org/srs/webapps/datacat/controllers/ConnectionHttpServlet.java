
package org.srs.webapps.datacat.controllers;

import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.sql.DataSource;

/**
 *
 * @author bvan
 */
public abstract class ConnectionHttpServlet extends HttpServlet {

    private DataSource dataSource;

    private void initDatasource(String jndi) throws NamingException {
        javax.naming.Context ctx = new InitialContext();
        // relative to standard JNDI root for J2EE app
        javax.naming.Context envCtx = (javax.naming.Context) ctx.lookup( "java:comp/env" );
        this.dataSource = (DataSource) envCtx.lookup( jndi );
    }
    
    public Connection getConnection() throws SQLException {
        if(dataSource == null){
            try {
                initDatasource("jdbc/datacat-prod");
            } catch(NamingException ex) {
                throw new SQLException("Unable to initialize datasource", ex);
            }
        }
        return dataSource.getConnection();
    }
    
}
