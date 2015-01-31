package org.srs.datacat.test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 *
 * @author bvan
 */
public abstract class DbHarness {
    public static Number[] numberMdValues = {0, new BigDecimal("3.14159"), 4294967296L, -1.0000000001};
    public static final String TEST_FILEFORMAT_01 = "junit.test";
    public static final String TEST_USER = "test_user";
    public static String[] alphaMdValues = {"abc", "def", "ghi", "jkl"};
    public static final String TEST_DATATYPE_01 = "JUNIT_TEST01";
    public static String alphaName = "alpha";
    public static String numberName = "num";
    public static final String TEST_BASE_NAME = "junit";
    public static final String TEST_DATASET_SOURCE = "JUNIT";
    public static final String TEST_BASE_PATH = "/" + TEST_BASE_NAME;

    public abstract DataSource getDataSource();

    public static DbHarness getDbHarness() throws SQLException{
        String harness = System.getProperty("db.test.harness");
        DbHarness h;
        if("mysql".equals(harness)){
            h = new MySQLDbHarness();
        } else if("hsqldb".equals(harness)){
            h = new HSqlDbHarness();
        } else {
            throw new IllegalArgumentException("unable to intiate database test harness");
        }
        Connection conn = h.getDataSource().getConnection();
        try {
            insertDatasetSource(conn, DbHarness.TEST_DATASET_SOURCE);
        } catch (SQLException ex){}
        try {
            insertDatasetDataType(conn, DbHarness.TEST_DATATYPE_01, null, null );
        } catch (SQLException ex){}
        try {
            insertDatasetFileFormat(conn, DbHarness.TEST_FILEFORMAT_01, null, null );
        } catch (SQLException ex){}
        conn.commit();
        conn.close();
        return h;
    }

    protected static String insertDatasetSource(Connection conn, String source) throws SQLException{
        String sql = "INSERT INTO DatasetSource (DatasetSource) VALUES (?)";
        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, source);
            stmt.executeUpdate();
            return source;
        }
    }

    protected static String insertDatasetDataType(Connection conn, String dataType, String description, Integer priority) throws SQLException{
        String sql = "INSERT INTO DatasetDataType (DatasetDataType, Description, CrawlerPriority) "
                + "VALUES (?,?,?)";
        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dataType.toUpperCase());
            stmt.setString(2, description);
            if(priority != null){
                stmt.setInt(3, priority);
            } else {
                stmt.setNull(3, java.sql.Types.INTEGER);
            }
            stmt.executeUpdate();
            return dataType.toUpperCase();
        }
    }

    protected static String insertDatasetFileFormat(Connection conn, String fileFormat, String description,
            String mimeType) throws SQLException{
        String sql = "INSERT INTO DatasetFileFormat (DatasetFileFormat, Description, MimeType) "
                + "VALUES (?,?,?)";
        try(PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fileFormat.toLowerCase());
            stmt.setString(2, description);
            if(mimeType != null){
                stmt.setString(3, mimeType);
            } else {
                stmt.setNull(3, java.sql.Types.VARCHAR);
            }
            stmt.executeUpdate();
            return fileFormat.toLowerCase();
        }
    }

}
