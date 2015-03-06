
package org.srs.datacat.dao.sql;

import com.google.common.base.Optional;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.model.dataset.DatasetLocationModel;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.dataset.DatasetVersionModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.dataset.DatasetViewInfoModel;
import org.srs.datacat.shared.Patchable;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.DatasetViewInfo;
import org.srs.datacat.model.RecordType;
import static org.srs.datacat.model.DcExceptions.*;
import org.srs.datacat.model.dataset.DatasetOption;
import org.srs.vfs.PathUtils;

/**
 *
 * @author bvan
 */
public class SqlDatasetDAO extends SqlBaseDAO implements org.srs.datacat.dao.DatasetDAO {
    
    private static final String DEFAULT_DATA_SOURCE = "RESTFUL_API_v0.2";

    public SqlDatasetDAO(Connection conn){
        super( conn );
    }

    public SqlDatasetDAO(Connection conn, ReentrantLock lock){
        super(conn, lock);
    }
    
    public Dataset createDatasetNode(DatacatRecord parent, String name, 
            Dataset request) throws IOException, FileSystemException{
        try {
            return insertDataset(parent, name, request);
        } catch (SQLException ex){
            throw new IOException("Unable to insert node: " + PathUtils.resolve(parent.getPath(), name), ex);
        }
    }
    
    @Override
    public Dataset createDataset(DatacatRecord parent, String dsName,
            Optional<DatasetModel> dsReq, Optional<DatasetViewInfoModel> viewInfo, Set options) throws IOException{
        
        Set<DatasetOption> dsOptions = new HashSet<>(options); // make a copy
        DatacatRecord target = null;
        
        if(!options.contains(DatasetOption.SKIP_NODE_CHECK)){
            try {
                target = getObjectInParent(parent, dsName);
            } catch (NoSuchFileException ex){
                target = null; // This is okay
            }
        }
        
        if(dsReq.isPresent()){
            if(target != null){
                if(!dsOptions.remove(DatasetOption.MERGE_NODE)){
                    String pathString = PathUtils.resolve( parent.getPath(), dsName);
                    DATASET_EXISTS.throwError(pathString, "A dataset node already exists at this location");
                }
                patchDataset(target, dsReq.get());
                target = getObjectInParent(parent, dsName);
            } else {
                target = createNode(parent, dsName, (Dataset) dsReq.get());
                // If we added a node, skip version check
                dsOptions.add(DatasetOption.SKIP_VERSION_CHECK); 
            }
        }

        if(target == null || !(target instanceof Dataset)){
            throw new IOException(new IllegalArgumentException("Unable to process request: no Dataset target found"));
        }

        // Target must exist and also be a Dataset, from above
        Dataset.Builder builder = new Dataset.Builder((Dataset) target);
        // One of these conditions must be present to continue on and create a view           
        // We had a flag that denoting we should create a view, so we continue on
        if(viewInfo.isPresent()){
            DatasetViewInfo retView = createOrMergeDatasetView(target, viewInfo.get(), dsOptions);
            builder.version(retView.getVersion());
            if(retView.locationsOpt().isPresent()){
                if(retView.singularLocationOpt().isPresent()){
                    builder.location(retView.singularLocationOpt().get());
                } else if (!retView.getLocations().isEmpty()){
                    builder.locations(retView.getLocations());
                }
            }
        }
        return builder.build();
    }
    
    public void deleteDataset(DatacatRecord dataset) throws IOException {
        try {
            String deleteSql = "delete from VerDataset where Dataset=?";
            delete1(deleteSql, dataset.getPk());
        } catch (SQLException ex){
            throw new IOException("Error deleting dataset: " + dataset.getPath(), ex);
        }
    }
    
    protected DatasetVersion createOrMergeDatasetVersion(DatacatRecord dsRecord, DatasetVersionModel request, 
            Optional<DatasetVersionModel> curVersionOpt, boolean mergeVersion) throws IOException, FileSystemException{
        try {
            int newId = request.getVersionId();
            boolean isCurrent = true;
            // If there exists a version already and we aren't skipping the check...
            if(curVersionOpt.isPresent()){
                DatasetVersionModel currentVersion = curVersionOpt.get();
                int currentId = currentVersion.getVersionId();
                if(mergeVersion){
                    newId = getMergeVersionId(dsRecord.getPath(), currentId, newId);
                    if(newId == currentId){
                        deleteDatasetVersion(dsRecord.getPk(), currentVersion);
                    }
                } else {
                    // Will throw an error if the record exists.
                    newId = getCreationVersionID(dsRecord.getPath(), currentId, newId);
                }
            }
            return insertDatasetVersion(dsRecord, newId, isCurrent, request);
        } catch (SQLException ex){
            throw new IOException("Unable to create or merge version", ex);
        }
    }
    
