
package org.srs.datacat.sql;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.srs.vfs.AbstractFsProvider;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.dataset.DatasetBuilder;
import org.srs.datacat.shared.dataset.FlatDataset;
import org.srs.datacat.vfs.DcFileSystemProvider;
import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.attribute.DatasetOption;
import static org.srs.datacat.vfs.DcFileSystemProvider.DcFsException.*;

/**
 *
 * @author bvan
 */
public class DatasetDAO extends BaseDAO {
    
    public DatasetDAO(Connection conn){
        super( conn );
    }
    
    public Dataset createDatasetNodeAndView(Long parentPk, DatacatObject.Type parentType, DcPath path, Dataset dsReq, 
            Set<DatasetOption> dsOptions) throws IOException, SQLException{
        
        String pathString = path.toString();
        boolean createNode = dsOptions.remove(DatasetOption.CREATE_NODE);
        if(dsReq == null){
            throw new IOException("Not enough information to create create a Dataset node or view");
        }
        
        Dataset ds = null;

        if(createNode){
            ds = createDatasetNode(parentPk, parentType, path, dsReq);
        }

        if(ds == null){
            DatacatObject o = getDatacatObject(parentPk, pathString);
            if(!(o instanceof Dataset)){
                AbstractFsProvider.AfsException.NO_SUCH_FILE.throwError( pathString, "Target is not a dataset");
            }
            ds = (Dataset) o;
        }
        
        DatasetBuilder builder = DatasetBuilder.create(ds);
        
        if(!dsOptions.isEmpty()){ 
            // We should have enought information to continue on
            if(dsReq instanceof FlatDataset){
                DatasetVersion requestVersion = ((FlatDataset) dsReq).getVersion();
                DatasetLocation requestLocation = ((FlatDataset) dsReq).getLocation();
                createDatasetView( ds, builder, requestVersion, requestLocation, dsOptions );
            } else {
                throw new IOException("Unable to create dataset, not enough information");
            }
        }
        return builder.build();
    }
    
    public void createDatasetView(Dataset ds, DatasetBuilder builder, DatasetVersion requestVersion, DatasetLocation requestLocation, 
            Set<DatasetOption> dsOptions) throws IOException, SQLException{        
        boolean mergeVersion = dsOptions.remove(DatasetOption.MERGE_VERSION);
        boolean createVersion = dsOptions.remove(DatasetOption.CREATE_VERSION);
        boolean createLocation = dsOptions.remove(DatasetOption.CREATE_LOCATION);
        
        String path = ds.getPath();
        DatasetVersion currentVersion = getCurrentVersion(ds.getPk());
        
        if(createVersion || mergeVersion) {
            currentVersion = createOrMergeDatasetVersion(ds.getPk(), path, currentVersion, requestVersion, mergeVersion);
            builder.version(currentVersion);
        }
        
        if(createLocation){
            if(currentVersion == null){
                DcFileSystemProvider.DcFsException.NO_SUCH_VERSION.throwError(path, "No version exists which we can add a location to");
            }
            builder.location(createDatasetLocation(currentVersion, path, requestLocation));
        }
    }
    
    public Dataset createDatasetNode(Long parentPk, DatacatObject.Type parentType, DcPath path, Dataset request) throws SQLException, FileSystemException{
        return insertDataset( parentPk, parentType, path.toString(), request );
    }

    public DatasetVersion createOrMergeDatasetVersion(Long datasetPk, DcPath path, DatasetVersion newVer,
            boolean mergeVersion) throws SQLException, FileSystemException {
        return createOrMergeDatasetVersion(datasetPk, path, getCurrentVersion(datasetPk), newVer, mergeVersion);
    }

    public DatasetVersion createOrMergeDatasetVersion(Long datasetPk, DcPath path, DatasetVersion currentVersion, 
            DatasetVersion request, boolean mergeVersion) throws SQLException, FileSystemException{
        return createOrMergeDatasetVersion( datasetPk, path.toString(), currentVersion, request, mergeVersion );
    }
    
    protected DatasetVersion createOrMergeDatasetVersion(Long datasetPk, String dsPath, DatasetVersion currentVersion, 
            DatasetVersion request, boolean mergeVersion) throws SQLException, FileSystemException{
        // If there is a current version, we should see if we can merge or create a new version.
        int newId = request.getVersionId();
        boolean isCurrent = true;
        if(currentVersion != null){
            int currentId = currentVersion.getVersionId();
            if(mergeVersion){
                newId = getMergeVersionId(dsPath, currentId, newId);
                if(newId == currentId){
                    deleteDatasetVersion(datasetPk, currentVersion);
                }
            } else {
                // Will throw an error if the record exists.
                newId = getCreationVersionID(dsPath, currentId, newId);
            }
        }
        DatasetVersion retVer = insertDatasetVersion(datasetPk, newId, isCurrent, request);
        return retVer;
    }
    
