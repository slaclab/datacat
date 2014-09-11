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
public class HSqlDbHarness {
   
    static String url = "jdbc:hsqldb:mem:test";
    private final String driver = "org.hsqldb.jdbc.JDBCDriver";
    DataSource ds;
    
    public static String JUNIT_DATASET_DATATYPE = "JUNIT_TEST";
    public static String JUNIT_DATASET_DATASOURCE = "JUNIT_SOURCE";
    public static String JUNIT_DATASET_FILEFORMAT = "junit.test";
    
    public HSqlDbHarness() throws SQLException{
        
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
        executeScript(conn, getClass().getResourceAsStream("hsqldb-schema.sql"), true);
        executeScript(conn, getClass().getResourceAsStream("init-junit.sql"), true);
        conn.commit();
        
//        
//        
//        String sql = "insert into VerDataset (DatasetName, DataSetFileFormat, DataSetDataType, DatasetLogicalFolder, DatasetGroup) values (?, ?, ?, ?, ?)";
//        long parent = createFolder( conn, 0, "testfolder.ins");
//        
//        long pk;
//        try (PreparedStatement stmt = conn.prepareStatement( sql, new String[]{"DATASET"})){
//            String name = "testds.ins";
//            stmt.setString( 1, name );
//            stmt.setString( 2, JUNIT_DATASET_FILEFORMAT );
//            stmt.setString( 3, JUNIT_DATASET_DATATYPE );
//            stmt.setLong( 4, parent );
//            stmt.setNull( 5, java.sql.Types.BIGINT );
//            stmt.executeUpdate();   // will throw exception if required parameter is empty...
//            try(ResultSet rs = stmt.getGeneratedKeys()) {
//                rs.next();
//                pk = rs.getLong( 1 );
//            }
//        }
//        sql = "insert into DatasetVersion (Dataset, VersionId, DatasetSource) VALUES (?,?,?)";
//        try (PreparedStatement stmt = conn.prepareStatement( sql, new String[]{"DATASETVERSION"})){
//            stmt.setLong( 1, pk);
//            stmt.setInt( 2, 0);
//            stmt.setString( 3, JUNIT_DATASET_DATASOURCE);
//            stmt.executeUpdate();   // will throw exception if required parameter is empty...
//            try(ResultSet rs = stmt.getGeneratedKeys()) {
//                rs.next();
//                pk = rs.getLong( 1 );
//            }
//        }
//        long l = 4294967296L;
//        Number n = ((Number) l);
//        sql = "insert into VerDatasetMetaNumber (DatasetVersion, MetaName, MetaValue) VALUES (?,?,?)";
//        try (PreparedStatement stmt = conn.prepareStatement( sql, new String[]{"DATASETVERSION"})){
//            stmt.setLong( 1, pk);
//            stmt.setString( 2, "test");
//            stmt.setObject( 3, n);
//            stmt.executeUpdate();   // will throw exception if required parameter is empty...
//            try(ResultSet rs = stmt.getGeneratedKeys()) {
//                rs.next();
//                pk = rs.getLong( 1 );
//            }
//        }
//        
//        sql = "SELECT * FROM VerDatasetMetaNumber";
//        try (PreparedStatement stmt = conn.prepareStatement( sql)){
//            ResultSet rs = stmt.executeQuery();
//            rs.next();
//            System.out.println(rs.getObject( "METAVALUE").toString());
//        }
        
//        try (PreparedStatement stmt = conn.prepareStatement( sql, new String[]{"DATASET"})){                
//            for(int i = 0; i < 100; i++){
//                String name = String.format( "folder%05d", i );
//                System.out.println( "creating new folder: /testpath/" + name );
//                
//                long pk = 0;
//                for(int j = 0; j < 1000; j++){
//                    name = String.format( "dataset%05d", j );
//                    stmt.setString( 1, name );
//                    stmt.setString( 2, JUNIT_DATASET_FILEFORMAT );
//                    stmt.setString( 3, JUNIT_DATASET_DATATYPE );
//                    stmt.setLong( 4, parent );
//                    stmt.setNull( 5, java.sql.Types.BIGINT );
//                    stmt.executeUpdate();   // will throw exception if required parameter is empty...
//                    try(ResultSet rs = stmt.getGeneratedKeys()) {
//                        rs.next();
//                        pk = rs.getLong( 1 );
//                    }
//                }
//                System.out.println( pk );
//            }
//        }
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
    
    
    public long createFolder(Connection conn, long parentPk, String name) throws SQLException{
        String sql = "INSERT INTO DATASETLOGICALFOLDER (NAME, PARENT) SELECT ?, DATASETLOGICALFOLDER FROM DATASETLOGICALFOLDER WHERE NAME = 'testpath'";
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
