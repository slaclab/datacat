
package org.srs.datacat.sql;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.srs.datacat.model.DatasetContainer;
import org.srs.vfs.PathUtils;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetGroup;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.LogicalFolder;
import org.srs.datacat.vfs.DcPath;

/**
 *
 * @author bvan
 */
public class BaseDAO implements AutoCloseable {

    private Connection conn;    
    public BaseDAO(Connection conn){
        this.conn = conn;
    }
    
    @Override
    public void close() throws SQLException {
        if(conn != null){
            conn.close();
        }
    }
    
    public void commit() throws SQLException {
        conn.commit();
    }
    
    public void rollback() throws SQLException {
        if(conn != null){
            conn.rollback();
        }
    }
    
    public Connection getConnection(){
        return this.conn;
    }
    
    protected void delete1(String sql, Object o) throws SQLException{
        try (PreparedStatement stmt = getConnection().prepareStatement( sql )){
            stmt.setObject( 1, o);
            stmt.executeUpdate();
        }
    }
    
    public DatacatObject getDatacatObject(DcPath path) throws IOException, FileNotFoundException {
        DatacatObject next = new DatacatObject(null,null,"ROOT");
        for(int i = 0; i < path.getNameCount(); i++){
            next = getDatacatObject(next.getPk(), path.subpath(0, i+1).toAbsolutePath().toString());
        }
        return next;
    }
    
    public DatacatObject getDatacatObject(String path) throws IOException, FileNotFoundException {
        if(!PathUtils.isAbsolute( path )){
            path = "/" + path;
        }
        path = PathUtils.normalize( path );
        DatacatObject next = getDatacatObject(null, "/");
        int offsets[] = PathUtils.offsets(path);
        for(int i = 1; i <= offsets.length; i++){
            next = getDatacatObject( next.getPk(), PathUtils.absoluteSubpath(path, i, offsets));
        }
        return next;
    }
    
    public DatacatObject getDatacatObject(Long parentPk, DcPath path) throws IOException, FileNotFoundException {
        return getDatacatObject( parentPk, path.toString());
    }
    
    protected DatacatObject getDatacatObject(Long parentPk, String path) throws IOException, FileNotFoundException {
        try {
            return getChild( parentPk, path);
        } catch(SQLException ex) {
            throw new IOException("Unknown exception occurred in the database", ex);
        }
    }

    private DatacatObject getChild(Long parentPk, String path) throws SQLException, FileNotFoundException{
        int[] offsets = PathUtils.offsets(path);
        String fileName = PathUtils.getFileName(path, offsets);
        String parentPath = PathUtils.getParentPath(path, offsets);
        String nameParam = null;
        
        String parentClause;
        if(parentPath == null || "/".equals(path)){
            parentClause = " is null ";
        } else {
            nameParam = fileName;
            parentClause = " = ? and name = ?";
        }
        
        String sql = String.format("WITH OBJECTS (type, pk, name, parent) AS ( "
                + "    SELECT 'F', datasetlogicalfolder, name, parent "
                + "      FROM datasetlogicalfolder "
                + "  UNION ALL "
                + "    SELECT 'G', datasetGroup, name, datasetLogicalFolder "
                + "      FROM DatasetGroup "
                + "  UNION ALL "
                + "    SELECT 'D', dataset, datasetName, "
                + "      CASE WHEN datasetlogicalfolder is not null THEN datasetlogicalfolder else datasetgroup END "
                + "      FROM VerDataset "
                + ") "
                + "SELECT type, pk, name, parent FROM OBJECTS "
                + "  WHERE parent %s "
                + "  ORDER BY name", parentClause);

        DatacatObject.Builder builder = null;
        try(PreparedStatement stmt = getConnection().prepareStatement( sql )) {
            if(nameParam != null){
                stmt.setLong( 1, parentPk);
                stmt.setString( 2, nameParam);
            }
            ResultSet rs = stmt.executeQuery();
            if(!rs.next()){
                throw (new FileNotFoundException( "Unable to resolve objects: " + path ));
            }
            builder = getBuilder(rs, parentPath);
        }
        completeObject(builder);
        return builder.build();
    }
    
