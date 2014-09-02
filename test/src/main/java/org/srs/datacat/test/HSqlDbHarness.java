package org.srs.datacat.test;

import java.io.InputStream;
import javax.naming.Reference;
import javax.sql.DataSource;


import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;


import org.hsqldb.jdbc.JDBCPool;

/**
 * Hello world!
 <bean class="org.apache.commons.dbcp2.BasicDataSource" id="dataSource">
    <property name="driverClassName" value="org.hsqldb.jdbcDriver" />
    <property name="url" value="jdbc:hsqldb:file:#{systemProperties['user.home']}/db/data" />
    <property name="username" value="sa" />
    <property name="password" value="" />
</bean>
 */
public class HSqlDbHarness 
{
   
    static String url = "jdbc:hsqldb:mem:test";
    private final String driver = "org.hsqldb.jdbc.JDBCDriver";
    DataSource ds;
    
    public HSqlDbHarness() throws SQLException{
        
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        
        ConnectionFactory connectionFactory =
            new DriverManagerConnectionFactory(url,null);

        PoolableConnectionFactory poolableConnectionFactory =
            new PoolableConnectionFactory(connectionFactory, null);

        ObjectPool<PoolableConnection> connectionPool =
                new GenericObjectPool<>(poolableConnectionFactory);
        
        poolableConnectionFactory.setPool(connectionPool);

        PoolingDataSource<PoolableConnection> dataSource =
                new PoolingDataSource<>(connectionPool);
        dataSource.setAccessToUnderlyingConnectionAllowed(true);
        this.ds = dataSource;
        try {
            init();
        } catch (SQLException ex){
            ex.printStackTrace();
        }
        
    }
    
    private void init() throws SQLException{
        Connection conn = ds.getConnection();
        executeScript(conn, getClass().getResourceAsStream("hsqldb-schema.sql"), true);
        executeScript(conn, getClass().getResourceAsStream("init-junit.sql"), true);
        conn.commit();
    }

    public void executeScript(Connection conn, InputStream scriptStream, boolean swallowErrors) throws SQLException{
        Statement stmt = null;
        Scanner scanner = new Scanner(scriptStream);
        scanner.useDelimiter("(;(\r)?\n)|(--\n)");
        try {
            stmt = conn.createStatement();
            String last = "";
            while(scanner.hasNext()){
                String line = scanner.next().trim();

                if(line.startsWith( "--" )){
                    int start = 2;
                    while(start < line.length() && line.charAt(start) != '\n'){
                        start++;
                    }
                    line = line.substring( start ).trim();
                }
                
                if(line.startsWith( "/*!" ) && line.endsWith( "*/" )){
                    int i = line.indexOf( ' ' );
                    line = line.substring( i + 1, line.length() - " */".length() );
                    line = line.trim();
                }                
                
                if(line.length() > 0){
                    if(line.startsWith( "--" )){
                        continue;
                    }
                    String thisCommand = line.toLowerCase().split(" ")[0];
                    try {
                        stmt.execute( line );
                        last = thisCommand;
                    } catch (SQLException ex){
                        if(swallowErrors){
                            continue;
                        }
                        // If we failed a drop or alter statement...
                        if(last.isEmpty()){
                            if("drop".equals(thisCommand) ||"alter".equals(thisCommand) ) continue;
                        }
                        System.err.println("Failed on:\n" + line);
                        throw ex;
                    }
                }
            }
        } finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }
    
    public DataSource getDataSource(){
        return ds;
    }
    
    
    public static void main(String[] argv) throws SQLException{
        System.out.println(new HSqlDbHarness().getDataSource().toString());
    }
}
