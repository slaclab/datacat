package org.srs.datacat.dao.sql;

import com.google.common.base.Optional;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.shared.DatasetContainerBuilder;
import org.srs.datacat.model.dataset.DatasetLocationModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetGroup;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.LogicalFolder;
import org.srs.datacat.model.RecordType;
import org.srs.vfs.AbstractFsProvider.AfsException;
import org.srs.vfs.PathUtils;

/**
 *
 * @author bvan
 */
public class SqlBaseDAO implements org.srs.datacat.dao.BaseDAO {

    private final Connection conn;
    private final ReentrantLock lock;

    public SqlBaseDAO(Connection conn){
        this.conn = conn;
        this.lock = null;
    }

    public SqlBaseDAO(Connection conn, ReentrantLock lock){
        this.conn = conn;
        this.lock = lock;
    }

    @Override
    public void close() throws IOException{
        try {
            if(conn != null){
                conn.close();
            }
            if(lock != null && lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        } catch(SQLException ex) {
            throw new IOException("Error closing data source", ex);
        }

    }

    @Override
    public void commit() throws IOException{
        try {
            conn.commit();
            if(lock != null && lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        } catch(SQLException ex) {
            throw new IOException("Error committing changes", ex);
        }
    }

    protected void rollback() throws SQLException{
        if(conn != null){
            conn.rollback();
        }
        if(lock != null && lock.isHeldByCurrentThread()){
            lock.unlock();
        }
    }

    protected Connection getConnection(){
        return this.conn;
    }

    protected void delete1(String sql, Object o) throws SQLException{
        try(PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setObject(1, o);
            stmt.executeUpdate();
        }
    }

    @Override
    public DatacatNode getObjectInParent(DatacatRecord parent, String name) throws IOException, NoSuchFileException{
        return getDatacatObject(parent, name);
    }

    protected DatacatNode getDatacatObject(DatacatRecord parent, String name) throws IOException, NoSuchFileException{
        try {
            return getChild(parent, name);
        } catch(SQLException ex) {
            throw new IOException("Unknown exception occurred in the database", ex);
        }
    }

    private DatacatNode getChild(DatacatRecord parent, String name) throws SQLException, NoSuchFileException{
        String parentPath = parent != null ? parent.getPath() : null;
        String nameParam = null;

        String childPath = parent != null ? PathUtils.resolve(parent.getPath(), name) : name;
        String parentClause;
        if(parentPath == null || "/".equals(name)){
            parentClause = " is null ";
        } else {
            nameParam = name;
            parentClause = " = ? and name = ?";
        }

        String sql = getChildSql(parentClause);

        DatacatObject.Builder builder = null;
        Long pk = parent != null ? parent.getPk() : null;
        try(PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            if(nameParam != null){
                stmt.setLong(1, pk);
                stmt.setString(2, nameParam);
            }
            ResultSet rs = stmt.executeQuery();
            if(!rs.next()){
                String msg = String.format("Unable to resolve %s in parent %s", childPath, parent);
                throw new NoSuchFileException(msg);
            }
            builder = getBuilder(rs, parentPath);
        }
        completeObject(builder);
        return builder.build();
    }
    
    protected void completeObject(org.srs.datacat.shared.DatacatObject.Builder builder) throws SQLException{
        if(builder instanceof Dataset.Builder){
            completeDataset((Dataset.Builder) builder);
        } else if(builder instanceof DatasetGroup.Builder){
            completeContainer((DatasetGroup.Builder) builder,
                    "select description from DatasetGroup where datasetgroup = ?");
            setContainerMetadata(builder);
        } else if(builder instanceof LogicalFolder.Builder){
            completeContainer((LogicalFolder.Builder) builder,
                    "select description from DatasetLogicalFolder where datasetlogicalfolder = ?");
            setContainerMetadata(builder);
        }
    }

    protected void completeDataset(Dataset.Builder builder) throws SQLException{
        String sql = "select vd.datasetfileformat, "
                + "vd.datasetdatatype, vd.latestversion, "
                + "vd.registered created "
                + "from VerDataset vd "
                + "where vd.dataset = ? ";

        try(PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setLong(1, builder.pk);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            builder.fileFormat(rs.getString("datasetfileformat"))
                    .dataType(rs.getString("datasetdatatype"))
                    .created(rs.getTimestamp("created"));
        }
    }

    protected void completeContainer(DatasetContainerBuilder builder, String sql) throws SQLException{
        try(PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setLong(1, builder.pk);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            builder.description(rs.getString("description"));
        }
    }

    protected void setVersionMetadata(DatasetVersion.Builder builder) throws SQLException{
        String sql = getVersionMetadataSql();
        HashMap<String, Object> metadata = new HashMap<>();
        Long pk = builder.pk;
        try(PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setLong(1, pk);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                processMetadata(rs, metadata);
            }
        }
        if(!metadata.isEmpty()){
            builder.metadata(metadata);
        }
    }

    protected void setContainerMetadata(org.srs.datacat.shared.DatacatObject.Builder builder) throws SQLException{
        String tableType = null;
        Long pk = builder.pk;
        if(builder instanceof LogicalFolder.Builder){
            tableType = "LogicalFolder";
        } else if(builder instanceof DatasetGroup.Builder){
            tableType = "DatasetGroup";
        }
        Map<String, Object> metadata = getMetadata(pk, tableType, tableType);
        if(!metadata.isEmpty()){
            builder.metadata(metadata);
        }
    }
    
    protected Map<String, Object> getMetadata(long pk, String tablePrefix, String column) throws SQLException{
        HashMap<String, Object> metadata = new HashMap<>();
        String mdBase = "select Metaname, Metavalue from %sMeta%s where %s = ?";
        String sql = String.format(mdBase, tablePrefix, "String", column);
        try(PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setLong(1, pk);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                metadata.put(rs.getString("metaname"), rs.getString("metavalue"));
            }
        }

        sql = String.format(mdBase, tablePrefix, "Number", column);
        try(PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setLong(1, pk);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                Number n;
                java.math.BigDecimal v = (java.math.BigDecimal) rs.getBigDecimal("metavalue");
                n = v.scale() == 0 ? v.toBigIntegerExact() : v;
                metadata.put(rs.getString("metaname"), (Number) n);
            }
        }
        sql = String.format(mdBase, tablePrefix, "Timestamp", column);
        try(PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setLong(1, pk);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                Timestamp t = rs.getTimestamp("metavalue");
                metadata.put(rs.getString("metaname"), t);
            }
        }
        return metadata;
    }

    @Override
    public void delete(DatacatRecord record) throws IOException{
        if(record.getType().isContainer()){
            doDeleteDirectory(record);
        } else {
            doDeleteDataset(record);
        }
    }

    protected void doDeleteDirectory(DatacatRecord record) throws DirectoryNotEmptyException, IOException{
        if(!record.getType().isContainer()){
            String msg = "Unable to delete object: Not a Group or Folder" + record.getType();
            throw new IOException(msg);
        }
        SqlContainerDAO dao = new SqlContainerDAO(getConnection());
        // Verify directory is empty
        try(DirectoryStream ds = dao.getChildrenStream(record, Optional.of(DatasetView.EMPTY))) {
            if(ds.iterator().hasNext()){
                AfsException.DIRECTORY_NOT_EMPTY.throwError(record.getPath(), "Container not empty");
            }
        }
        dao.deleteContainer(record);
    }

    protected void doDeleteDataset(DatacatRecord record) throws IOException{
        if(!(record.getType() == RecordType.DATASET)){
            throw new IOException("Can only delete Datacat objects");
        }
        SqlDatasetDAO dao = new SqlDatasetDAO(getConnection());
        dao.deleteDataset(record);
    }
    
    @Override
    public void mergeMetadata(DatacatRecord record, Map<String, Object> metaData) throws IOException{
        try {
            switch(record.getType()){
                case DATASETVERSION:
                    mergeDatasetVersionMetadata(record.getPk(), metaData);
                    break;
                case GROUP:
                    mergeGroupMetadata(record.getPk(), metaData);
                    break;
                case FOLDER:
                    mergeFolderMetadata(record.getPk(), metaData);
                    break;
                default:
                    String msg = "Unable to add metadata to object type: " + record.getType();
                    throw new IOException(msg);
            }
        } catch(SQLException ex) {
            throw new IOException("Unable to add metadata to object", ex);
        }
    }
    
    protected void addDatasetVersionMetadata(Long pk, Map<String, Object> metaData) throws SQLException{
        addDatacatObjectMetadata(pk, metaData, "VerDataset", "DatasetVersion");
    }

    protected void addGroupMetadata(long datasetGroupPK, Map<String, Object> metaData) throws SQLException{
        addDatacatObjectMetadata(datasetGroupPK, metaData, "DatasetGroup", "DatasetGroup");
    }

    protected void addFolderMetadata(long logicalFolderPK, Map<String, Object> metaData) throws SQLException{
        addDatacatObjectMetadata(logicalFolderPK, metaData, "LogicalFolder", "LogicalFolder");
    }
    
    protected void mergeDatasetVersionMetadata(Long pk, Map<String, Object> metaData) throws SQLException{
        mergeDatacatObjectMetadata(pk, metaData, "VerDataset", "DatasetVersion");
    }

    protected void mergeGroupMetadata(long datasetGroupPK, Map<String, Object> metaData) throws SQLException{
        mergeDatacatObjectMetadata(datasetGroupPK, metaData, "DatasetGroup", "DatasetGroup");
    }

    protected void mergeFolderMetadata(long logicalFolderPK, Map<String, Object> metaData) throws SQLException{
        mergeDatacatObjectMetadata(logicalFolderPK, metaData, "LogicalFolder", "LogicalFolder");
    }
    
    protected void deleteDatasetVersionMetadata(Long pk, Set<String> metaDataKeys) throws SQLException{
        deleteDatacatObjectMetadata(pk, metaDataKeys, "VerDataset", "DatasetVersion");
    }

    protected void deleteGroupMetadata(long datasetGroupPK, Set<String> metaDataKeys) throws SQLException{
        deleteDatacatObjectMetadata(datasetGroupPK, metaDataKeys, "DatasetGroup", "DatasetGroup");
    }

    protected void deleteFolderMetadata(long logicalFolderPK, Set<String> metaDataKeys) throws SQLException{
        deleteDatacatObjectMetadata(logicalFolderPK, metaDataKeys, "LogicalFolder", "LogicalFolder");
    }

    private void addDatacatObjectMetadata(long objectPK, Map<String, Object> metaData, String tablePrefix,
            String column) throws SQLException{
        if(metaData == null){
            return;
        }
        if(!(metaData instanceof HashMap)){
            metaData = new HashMap(metaData);
        }
        final String metaSql = "insert into %sMeta%s (%s,MetaName,MetaValue) values (?,?,?)";
        String metaStringSql = String.format(metaSql, tablePrefix, "String", column);
        String metaNumberSql = String.format(metaSql, tablePrefix, "Number", column);
        String metaTimestampSql = String.format(metaSql, tablePrefix, "Timestamp", column);
        PreparedStatement stmtMetaString = null;
        PreparedStatement stmtMetaNumber = null;
        PreparedStatement stmtMetaTimestamp = null;
        PreparedStatement stmt;
        
        try {
            stmtMetaString = getConnection().prepareStatement(metaStringSql);
            stmtMetaNumber = getConnection().prepareStatement(metaNumberSql);
            stmtMetaTimestamp = getConnection().prepareStatement(metaTimestampSql);
            Iterator i = metaData.entrySet().iterator();
            while(i.hasNext()){
                Map.Entry e = (Map.Entry) i.next();
                String metaName = (String) e.getKey();
                Object metaValue = e.getValue();
                
                // Determine MetaData Object type and insert it into the appropriate table:
                if(metaValue instanceof Timestamp){
                    stmt = stmtMetaTimestamp;
                    stmt.setTimestamp(3, (Timestamp) metaValue);
                } else if(metaValue instanceof Number){
                    stmt = stmtMetaNumber;
                    stmt.setObject(3, metaValue);
                } else { // all others stored as String
                    stmt = stmtMetaString;
                    stmt.setString(3, metaValue.toString());
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
    
    private void mergeDatacatObjectMetadata(long objectPK, Map<String, Object> metaData, String tablePrefix,
            String column) throws SQLException{
        Map<String, Object> insertMetaData = new HashMap<>();
        Set<String> deleteMetadata = new HashSet<>();
        if(metaData == null){
            return;
        }
        if(!(metaData instanceof HashMap)){
            metaData = new HashMap(metaData);
        }
        final String metaSql = "UPDATE %sMeta%s SET MetaValue = ? WHERE MetaName= ? and %s = ?";
        String metaStringSql = String.format(metaSql, tablePrefix, "String", column);
        String metaNumberSql = String.format(metaSql, tablePrefix, "Number", column);
        String metaTimestampSql = String.format(metaSql, tablePrefix, "Timestamp", column);
        PreparedStatement stmtMetaString = null;
        PreparedStatement stmtMetaNumber = null;
        PreparedStatement stmtMetaTimestamp = null;
        PreparedStatement stmt;
        
        try {
            stmtMetaString = getConnection().prepareStatement(metaStringSql);
            stmtMetaNumber = getConnection().prepareStatement(metaNumberSql);
            stmtMetaTimestamp = getConnection().prepareStatement(metaTimestampSql);
            Iterator i = metaData.entrySet().iterator();
            while(i.hasNext()){
                Map.Entry e = (Map.Entry) i.next();
                String metaName = (String) e.getKey();
                Object metaValue = e.getValue();
                if(metaValue == null){
                    deleteMetadata.add(metaName);
                    continue;
                }

                // Determine MetaData Object type and insert it into the appropriate table:
                if(metaValue instanceof Timestamp){
                    stmt = stmtMetaTimestamp;
                    stmt.setTimestamp(1, (Timestamp) metaValue);
                } else if(metaValue instanceof Number){
                    stmt = stmtMetaNumber;
                    stmt.setObject(1, metaValue);
                } else { // all others stored as String
                    stmt = stmtMetaString;
                    stmt.setString(1, metaValue.toString());
                }

                stmt.setLong(3, objectPK);
                stmt.setString(2, metaName);
                int result = stmt.executeUpdate();
                if(result == 0){
                    insertMetaData.put(metaName, metaValue);
                }
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
        addDatacatObjectMetadata(objectPK, insertMetaData, tablePrefix, column);
        deleteDatacatObjectMetadata(objectPK, deleteMetadata, tablePrefix, column);
    }
    
    private void deleteDatacatObjectMetadata(long objectPK, Set<String> metaDataKeys, String tablePrefix,
            String column) throws SQLException{
        if(metaDataKeys == null){
            return;
        }
        final String metaSql = "DELETE FROM %sMeta%s WHERE MetaName= ? and %s = ?";
        String metaStringSql = String.format(metaSql, tablePrefix, "String", column);
        String metaNumberSql = String.format(metaSql, tablePrefix, "Number", column);
        String metaTimestampSql = String.format(metaSql, tablePrefix, "Timestamp", column);
        PreparedStatement stmtMetaString = null;
        PreparedStatement stmtMetaNumber = null;
        PreparedStatement stmtMetaTimestamp = null;
        PreparedStatement stmt;
        
        try {
            stmtMetaString = getConnection().prepareStatement(metaStringSql);
            stmtMetaNumber = getConnection().prepareStatement(metaNumberSql);
            stmtMetaTimestamp = getConnection().prepareStatement(metaTimestampSql);
            Iterator<String> i = metaDataKeys.iterator();

            Map<String, Object> existingMetadata = getMetadata(objectPK, tablePrefix, column);
            while(i.hasNext()){
                String metaName = i.next();
                Object metaValue = existingMetadata.get(metaName);

                // Determine MetaData Object type and insert it into the appropriate table:
                if(metaValue instanceof Timestamp){
                    stmt = stmtMetaTimestamp;
                } else if(metaValue instanceof Number){
                    stmt = stmtMetaNumber;
                } else { // all others stored as String
                    stmt = stmtMetaString;
                }

                stmt.setString(1, metaName);
                stmt.setLong(2, objectPK);
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

    protected static RecordType getType(String typeChar){
        switch(typeChar){
            case "F":
                return RecordType.FOLDER;
            case "G":
                return RecordType.GROUP;
            case "D":
                return RecordType.DATASET;
            default:
                return null;
        }
    }

    protected static DatacatObject.Builder getBuilder(ResultSet rs, String parentPath) throws SQLException{
        RecordType type = getType(rs.getString("type"));
        DatacatObject.Builder o;
        switch(type){
            case DATASET:
                o = new Dataset.Builder();
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
        String name = rs.getString("name");
        o.pk(rs.getLong("pk"))
                .parentPk(rs.getLong("parent"))
                .name(name)
                .acl(rs.getString("acl"));
        if(parentPath != null && !parentPath.isEmpty()){
            o.path(PathUtils.resolve(parentPath, name));
        } else {
            o.path("/");
        }
        return o;
    }

    protected static void processMetadata(ResultSet rs, HashMap<String, Object> metadata) throws SQLException{
        String mdType = rs.getString("mdtype");
        if(mdType == null){
            return;
        }
        switch(rs.getString("mdtype")){
            case "N":
                Number n;
                java.math.BigDecimal v = rs.getBigDecimal("metanumber");
                n = v.scale() == 0 ? v.toBigIntegerExact() : v;
                metadata.put(rs.getString("metaname"), (Number) n);
                return;
            case "S":
                metadata.put(rs.getString("metaname"), rs.getString("metastring"));
                return;
            case "T":
                metadata.put(rs.getString("metaname"), rs.getTimestamp("metatimestamp"));
            default:
        }
    }

    protected static void processLocation(ResultSet rs, Long versionPk, 
            List<DatasetLocationModel> locations) throws SQLException{
        DatasetLocation.Builder builder = new DatasetLocation.Builder();
        builder.pk(rs.getLong("datasetlocation"));
        builder.parentPk(versionPk);
        builder.site(rs.getString("datasetsite"));
        builder.resource(rs.getString("path"));
        builder.runMin(rs.getLong("runmin"));
        builder.runMax(rs.getLong("runmax"));
        builder.eventCount(rs.getLong("numberevents"));
        builder.size(rs.getLong("filesizebytes"));
        BigDecimal bd = rs.getBigDecimal("checksum");
        if(bd != null){
            builder.checksum(bd.unscaledValue().toString(16));
        }
        builder.modified(rs.getTimestamp("lastmodified"));
        builder.scanned(rs.getTimestamp("lastscanned"));
        builder.scanStatus(rs.getString("scanstatus"));
        builder.created(rs.getTimestamp("registered"));
        builder.master(rs.getBoolean("isMaster"));
        locations.add(builder.build());
    }

    @Override
    public <T extends DatacatNode> T createNode(DatacatRecord parent, String path,
            T request) throws IOException, FileSystemException{
        if(request instanceof Dataset){
            SqlDatasetDAO dao = new SqlDatasetDAO(getConnection());
            return (T) dao.createDatasetNode(parent, path, (Dataset) request);
        }
        // It should be a container
        SqlContainerDAO dao = new SqlContainerDAO(getConnection());
        return (T) dao.createContainer(parent, path, request);
    }

    @Override
    public void setAcl(DatacatRecord record, String acl) throws IOException{
        try {
            setAclInternal(record, acl);
        } catch (SQLException ex){
            throw new IOException(ex);
        }
    }
    
    private void setAclInternal(DatacatRecord record, String acl) throws SQLException {
        String sql = "UPDATE %s SET ACL = ? WHERE %s = ?";
        String tableType = "DatasetLogicalFolder";
        if(record instanceof DatasetGroup.Builder){
            tableType = "DatasetGroup";
        }
        sql = String.format(sql, tableType, tableType);
        try(PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, acl);
            stmt.setLong(2, record.getPk());
            stmt.executeUpdate();
        }
    }

    protected enum VersionParent {
        DATASET,
        CONTAINER;
    }

    protected String getVersionsSql(VersionParent condition, DatasetView view){
        String queryCondition = "";
        switch(condition){
            case DATASET:
                queryCondition = "vd.dataset = ? ";
                break;
            case CONTAINER:
                queryCondition = "vd.parent = ? ";
                break;
            default:
                break;
        }

        String datasetSqlWithMetadata = 
            "WITH Dataset (dataset, parent, name, latestversion) as ("
            + "  SELECT ds.dataset, CASE WHEN ds.datasetlogicalfolder is not null "
            + "      THEN ds.datasetlogicalfolder else ds.datasetgroup END parent, "
            + "      ds.datasetname name, ds.latestversion "
            + "  FROM VerDataset ds"
            + "), "
            + "DatasetVersions (dataset, datasetversion, versionid, datasetsource, islatest) AS ( "
            + "  select vd.dataset, dsv.datasetversion, dsv.versionid, dsv.datasetsource, "
            + "        CASE WHEN vd.latestversion = dsv.datasetversion THEN 1 ELSE 0 END isLatest "
            + "        FROM Dataset vd "
            + "        JOIN DatasetVersion dsv on (vd.latestversion = dsv.datasetversion) "
            + "        WHERE " + queryCondition
            + "            and " + versionString(view)
            + "       ORDER BY vd.name, dsv.versionid desc "
            + ") "
            + "SELECT dsv.dataset, dsv.datasetversion, dsv.versionid, dsv.datasetsource, dsv.islatest,  "
            + "     md.mdtype, md.metaname, md.metastring, md.metanumber, md.metatimestamp "
            + "FROM DatasetVersions dsv "
            + " JOIN "
            + " ( SELECT mn.datasetversion, 'N' mdtype, mn.metaname, "
            + "         null metastring, mn.metavalue metanumber, null metatimestamp   "
            + "     FROM VerDatasetMetaNumber mn "
            + "   UNION ALL  "
            + "   SELECT ms.datasetversion, 'S' mdtype, ms.metaname, "
            + "         ms.metavalue metastring, null metanumber, null metatimestamp   "
            + "     FROM VerDatasetMetaString ms "
            + "   UNION ALL  "
            + "   SELECT mt.datasetversion, 'T' mdtype, mt.metaname, "
            + "         null metastring, null metanumber, mt.metavalue metatimestamp   "
            + "     FROM VerDatasetMetaTimestamp mt "
            + "  ) md on (md.datasetversion = dsv.datasetversion)";
        return datasetSqlWithMetadata;
    }

    protected String getLocationsSql(VersionParent condition, DatasetView view){
        String queryCondition = "";
        switch(condition){
            case DATASET:
                queryCondition = "vd.dataset = ? ";
                break;
            case CONTAINER:
                queryCondition = "vd.datasetLogicalFolder = ? ";
                break;
            default:
                break;
        }
        String datasetSqlLocations
            = "WITH Dataset (dataset, parent, name, latestversion) as ("
            + "  SELECT ds.dataset, CASE WHEN ds.datasetlogicalfolder is not null "
            + "      THEN ds.datasetlogicalfolder else ds.datasetgroup END parent, "
            + "      ds.datasetname name, ds.latestversion "
            + "  FROM VerDataset ds "
            + ")"
            + "select vd.dataset, dsv.datasetversion,  "
            + "    vdl.datasetlocation, vdl.datasetsite, vdl.path, vdl.runmin, vdl.runmax,   "
            + "    vdl.numberevents, vdl.filesizebytes, vdl.checksum, vdl.lastmodified,   "
            + "    vdl.lastscanned, vdl.scanstatus, vdl.registered,   "
            + "    CASE WHEN dsv.masterlocation = vdl.datasetlocation THEN 1 ELSE 0 END isMaster   "
            + "  FROM Dataset vd   "
            + "  JOIN DatasetVersion dsv on (vd.latestversion = dsv.datasetversion)   "
            + "  JOIN VerDatasetLocation vdl on (dsv.datasetversion = vdl.datasetversion)  "
            + "  WHERE " + queryCondition
            + "            and " + versionString(view)
            + "  ORDER BY vd.name, dsv.versionid desc, vdl.registered";
        return datasetSqlLocations;
    }
    
    protected String getChildSql(String parentClause){
        String sql = String.format("WITH OBJECTS (type, pk, name, parent, acl) AS ( "
                + "    SELECT 'F', datasetlogicalfolder, name, parent, acl "
                + "      FROM DatasetLogicalFolder "
                + "  UNION ALL "
                + "    SELECT 'G', datasetGroup, name, datasetLogicalFolder, acl "
                + "      FROM DatasetGroup "
                + "  UNION ALL "
                + "    SELECT 'D', dataset, datasetName, "
                + "      CASE WHEN datasetlogicalfolder is not null "
                + "         THEN datasetlogicalfolder else datasetgroup END, acl "
                + "      FROM VerDataset "
                + ") "
                + "SELECT type, pk, name, parent, acl FROM OBJECTS "
                + "  WHERE parent %s "
                + "  ORDER BY name", parentClause);
        return sql;
    }

    protected String versionString(DatasetView view){
        return view.isCurrent() ? " dsv.datasetversion = vd.latestversion " : " dsv.versionid = ? ";
    }
    
    protected String getVersionMetadataSql(){
        String sql= 
                "WITH DSV (dsv) AS ( "
                + "  SELECT ? FROM DUAL "
                + ") "
                + "SELECT type, metaname, metastring, metanumber, metatimestamp FROM  "
                + " ( SELECT 'N' mdtype, mn.metaname, null metastring, mn.metavalue metanumber, null metatimestamp "
                + "     FROM VerDatasetMetaNumber mn where mn.DatasetVersion = (SELECT dsv FROM DSV) "
                + "   UNION ALL "
                + "   SELECT 'S' mdtype, ms.metaname, ms.metavalue metastring, null metanumber, null metatimestamp "
                + "     FROM VerDatasetMetaString ms where ms.DatasetVersion = (SELECT dsv FROM DSV) "
                + "   UNION ALL "
                + "   SELECT 'T' mdtype, mt.metaname, null metastring, null metanumber, mt.metavalue metatimestamp "
                + "     FROM VerDatasetMetaTimestamp mt where mt.DatasetVersion = (SELECT dsv FROM DSV) "
                + "  )";
        return sql;
    }

}