    protected void completeObject(org.srs.datacat.shared.DatacatObject.Builder builder) throws SQLException{

        if(builder instanceof org.srs.datacat.shared.Dataset.Builder){
            completeDataset((org.srs.datacat.shared.Dataset.Builder) builder );
        } else if(builder instanceof DatasetGroup.Builder){
            completeContainer((DatasetGroup.Builder) builder,
                    "select description from datasetgroup where datasetgroup = ?" );
            setContainerMetadata(builder );
        } else if(builder instanceof LogicalFolder.Builder){
            completeContainer((LogicalFolder.Builder) builder,
                    "select description from datasetlogicalfolder where datasetlogicalfolder = ?" );
            setContainerMetadata(builder );
        }
        
    }

    protected void completeDataset(Dataset.Builder builder) throws SQLException{
        String sql = "select vd.datasetfileformat, "
                + "vd.datasetdatatype, vd.latestversion, "
                + "vd.registered vregistered "
                + "from verdataset vd "
                + "where vd.dataset = ? ";

        try(PreparedStatement stmt = getConnection().prepareStatement( sql )) {
            stmt.setLong( 1, builder.pk );
            ResultSet rs = stmt.executeQuery();
            rs.next();
            builder.datasetFileFormat( rs.getString( "datasetfileformat" ) )
                .datasetDataType( rs.getString( "datasetdatatype" ) );
        }
    }

    protected void completeContainer(DatasetContainer.Builder builder, String sql) throws SQLException{
        try(PreparedStatement stmt = getConnection().prepareStatement( sql )) {
            stmt.setLong( 1, builder.pk );
            ResultSet rs = stmt.executeQuery();
            rs.next();
            builder.description( rs.getString( "description" ) );
        }
    }
    
    protected void setVersionMetadata(DatasetVersion.Builder builder) throws SQLException{
        HashMap<String, String> smap = new HashMap<>();
        HashMap<String, Number> nmap = new HashMap<>();
        String tableType = "verdataset";
        String column = "datasetversion";
        Long pk = builder.pk;
        String mdBase = "select metaname, metavalue from %smeta%s where %s = ?";
        String sql = String.format(mdBase, tableType, "string", column);
        try (PreparedStatement stmt = getConnection().prepareStatement( sql )){
            stmt.setLong(1, pk);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                smap.put(rs.getString("metaname"), rs.getString("metavalue"));
            }
        }

