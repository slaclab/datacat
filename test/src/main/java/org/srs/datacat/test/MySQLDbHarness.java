package org.srs.datacat.test;

import java.io.InputStream;


import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;


/**
 * HSQL DB Test Harness
 */
public class MySQLDbHarness extends DbHarness {
   
    static String url = "jdbc:hsqldb:mem:test";
    private final String driver = "org.hsqldb.jdbc.JDBCDriver";
    DataSource ds;
    
    public static String JUNIT_DATASET_DATATYPE = "JUNIT_TEST";
    public static String JUNIT_DATASET_DATASOURCE = "JUNIT_SOURCE";
    public static String JUNIT_DATASET_FILEFORMAT = "junit.test";
    
    protected MySQLDbHarness() throws SQLException{
        
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        
        Properties props = new Properties();
        props.put( "defaultAutoCommit", false);
        ConnectionFactory connectionFactory =
            new DriverManagerConnectionFactory(url,null);

        PoolableConnectionFactory poolableConnectionFactory =
            new PoolableConnectionFactory(connectionFactory, null);
        
        poolableConnectionFactory.setDefaultAutoCommit(false);

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
        executeScript(conn, getClass().getResourceAsStream("mysql-schema.sql"), true);
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
                    if(thisCommand.startsWith( "block")){
                        
                        line = line.substring("BLOCK ".length());
                        while(!line.endsWith( "END BLOCK")){
                            line = line + ";" + scanner.next().trim();
                        }
                        line = line.substring(0, line.length() - ";END BLOCK".length());
                    }
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
    
    @Override
    public DataSource getDataSource(){
        return ds;
    }

    
    public static void main(String[] argv) throws SQLException{
        System.out.println(new MySQLDbHarness().getDataSource().toString());
    }
    
    
    public long createFolder(Connection conn, long parentPk, String name) throws SQLException{
        String sql = "INSERT INTO DatasetLogicalFolder (NAME, PARENT) SELECT ?, DatasetLogicalFolder FROM DatasetLogicalFolder WHERE NAME = 'testpath'";
        try (PreparedStatement stmt = conn.prepareStatement( sql, new String[]{"DATASETLOGICALFOLDER"} )) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()){
                rs.next();
                return rs.getLong(1);
            }
        }
    }

}