    protected DatasetLocation createDatasetLocation(DatacatRecord versionRecord, DatasetLocationModel newLoc,   
            boolean skipCheck) throws IOException, FileSystemException{
        try {
            if(!skipCheck){
                assertCanCreateLocation(versionRecord, newLoc);
            }
            return insertDatasetLocation(versionRecord.getPk(), newLoc);
        } catch (SQLException ex){
            throw new IOException("Unable to check and/or insert dataset location", ex);
        }
    }
    
    protected DatasetVersion getCurrentVersion(DatacatRecord dsRecord) throws IOException {
        try {
            for(DatasetVersion v: getDatasetVersions(dsRecord)){
                if(v.isLatest()){
                    return v;
                }
            }
            return null;            
        } catch (SQLException ex){
            throw new IOException("Unable to retrieve current version", ex);
        }
    }

    @Override
    public DatasetViewInfo getDatasetViewInfo(DatacatRecord dsRecord, DatasetView view) throws IOException{
        try {
            return getDatasetViewInfoInternal(dsRecord, view );
        } catch (SQLException ex){
            throw new IOException("Failed to retrieve version", ex);
        }
    }

    private DatasetViewInfo getDatasetViewInfoInternal(DatacatRecord dsRecord, DatasetView view) throws SQLException{
        String sqlWithMetadata = getVersionsSql(VersionParent.DATASET, view );
        String sqlLocations = getLocationsSql(VersionParent.DATASET, view );
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        try {
            stmt1 = getConnection().prepareStatement(sqlWithMetadata);
            stmt2 = getConnection().prepareStatement(sqlLocations);
            stmt1.setLong( 1, dsRecord.getPk());
            stmt2.setLong( 1, dsRecord.getPk());
            if(!view.isCurrent()){
                stmt1.setInt( 2, view.getVersionId());
                stmt2.setInt( 2, view.getVersionId());
            }
            ResultSet rs1 = stmt1.executeQuery();
            ResultSet rs2 = stmt2.executeQuery();
            
            HashMap<String, Object> metadata = new HashMap<>();
            
            DatasetVersion.Builder builder = new DatasetVersion.Builder();
            List<DatasetLocationModel> locations = new ArrayList<>();
            if(rs1.next()){
                builder.pk(rs1.getLong("datasetversion"));
                builder.parentPk(dsRecord.getPk());
                builder.versionId(rs1.getInt("versionid"));
                builder.datasetSource(rs1.getString("datasetSource"));
                builder.latest(rs1.getBoolean("isLatest"));
                builder.path( dsRecord.getPath() + ";v=" + rs1.getInt("versionid"));
                processMetadata( rs1, metadata );
                while(rs1.next()){
                    processMetadata( rs1, metadata );
                }
                while(rs2.next()){
                    processLocation( rs2, builder.pk, locations);
                }
                builder.metadata(metadata);
                return new DatasetViewInfo(builder.build(), locations);
            }
            return null;
        } finally {
            if(stmt1 != null){
                stmt1.close();
            }
            if(stmt2 != null){
                stmt2.close();   
            }
        }
    }
    