    public DatasetLocation createDatasetLocation(DatasetVersion version, DcPath path, DatasetLocation newLoc) throws SQLException, FileSystemException{
        return createDatasetLocation(version, path.toString(), newLoc);
    }
    
    public DatasetLocation createDatasetLocation(DatasetVersion version, String path, DatasetLocation newLoc) throws SQLException, FileSystemException{
        assertCanCreateLocation(version.getPk(), path, newLoc);
        return insertDatasetLocation(version.getPk(), newLoc);
    }
    
    public DatasetVersion getCurrentVersion(Long datasetPk) throws SQLException {
        for(DatasetVersion v: getDatasetVersions(datasetPk)){
            if(v.isLatest()){
                return v;
            }
        }
        return null;
    }
    
    public List<DatasetVersion> getDatasetVersions(Long datasetPk) throws SQLException{
        String sql = 
                "select dsv.datasetversion, dsv.versionid, dsv.datasetsource, " 
                + "CASE WHEN vd.latestversion = dsv.datasetversion THEN 1 ELSE 0 END isLatest "
                + "FROM verdataset vd "
                + "JOIN datasetversion dsv on (vd.latestversion = dsv.datasetversion) "
                + "JOIN verdatasetlocation vdl on (dsv.masterlocation = vdl.datasetlocation) "
                + "WHERE vd.dataset = ? ";

        try (PreparedStatement stmt = getConnection().prepareStatement( sql )){
            stmt.setLong( 1, datasetPk);
            ResultSet rs = stmt.executeQuery();
            ArrayList<DatasetVersion> versions = new ArrayList<>();
            DatasetVersion.Builder builder;
            while(rs.next()){
                builder = new DatasetVersion.Builder();
                builder.pk(rs.getLong( "datasetversion"));
                builder.parentPk(datasetPk);
                builder.versionId(rs.getInt( "versionid"));
                builder.datasetSource(rs.getString( "datasetSource"));
                builder.latest(rs.getBoolean( "isLatest"));
                setVersionMetadata( builder );
                versions.add( builder.build());
            }
            return versions;
        }
    }
    