        sql = String.format(mdBase, tableType, "number", column);
        try (PreparedStatement stmt = getConnection().prepareStatement( sql )){
            stmt.setLong(1, pk);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Number n;
                java.math.BigDecimal v = (java.math.BigDecimal)rs.getObject("metavalue");
                n = v.scale()==0?v.toBigIntegerExact():v;
                nmap.put(rs.getString("metaname"), (Number)n);
            }
        }
        if(!nmap.isEmpty()){
            builder.numberMetadata( nmap );
        }
        if(!smap.isEmpty()) {
            builder.stringMetadata(smap);
        }
    }

    protected void setContainerMetadata(org.srs.datacat.shared.DatacatObject.Builder builder) throws SQLException{
        HashMap<String, String> smap = new HashMap<>();
        HashMap<String, Number> nmap = new HashMap<>();

        String tableType = null;
        String column = null;
        Long pk = builder.pk;
        if(builder instanceof LogicalFolder.Builder){
            tableType = "logicalfolder";
        } else if(builder instanceof DatasetGroup.Builder){
            tableType = "datasetgroup";
        }
        column = tableType;
        String mdBase = "select metaname, metavalue from %smeta%s where %s = ?";
        String sql = String.format( mdBase, tableType, "string", column );
        try(PreparedStatement stmt = getConnection().prepareStatement( sql )) {
            stmt.setLong( 1, pk );
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                smap.put( rs.getString( "metaname" ), rs.getString( "metavalue" ) );
            }
        }

        sql = String.format( mdBase, tableType, "number", column );
        try(PreparedStatement stmt = getConnection().prepareStatement( sql )) {
            stmt.setLong( 1, pk );
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                Number n;
                java.math.BigDecimal v = (java.math.BigDecimal) rs.getObject( "metavalue" );
                n = v.scale() == 0 ? v.toBigIntegerExact() : v;
                nmap.put( rs.getString( "metaname" ), (Number) n );
            }
        }
        if(!nmap.isEmpty()){
            builder.numberMetadata( nmap );
        }
        if(!smap.isEmpty()){
            builder.stringMetadata( smap );
        }
    }
    
    public void addDatasetVersionMetadata(DatasetVersion version, Map metaData) throws SQLException{
        addDatacatObjectMetadata( version.getPk(), metaData, "VerDataset", "DatasetVersion" );
    }

    public void addGroupMetadata(long datasetGroupPK, Map metaData) throws SQLException{
        addDatacatObjectMetadata( datasetGroupPK, metaData, "DatasetGroup", "DatasetGroup" );
    }

    public void addFolderMetadata(long logicalFolderPK, Map metaData) throws SQLException{
        addDatacatObjectMetadata( logicalFolderPK, metaData, "LogicalFolder", "LogicalFolder" );
    }
    
    private void addDatacatObjectMetadata(long objectPK, Map metaData, String tablePrefix, String column) throws SQLException{
        if(metaData == null){
            return;
        }
        if(!(metaData instanceof HashMap)){
            metaData = new HashMap( metaData );
        }
        final String metaSql = "insert into %sMeta%s (%s,MetaName,MetaValue) values (?,?,?)";
        String metaStringSql = String.format( metaSql, tablePrefix, "String", column );
        String metaNumberSql = String.format( metaSql, tablePrefix, "Number", column );
        String metaTimestampSql = String.format( metaSql, tablePrefix, "Timestamp", column );
        PreparedStatement stmtMetaString = getConnection().prepareStatement( metaStringSql );
        PreparedStatement stmtMetaNumber = getConnection().prepareStatement( metaNumberSql );
        PreparedStatement stmtMetaTimestamp = getConnection().prepareStatement( metaTimestampSql );
        PreparedStatement stmt = null;

        try {
            Iterator i = metaData.entrySet().iterator();
            while(i.hasNext()){
                Map.Entry e = (Map.Entry) i.next();
                String metaName = (String) e.getKey();
                Object metaValue = e.getValue();

                // Determine MetaData Object type and insert it into the appropriate table:
                if(metaValue instanceof Timestamp){
                    stmt = stmtMetaTimestamp;
                    stmt.setTimestamp( 3, (Timestamp) metaValue );
                } else if(metaValue instanceof Number){
                    stmt = stmtMetaNumber;
                    stmt.setObject( 3, metaValue );
                } else { // all others stored as String
                    stmt = stmtMetaString;
                    stmt.setString( 3, metaValue.toString() );
                }
                
                stmt.setLong(1, objectPK);
                stmt.setString(2, metaName);
                stmt.executeUpdate();
            }
        } finally {
            if(stmtMetaString != null){
                stmtMetaString.close();
            }
            if(stmtMetaNumber != null){
                stmtMetaNumber.close();
            }
            if(stmtMetaTimestamp != null){
                stmtMetaTimestamp.close();
            }
        }
    }
    
    public static DatacatObject.Type getType(String typeChar){
        switch(typeChar){
            case "F":
                return DatacatObject.Type.FOLDER;
            case "G":
                return DatacatObject.Type.GROUP;
            case "D":
                return DatacatObject.Type.DATASET;
        }
        return null;
    }
    
    public static DatacatObject.Builder getBuilder(ResultSet rs, String parentPath) throws SQLException {
        DatacatObject.Type type = getType(rs.getString("type"));
        DatacatObject.Builder o;
        switch (type){
            case DATASET:
                o =  new Dataset.Builder();
                break;
            case FOLDER:
                o = new LogicalFolder.Builder();
                break;
            case GROUP:
                o = new DatasetGroup.Builder();
                break;
            default:
                o = new DatacatObject.Builder();
        }
        String name = rs.getString( "name" );
        o.pk( rs.getLong( "pk" ) )
                .parentPk( rs.getLong( "parent" ) )
                .name(name);
        if(parentPath != null && !parentPath.isEmpty()){
            o.path(PathUtils.resolve( parentPath, name));
        }
        return o;
    }

}