    public DatasetViewInfo createOrMergeDatasetView(DatacatRecord dsRecord, DatasetViewInfoModel reqView, 
            Set<DatasetOption> options) throws IOException {        
        Set<DatasetOption> dsOptions = new HashSet<>(options); // make a copy
        boolean mergeVersion = dsOptions.remove(DatasetOption.MERGE_VERSION);
        boolean createVersion = dsOptions.remove(DatasetOption.CREATE_VERSION);
        boolean createLocations = dsOptions.remove(DatasetOption.CREATE_LOCATIONS);
        boolean skipVersionCheck = dsOptions.remove(DatasetOption.SKIP_VERSION_CHECK);
        boolean skipLocationCheck = dsOptions.remove(DatasetOption.SKIP_LOCATION_CHECK);
        List<DatasetLocationModel> retLocations = new ArrayList<>();
        DatasetVersion curVersion = null;
        
        if(createVersion || mergeVersion) {
            if(!reqView.versionOpt().isPresent()){
                throw new IllegalArgumentException("Missing version from request");
            }
            DatasetVersionModel maybeVersion = skipVersionCheck ? null : getCurrentVersion(dsRecord);
            Optional<DatasetVersionModel> versionOpt = Optional.fromNullable(maybeVersion);
            curVersion = createOrMergeDatasetVersion(dsRecord, (DatasetVersion) reqView.getVersion(),
                    versionOpt, mergeVersion);
            skipLocationCheck = true;
        } else {
            /* We didn't create or merge a version, so we're only creating a location.
               If skipVersionCheck, use reqVersion otherwise get current version */
            curVersion = skipVersionCheck ? (DatasetVersion) reqView.getVersion() : getCurrentVersion(dsRecord);
        }

        if(createLocations){
            if(curVersion == null){
                NO_SUCH_VERSION.throwError(dsRecord.getPath(), "No version exists which we can add a location to");
            }
            if(!reqView.locationsOpt().isPresent() || reqView.getLocations().isEmpty()){
                throw new IllegalArgumentException("Unable to create the view specified without locations");
            }
            for(DatasetLocationModel reqLocation: reqView.getLocations()){
                DatasetLocation l = createDatasetLocation(curVersion, reqLocation, skipLocationCheck);
                retLocations.add(l);
            }
        }
        return new DatasetViewInfo(curVersion, retLocations);
    }
    
    private List<DatasetVersion> getDatasetVersions(DatacatRecord dsRecord) throws SQLException{
        String sql = 
                "select dsv.datasetversion, dsv.versionid, dsv.datasetsource, " 
                + "CASE WHEN vd.latestversion = dsv.datasetversion THEN 1 ELSE 0 END isLatest "
                + "FROM verdataset vd "
                + "JOIN datasetversion dsv on (vd.latestversion = dsv.datasetversion) "
                + "WHERE vd.dataset = ? ";

        try (PreparedStatement stmt = getConnection().prepareStatement( sql )){
            stmt.setLong( 1, dsRecord.getPk());
            ResultSet rs = stmt.executeQuery();
            ArrayList<DatasetVersion> versions = new ArrayList<>();
            DatasetVersion.Builder builder;
            while(rs.next()){
                builder = new DatasetVersion.Builder();
                builder.pk(rs.getLong( "datasetversion"));
                builder.parentPk(dsRecord.getPk());
                builder.versionId(rs.getInt( "versionid"));
                builder.datasetSource(rs.getString( "datasetSource"));
                builder.latest(rs.getBoolean( "isLatest"));
                builder.path( dsRecord.getPath() + ";v=" + rs.getInt("versionid"));
                setVersionMetadata( builder );
                versions.add( builder.build());
            }
            return versions;
        }
    }
    
    private List<DatasetLocation> getDatasetLocations(Long versionPk) throws SQLException{
        String sql = 
                "select vdl.datasetlocation, vdl.datasetsite, vdl.path, vdl.runmin, vdl.runmax, "
                + "vdl.numberevents, vdl.filesizebytes, vdl.checksum, vdl.lastmodified, "
                + "vdl.lastscanned, vdl.scanstatus, vdl.registered, "
                + "CASE WHEN dsv.masterlocation = vdl.datasetlocation THEN 1 ELSE 0 END isMaster "
                + "FROM datasetversion dsv "
                + "JOIN verdatasetlocation vdl on (dsv.masterlocation = vdl.datasetlocation) "
                + "WHERE dsv.datasetversion = ? ";
        try (PreparedStatement stmt = getConnection().prepareStatement( sql )){
            stmt.setLong( 1, versionPk );
            ResultSet rs = stmt.executeQuery();
            ArrayList<DatasetLocation> locations = new ArrayList<>();
            DatasetLocation.Builder builder;
            while(rs.next()){
                builder = new DatasetLocation.Builder();
                builder.pk(rs.getLong("datasetlocation"));
                builder.parentPk(versionPk);
                builder.site(rs.getString( "datasetsite"));
                builder.resource(rs.getString( "path"));
                builder.runMin(rs.getLong( "runmin"));
                builder.runMax(rs.getLong( "runmax"));
                builder.eventCount(rs.getLong( "numberevents"));
                builder.size(rs.getLong( "filesizebytes"));
                BigDecimal bd = rs.getBigDecimal("checksum");
                if(bd != null){
                    builder.checksum(bd.unscaledValue().toString(16));
                }
                builder.modified(rs.getTimestamp( "lastmodified"));
                builder.scanned(rs.getTimestamp( "lastscanned"));
                builder.scanStatus( rs.getString( "scanstatus"));
                builder.created(rs.getTimestamp( "registered"));
                builder.master( rs.getBoolean( "isMaster"));
                locations.add( builder.build());
            }
            return locations;
        }        
    }
    