    public List<DatasetLocation> getDatasetLocations(Long versionPk) throws SQLException{
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
                builder.fileSystemPath(rs.getString( "path"));
                builder.runMin(rs.getLong( "runmin"));
                builder.runMax(rs.getLong( "runmax"));
                builder.eventCount(rs.getLong( "numberevents"));
                builder.fileSize(rs.getLong( "filesizebytes"));
                builder.checkSum(rs.getLong( "checksum"));
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
    
    protected Dataset insertDataset(Long parentPk, DatacatObject.Type parentType, String parentPath, Dataset request) throws SQLException {
        String insertSql = "insert into VerDataset (DatasetName, DataSetFileFormat, DataSetDataType, "
                + "DatasetLogicalFolder, DatasetGroup) values (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement( insertSql, new String[]{"Dataset", "Registered"} )) {
            stmt.setString(1, request.getName() );
            stmt.setString(2, request.getFileFormat() );
            stmt.setString(3, request.getDataType().toUpperCase());
            switch(parentType){
                case FOLDER:
                    stmt.setLong( 4, parentPk);
                    stmt.setNull( 5, java.sql.Types.BIGINT );
                    break;
                case GROUP:
                    stmt.setNull( 4, java.sql.Types.BIGINT );
                    stmt.setLong( 5, parentPk);
            }
            stmt.executeUpdate();   // will throw exception if required parameter is empty...
            DatasetBuilder builder = DatasetBuilder.create(request);
            try(ResultSet rs = stmt.getGeneratedKeys()){
                rs.next();
                builder.pk(rs.getLong(1));
                builder.parentPk(parentPk);
                builder.parentType(parentType);
                builder.path(parentPath);
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
            if(priority != null)
                stmt.setInt( 3, priority);
            else
                stmt.setNull( 3, java.sql.Types.INTEGER);
            stmt.executeUpdate();
            return dataType.toUpperCase();
        }
    }
    
    protected String insertDatasetFileFormat(String fileFormat, String description, String mimeType) throws SQLException{
        String sql = "INSERT INTO DatasetFileFormat (DatasetFileFormat, Description, MimeType) "
                + "VALUES (?,?,?)";
        try(PreparedStatement stmt = getConnection().prepareStatement( sql )){
            stmt.setString(1, fileFormat.toLowerCase());
            stmt.setString(2, description);
            if(mimeType != null)
                stmt.setString( 3, mimeType);
            else
                stmt.setNull( 3, java.sql.Types.VARCHAR);
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
    
    protected void deleteAllDatasetViews(long datasetPk) throws SQLException{
        for(DatasetVersion v: getDatasetVersions( datasetPk )){
            deleteDatasetVersion(datasetPk, v);
        }
    }
    
    protected void deleteDataset(long datasetPk) throws SQLException{
        String deleteSql = "delete from VerDataset where Dataset=?";
        delete1(deleteSql, datasetPk);
    }
    
    protected DatasetVersion insertDatasetVersion(Long datasetPk, int newVersionId, boolean isCurrent, DatasetVersion request) throws SQLException {
        // One last integrity check
        newVersionId = newVersionId < 0 ? 0 : newVersionId;
        
        String sql = 
                "insert into DatasetVersion "
                + "(Dataset, VersionID, DataSetSource, ProcessInstance, TaskName) "
                + "values (?, ?, ?, ?, ?)";
      
        DatasetVersion retVersion = null;
        try(PreparedStatement stmt = getConnection().prepareStatement( sql, new String[]{"DatasetVersion", "Registered"} )) {
            stmt.setLong(1, datasetPk );
            stmt.setInt(2, newVersionId );
            stmt.setString(3, request.getDatasetSource() );
            if(request.getProcessInstance() != null){
                stmt.setLong(4, request.getProcessInstance() );
            } else {
                stmt.setNull(4, java.sql.Types.BIGINT);
            }
            stmt.setString(5, request.getTaskName());
            stmt.executeUpdate();   // will throw exception if required parameter is empty...
            
            DatasetVersion.Builder builder = new DatasetVersion.Builder(request);
            builder.parentPk(datasetPk);
            builder.versionId(newVersionId);
            builder.latest(isCurrent);
            try(ResultSet rs = stmt.getGeneratedKeys()){
                rs.next();
                builder.pk(rs.getLong(1));
                builder.parentPk(datasetPk);
                builder.created(rs.getTimestamp(2));
            }
            retVersion = builder.build();
        }
        if(request.getMetadata() != null){
            addDatasetVersionMetadata(retVersion, request.getNumberMetadata());
            addDatasetVersionMetadata(retVersion, request.getStringMetadata());
        }
        // Update isLatest
        if(retVersion.isLatest()){
            sql = "UPDATE VerDataset set LatestVersion = ? WHERE Dataset = ?";
            try(PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                stmt.setLong(1, retVersion.getPk());
                stmt.setLong(2, datasetPk);
                stmt.executeUpdate();
            }
        }
        return retVersion;
    }
    
    protected void deleteDatasetVersion(long datasetPk, DatasetVersion version) throws SQLException{
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
                stmt.executeQuery();
            }            
        }

        String deleteSql = "delete from DatasetVersion where DatasetVersion=?";
        delete1(deleteSql, version.getPk());
    }

    protected DatasetLocation insertDatasetLocation(Long datasetVersionPk, DatasetLocation request) throws SQLException{
        String insertSql = 
              "insert into VerDataSetLocation (DatasetVersion, DatasetSite, Path, RunMin, RunMax, "
              + " NumberEvents, FileSizeBytes) values (?, ?, ?, ?, ?, ?, ?)";
        
        DatasetLocation retLoc;
        try(PreparedStatement stmt = getConnection().prepareStatement( insertSql, new String[]{"DatasetLocation", "Registered"} )) {
            stmt.setLong( 1, datasetVersionPk );
            stmt.setString( 2, request.getSite() );
            stmt.setString( 3, request.getFileSystemPath() );
            stmt.setLong( 4, request.getRunMin() );
            stmt.setLong( 5, request.getRunMax() );
            stmt.setLong( 6, request.getEventCount() );
            stmt.setLong( 7, request.getFileSize() );
            stmt.executeUpdate();   // will throw exception if required parameter is empty...
            DatasetLocation.Builder builder = new DatasetLocation.Builder(request);
            // now retrieve the primary key:
            
            try(ResultSet rs = stmt.getGeneratedKeys()){
                rs.next();
                builder.pk(rs.getLong(1));
                builder.parentPk(datasetVersionPk);
                builder.created(rs.getTimestamp(2));
            }
            retLoc = builder.build();
        }
        // If this is to be the master
        if(retLoc.isMaster()){
            String sql = "UPDATE VerDataset set LatestVersion = ? WHERE Dataset = ?";
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
                stmt.executeQuery();
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
                VERSION_EXISTS.throwError( dsPath, String.format( "Version %d already exists", currentId ) );
            default:
                if(currentId >= newId){
                    NEWER_VERSION_EXISTS.throwError( dsPath, String.format( "Version ID %d not greater than current version of %d", newId, currentId ) );
                }
        }
        return newId;
    }
    
    /**
     * Check if we can merge these items
     * @param datasetPk
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
    
    protected void assertCanCreateLocation(Long versionPk, String dsPath, DatasetLocation newLoc) throws SQLException, FileSystemException{
        for(DatasetLocation l: getDatasetLocations(versionPk)){
            if(l.getSite().equals(newLoc.getSite())){
                LOCATION_EXISTS.throwError( dsPath,"Location entry for site " + newLoc.getSite() + " already exists");
            }
        }
    }

}