    protected Dataset insertDataset(DatacatRecord parent, String name, Dataset request) throws SQLException {
        Long parentPk = parent.getPk();
        RecordType parentType = parent.getType();
        String insertSql = "insert into VerDataset (DatasetName, DataSetFileFormat, DataSetDataType, "
                + "DatasetLogicalFolder, DatasetGroup) values (?, ?, ?, ?, ?)";
        try(PreparedStatement stmt = getConnection().prepareStatement(insertSql, 
                new String[]{"DATASET", "REGISTERED"})) {
            stmt.setString(1, name);
            stmt.setString(2, request.getFileFormat() );
            stmt.setString(3, request.getDataType().toUpperCase());
            switch(parentType){
                case FOLDER:
                    stmt.setLong(4, parentPk);
                    stmt.setNull(5, java.sql.Types.BIGINT);
                    break;
                case GROUP:
                    stmt.setNull(4, java.sql.Types.BIGINT);
                    stmt.setLong(5, parentPk);
                default:
                    break;
            }
            stmt.executeUpdate();   // will throw exception if required parameter is empty...
            Dataset.Builder builder = new Dataset.Builder(request);
            try(ResultSet rs = stmt.getGeneratedKeys()){
                rs.next();
                builder.pk(rs.getLong(1));
                builder.parentPk(parentPk);
                builder.parentType(parentType);
                builder.path(PathUtils.resolve(parent.getPath(), name));
                builder.created(rs.getTimestamp(2));
            }
            return builder.build();
        }
    }
    
    protected String insertDatasetSource(String source) throws SQLException{
        String sql = "INSERT INTO DatasetSource (DatasetSource) VALUES (?)";
        try(PreparedStatement stmt = getConnection().prepareStatement( sql )){
            stmt.setString(1, source);
            stmt.executeUpdate();
            return source;
        }
    }
    
    protected String insertDatasetDataType(String dataType, String description, Integer priority) throws SQLException{
        String sql = "INSERT INTO DatasetDataType (DatasetDataType, Description, CrawlerPriority) "
                + "VALUES (?,?,?)";
        try(PreparedStatement stmt = getConnection().prepareStatement( sql )){
            stmt.setString(1, dataType.toUpperCase());
            stmt.setString(2, description);
            if(priority != null) {
                stmt.setInt(3, priority);
            } else {
                stmt.setNull(3, java.sql.Types.INTEGER);
            }
            stmt.executeUpdate();
            return dataType.toUpperCase();
        }
    }
    
    protected String insertDatasetFileFormat(String fileFormat, String description, 
            String mimeType) throws SQLException{
        String sql = "INSERT INTO DatasetFileFormat (DatasetFileFormat, Description, MimeType) "
                + "VALUES (?,?,?)";
        try(PreparedStatement stmt = getConnection().prepareStatement( sql )){
            stmt.setString(1, fileFormat.toLowerCase());
            stmt.setString(2, description);
            if(mimeType != null) {
                stmt.setString(3, mimeType);
            } else {
                stmt.setNull(3, java.sql.Types.VARCHAR);
            }
            stmt.executeUpdate();
            return fileFormat.toLowerCase();
        }
    }
    
    protected void deleteDatasetDataType(String dataType) throws SQLException {
        String deleteSql = "DELETE FROM DatasetDataType where DatasetDataType=?";
        delete1(deleteSql.toUpperCase(), dataType);
    }
    
    protected void deleteDatasetFileFormat(String fileFormat) throws SQLException {
        String deleteSql = "DELETE FROM DatasetDataType where DatasetDataType=?";
        delete1(deleteSql.toLowerCase(), fileFormat);
    }
    
    /*protected void deleteAllDatasetViews(long datasetPk) throws SQLException{
        for(DatasetVersion v: getDatasetVersions( datasetPk )){
            deleteDatasetVersion(datasetPk, v);
        }
    }
    
    protected void deleteDataset(long datasetPk) throws SQLException{
        String deleteSql = "delete from VerDataset where Dataset=?";
        delete1(deleteSql, datasetPk);
    }*/
    
    protected DatasetVersion insertDatasetVersion(DatacatRecord dsRecord, int newVersionId, 
            boolean isCurrent, DatasetVersionModel request) throws SQLException {
        // One last integrity check
        newVersionId = newVersionId < 0 ? 0 : newVersionId;
        
        String sql = 
                "insert into DatasetVersion "
                + "(Dataset, VersionID, DataSetSource, ProcessInstance, TaskName) "
                + "values (?, ?, ?, ?, ?)";
        String datasetSource = request.getDatasetSource() != null ? request.getDatasetSource() : DEFAULT_DATA_SOURCE;
        DatasetVersion retVersion = null;
        try(PreparedStatement stmt = getConnection().prepareStatement(sql, 
                new String[]{"DATASETVERSION", "REGISTERED"})) {
            stmt.setLong(1, dsRecord.getPk());
            stmt.setInt(2, newVersionId );
            stmt.setString(3, datasetSource);
            if(((DatasetVersion) request).getProcessInstance() != null){
                stmt.setLong(4, ((DatasetVersion) request).getProcessInstance());
            } else {
                stmt.setNull(4, java.sql.Types.BIGINT);
            }
            stmt.setString(5, ((DatasetVersion) request).getTaskName());
            stmt.executeUpdate();   // will throw exception if required parameter is empty...
            
            DatasetVersion.Builder builder = new DatasetVersion.Builder(request);
            builder.parentPk(dsRecord.getPk());
            builder.versionId(newVersionId);
            builder.latest(isCurrent);
            builder.path(dsRecord.getPath() + ";v=" + newVersionId);
            try(ResultSet rs = stmt.getGeneratedKeys()){
                rs.next();
                builder.pk(rs.getLong(1));
                builder.parentPk(dsRecord.getPk());
                builder.created(rs.getTimestamp(2));
                builder.metadata(request.getMetadataMap());
            }
            retVersion = builder.build();
        }
        if(request.getMetadataMap() != null && !request.getMetadataMap().isEmpty()){
            addDatasetVersionMetadata(retVersion.getPk(), request.getMetadataMap());
        }
        // Update isLatest
        if(retVersion.isLatest()){
            sql = "UPDATE VerDataset set LatestVersion = ? WHERE Dataset = ?";
            try(PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                stmt.setLong(1, retVersion.getPk());
                stmt.setLong(2, dsRecord.getPk());
                stmt.executeUpdate();
            }
        }
        return retVersion;
    }
    
    protected void deleteDatasetVersion(long datasetPk, DatasetVersionModel version) throws SQLException{
        if(version.isLatest()){
            // Will set to NULL if there is no other dataset version
            String nextLatest = 
                "UPDATE VerDataset m "
                + "   SET m.LatestVersion =  "
                + "     (SELECT dsv.DatasetVersion "
                + "       FROM DatasetVersion dsv "
                + "       WHERE dsv.Dataset=m.Dataset "
                + "       AND dsv.VersionId = "
                + "         (SELECT Max(VersionId) "
                + "           FROM DatasetVersion WHERE Dataset=m.DataSet "
                + "           AND VersionId != ?) "
                + "      ) "
                + "   WHERE m.Dataset = ?";
            
            try(PreparedStatement stmt = getConnection().prepareStatement( nextLatest )) {
                stmt.setInt( 1, version.getVersionId() );
                stmt.setLong( 2, datasetPk );
                stmt.executeUpdate();
            }            
        }

        String deleteSql = "delete from DatasetVersion where DatasetVersion=?";
        delete1(deleteSql, version.getPk());
    }

    protected DatasetLocation insertDatasetLocation(Long datasetVersionPk,
            DatasetLocationModel request) throws SQLException{
        String insertSql = 
              "insert into VerDataSetLocation (DatasetVersion, DatasetSite, Path, RunMin, RunMax, "
              + " NumberEvents, FileSizeBytes) values (?, ?, ?, ?, ?, ?, ?)";
        
        int i = 0;
        DatasetLocation retLoc;
        boolean isMaster = getDatasetLocations(datasetVersionPk).isEmpty();
        try(PreparedStatement stmt = getConnection().prepareStatement(insertSql, 
                new String[]{"DATASETLOCATION", "REGISTERED"})) {
            stmt.setLong(++i, datasetVersionPk );
            stmt.setString(++i, request.getSite() );
            stmt.setString(++i, request.getResource() );
            stmt.setObject(++i, ((DatasetLocation) request).getRunMin() );
            stmt.setObject(++i, ((DatasetLocation) request).getRunMax() );
            stmt.setObject(++i, ((DatasetLocation) request).getEventCount() );
            stmt.setObject(++i, request.getSize() );
            stmt.executeUpdate();   // will throw exception if required parameter is empty...
            DatasetLocation.Builder builder = new DatasetLocation.Builder(((DatasetLocation) request));
            // now retrieve the primary key:

            try(ResultSet rs = stmt.getGeneratedKeys()){
                rs.next();
                builder.pk(rs.getLong(1));
                builder.parentPk(datasetVersionPk);
                builder.created(rs.getTimestamp(2));
                builder.master(isMaster);
            }
            retLoc = builder.build();
        }
        // If this is to be the master
        if(retLoc.isMaster()){
            String sql = "UPDATE DatasetVersion set MasterLocation = ? WHERE DatasetVersion = ?";
            try(PreparedStatement stmt = getConnection().prepareStatement( sql )) {
                stmt.setLong(1, retLoc.getPk());
                stmt.setLong(2, datasetVersionPk );
                stmt.executeUpdate();
            }
        }
        return retLoc;
    }
    
    protected void deleteDatasetLocation(long datasetVersionPk, DatasetLocation location) throws SQLException{
        if(location.isMaster()){
            // Will set to NULL if there is no other location
            String nextLatest = 
                "UPDATE DatasetVersion m "
                + "   SET m.MasterLocation =  "
                + "     (SELECT dsl.DatasetLocation "
                + "       FROM VerDatasetLocation dsl "
                + "       WHERE dsl.DatasetVersion=m.DatasetVersion "
                + "       AND dsl.Registered = "
                + "         (SELECT Min(Registered) "
                + "           FROM VerDatasetLocation WHERE DatasetVersion=m.DatasetVersion "
                + "           AND Site != ?) "
                + "      ) "
                + "   WHERE m.DatasetVersion = ?";
            try(PreparedStatement stmt = getConnection().prepareStatement( nextLatest )) {
                stmt.setString( 1, location.getSite() );
                stmt.setLong( 2, datasetVersionPk );
                stmt.executeUpdate();
            }
        }
        
        String deleteSql = "delete from DatasetLocation where DatasetLocation=?";
        delete1(deleteSql, location.getPk() );
    }

    protected int getCreationVersionID(String dsPath, int currentId, int newId) throws FileSystemException {
        switch(newId){
            case DatasetView.NEW_VER:
                return currentId+1;
            case DatasetView.CURRENT_VER:
                VERSION_EXISTS.throwError(dsPath, String.format("Version %d already exists", currentId));
            default:
                if(currentId >= newId){
                    String msg = String.format("Version %d not greater than current version %d", newId, currentId);
                    NEWER_VERSION_EXISTS.throwError(dsPath, msg);
                }
        }
        return newId;
    }
    
    /**
     * Check if we can merge these items.
     * 
     * @param dsPath
     * @param currentId
     * @param newId
     * @return true if we are replacing the current version, false if we are creating a new one
     * @throws FileSystemException 
     */
    protected int getMergeVersionId(String dsPath, int currentId, int newId) throws FileSystemException {
        if(currentId == newId){
            return newId;
        }
        switch(newId){
            case DatasetView.CURRENT_VER:
                return currentId;
            case DatasetView.NEW_VER:
                return currentId + 1;
            default:
                return getCreationVersionID(dsPath, currentId, newId);
        }
    }
    
    protected void assertCanCreateLocation(DatacatRecord versionRecord, 
            DatasetLocationModel newLoc) throws SQLException, FileSystemException{
        for(DatasetLocation l: getDatasetLocations(versionRecord.getPk())){
            if(l.getSite().equals(newLoc.getSite())){
                String msg = "Location entry for site " + newLoc.getSite() + " already exists";
                LOCATION_EXISTS.throwError(versionRecord.getPath(), msg);
            }
        }
    }

    @Override
    public void patchDataset(DatacatRecord dataset, DatasetView view, Optional<DatasetModel> dsReq,
            Optional<DatasetViewInfoModel> viewInfo) throws IOException{
        patchDatasetInternal(dataset, view, dsReq, viewInfo);
    }
    
    protected void patchDatasetInternal(DatacatRecord dataset, DatasetView view,
            Optional<DatasetModel> dsReq,
            Optional<DatasetViewInfoModel> viewInfo) throws IOException{

        if(dsReq.isPresent()){
            patchDataset(dataset, dsReq.get());
        }

        if(viewInfo.isPresent()){
            DatasetViewInfoModel requestView = viewInfo.get();
            DatasetViewInfo currentView = getDatasetViewInfo(dataset, view);
            if(requestView.versionOpt().isPresent()){
                patchDatasetVersion(currentView.getVersion(), requestView.getVersion());
            }
            if(requestView.locationsOpt().isPresent()){
                if(!currentView.versionOpt().isPresent()){
                    throw new IOException("Unable to patch a non-existent version. Create a version first.");
                }
                patchDatasetLocation(currentView.singularLocationOpt().get(),
                        requestView.singularLocationOpt().get());
            }
        }
    }
    private void patchDataset(DatacatRecord existing, DatasetModel patch) throws IOException {
        try {
            for(Method method: patch.getClass().getMethods()){
                if(method.isAnnotationPresent(Patchable.class)){
                    Object patchedValue = method.invoke(patch);
                    if(patchedValue == null){
                        continue;
                    }
                    if(patchedValue instanceof Map && ((Map) patchedValue).isEmpty()){
                        continue;
                    }

                    String methodName = method.getName();
                    String sql;
                    switch(methodName){
                        case "getAcl":
                            sql = "UPDATE VerDataset SET ACL=? WHERE Dataset = ?";
                            break;
                        default:
                            throw new IOException("No Implementation to patch field " + methodName);
                    }
                    try(PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                        stmt.setObject(1, patchedValue);
                        stmt.setLong(2, existing.getPk());
                        stmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException ex){
            throw new IOException("Unable to perform patch", ex);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            throw new IOException("FATAL error in defined model", ex);
        }
    }
    
    private void patchDatasetVersion(DatacatRecord existing, DatasetVersionModel patch) throws IOException {
        try {
            for(Method method: existing.getClass().getMethods()){
                if(method.isAnnotationPresent(Patchable.class)){
                    Object patchedValue = method.invoke(patch);
                    if(patchedValue == null){
                        continue;
                    }
                    if(patchedValue instanceof Map && ((Map) patchedValue).isEmpty()){
                        continue;
                    }

                    String methodName = method.getName();
                    if("getMetadataMap".equals(methodName)){
                        mergeDatasetVersionMetadata(existing.getPk(), (Map) patchedValue);
                        continue;
                    }

                    String baseSql = "UPDATE DatasetVersion SET %s=? WHERE DatasetVersion = ?";
                    Patchable p = method.getAnnotation(Patchable.class);
                    String sql = String.format(baseSql, p.column());

                    try(PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                        stmt.setObject(1, patchedValue);
                        stmt.setLong(2, existing.getPk());
                        stmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException ex){
            throw new IOException("Unable to perform patch", ex);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            throw new IOException("FATAL error in defined model", ex);
        }
    }
    
    private void patchDatasetLocation(DatacatRecord existing, DatasetLocationModel patch) throws IOException {
        try {
            for(Method method: existing.getClass().getMethods()){
                if(method.isAnnotationPresent(Patchable.class)){
                    Object patchedValue = method.invoke(patch);
                    if(patchedValue == null){
                        continue;
                    }
                    if(patchedValue instanceof Map && ((Map) patchedValue).isEmpty()){
                        continue;
                    }

                    String methodName = method.getName();
                    if("getMetadataMap".equals(methodName)){
                        throw new IOException("Metadata on DatasetLocation not patchable");
                    }
                    String baseSql = "UPDATE VerDatasetLocation SET %s=? WHERE DatasetLocation = ?";
                    Patchable p = method.getAnnotation(Patchable.class);
                    String sql = String.format(baseSql, p.column());
                    if("checksum".equalsIgnoreCase(p.column())){
                        patchedValue = new BigInteger(patchedValue.toString(), 16);
                    }
                    try(PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                        stmt.setObject(1, patchedValue);
                        stmt.setLong(2, existing.getPk());
                        stmt.executeUpdate();
                    }

                }
            }
        } catch (SQLException ex){
            throw new IOException("Unable to perform patch", ex);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            throw new IOException("FATAL error in defined model", ex);
        }
    }
    
    
}
