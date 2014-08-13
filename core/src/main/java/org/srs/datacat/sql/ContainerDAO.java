
package org.srs.datacat.sql;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.DatacatObjectBuilder;
import org.srs.datacat.shared.LogicalFolder;
import org.srs.datacat.shared.container.BasicStat;
import org.srs.datacat.shared.container.DatasetStat;
import org.srs.datacat.vfs.DcPath;


/**
 *
 * @author bvan
 */
public class ContainerDAO extends BaseDAO {

    public ContainerDAO(Connection conn){
        super( conn );
    }
    
    public DatacatObject createContainer(Long parentPk, DcPath parentPath, DatacatObject request) throws SQLException{
        return insertContainer( parentPk, parentPath.toString(), request );
    }

    protected DatacatObject insertContainer(Long parentPk, String parentPath, DatacatObject request) throws SQLException{
        String tableName;
        String parentColumn;
        DatacatObject.Type newType = DatacatObject.Type.typeOf(request);
        switch(newType){
            case FOLDER:
                tableName = "DatasetLogicalFolder";
                parentColumn = "Parent";
                break;
            case GROUP:
                tableName = "DatasetGroup";
                parentColumn = "DatasetLogicalFolder";
                break;
            default:
                throw new SQLException("Unknown parent table: " + newType.toString());
        }
        String insertSqlTemplate = "INSERT INTO %s (NAME, %s, DESCRIPTION) VALUES (?,?,?)";
        String sql = String.format( insertSqlTemplate, tableName, parentColumn );
        
        String description = request instanceof DatasetContainer ? ((DatasetContainer) request).getDescription() : null;
        DatacatObject retObject;
        try (PreparedStatement stmt = getConnection().prepareStatement( sql, new String[]{tableName} )) {
            stmt.setString( 1, request.getName());
            stmt.setLong(2, parentPk);
            stmt.setString(3, description);
            stmt.executeUpdate();
            DatasetContainer.Builder builder = new DatasetContainer.Builder(request);
            try (ResultSet rs = stmt.getGeneratedKeys()){
                rs.next();
                builder.pk(rs.getLong(1));
            }
            builder.parentPk(parentPk);
            builder.path(parentPath);
            retObject = builder.build();
        }
        
        if(request.getMetadata() != null && !request.getMetadata().isEmpty()){
            if(newType == DatacatObject.Type.FOLDER){
                addFolderMetadata(retObject.getPk(), retObject.getNumberMetadata());
                addFolderMetadata(retObject.getPk(), retObject.getStringMetadata());
            } else {
                addGroupMetadata(retObject.getPk(), retObject.getNumberMetadata());
                addGroupMetadata(retObject.getPk(), retObject.getStringMetadata());
            }
        }
        return retObject;
    }
    
    protected void deleteFolder(long folderPk) throws SQLException {
        String deleteSql = "delete from DatasetLogicalFolder where DatasetLogicalFolder=?";
        delete1( deleteSql, folderPk);
    }
    
    protected void deleteGroup(long groupPk) throws SQLException {
        String deleteSql = "delete from DatasetGroup where DatasetGroup=?";
        delete1( deleteSql, groupPk);
    }
    
    public ArrayList<DatacatObject> getAllContainers(DcPath path, Long pk) throws SQLException{
        String sql = 
                " WITH CONTAINERS (type, pk, name, parent, lev, relpath) AS ( "
                + "     SELECT 'FOLDER', datasetlogicalfolder, name, parent, 0, '.' from datasetlogicalfolder where parent = ? "
                + "   UNION ALL "
                + "     SELECT   objects.type, objects.pk, objects.name, objects.parent, parent.lev+1,  "
                + "     CASE WHEN parent.relpath = '.'  "
                + "        THEN './' || parent.name  "
                + "        ELSE parent.relpath || '/' || parent.name  "
                + "        END      "
                + "      FROM ( "
                + "        SELECT   'FOLDER' type, DatasetLogicalFolder.datasetLogicalFolder pk, "
                + "          DatasetLogicalFolder.name, DatasetLogicalFolder.parent parent "
                + "          FROM DatasetLogicalFolder "
                + "      UNION ALL "
                + "        SELECT   'GROUP' type, DatasetGroup.datasetGroup pk, DatasetGroup.name, "
                + "          DatasetGroup.datasetLogicalFolder parent "
                + "          FROM DatasetGroup  "
                + "      ) objects  "
                + "      JOIN CONTAINERS parent on (objects.parent = parent.pk) "
                + ") "
                + "SELECT type, pk, name, parent, lev, relpath FROM CONTAINERS "
                + "ORDER BY relpath";
        DatacatObjectBuilder builder = null;
        try (PreparedStatement stmt = getConnection().prepareStatement( sql )){
            stmt.setLong(1, pk);
            ResultSet rs = stmt.executeQuery();
            ArrayList<DatacatObject> olist = new ArrayList<>();
            while(rs.next()){
                builder = getBuilder( rs );
                DcPath absPath = (DcPath) path.resolve(rs.getString("relpath")).toAbsolutePath();
                builder.path( absPath.toString() );
                completeObject(builder);
                olist.add( builder.build());
            }
            return olist;
        }
    }

    
    public BasicStat getBasicStat(DatacatObject container) throws SQLException {
        String parent = container instanceof LogicalFolder ? "datasetlogicalfolder" : "datasetgroup";

        String statSQL = "select 'DATASET' type, count(1) count from verdataset where " + parent + " = ? ";
        if(container instanceof LogicalFolder){
            statSQL = statSQL + "UNION ALL select 'GROUP' type, count(1) count from datasetgroup where datasetlogicalfolder = ? ";
            statSQL = statSQL + "UNION ALL select 'FOLDER' type, count(1) count from datasetlogicalfolder where parent = ? ";
        }
        try(PreparedStatement stmt = getConnection().prepareStatement( statSQL )) {
            stmt.setLong( 1, ((DatacatObject) container).getPk() );
            if(container instanceof LogicalFolder){
                stmt.setLong( 2, ((DatacatObject) container).getPk() );
                stmt.setLong( 3, ((DatacatObject) container).getPk() );
            }
            ResultSet rs = stmt.executeQuery();
            BasicStat cs = new BasicStat();
            while(rs.next()){
                String type = rs.getString( "type" );
                Long count = rs.getLong( "count");
                if("DATASET".equals( type ) )
                    cs.setDatasetCount( count );
                else if("GROUP".equals( type ) )
                    cs.setGroupCount( count );
                else if("FOLDER".equals( type ) )
                    cs.setFolderCount( count );
            }
            return cs;
        }
    }
    
    
    public DatasetStat getDatasetStat(DatacatObject container, BasicStat stat) throws SQLException {
        String primaryTable;
        if(container instanceof LogicalFolder){
            primaryTable = "datasetlogicalfolder";
        } else {
            primaryTable = "datasetgroup";
        }
        
        String statSQL = 
                "select count(*) files, Sum(l.NumberEvents) events, "
                + "Sum(l.filesizebytes) totalsize, min(l.runMin) minrun, max(l.runmax) maxrun "
                + "from " + primaryTable + " g "
                + "join verdataset d on (g." + primaryTable + " =d." + primaryTable + ") "
                + "join datasetversion dv on (d.latestversion=dv.datasetversion) "
                + "join verdatasetlocation l on (dv.masterLocation=l.datasetlocation) "
                + "where g." + primaryTable + " = ? ";
        
        try(PreparedStatement stmt = getConnection().prepareStatement( statSQL )) {
            stmt.setLong( 1, container.getPk() );
            ResultSet rs = stmt.executeQuery();
            if(!rs.next()){
                throw new SQLException("Unable to determine dataset stat");
            }
            DatasetStat ds = new DatasetStat(stat);
            ds.setDatasetCount( rs.getLong( "files") );
            ds.setEventCount( rs.getLong( "events") );
            ds.setDiskUsageBytes( rs.getLong( "totalsize") );
            ds.setRunMin( rs.getLong( "minrun") );
            ds.setRunMax( rs.getLong( "maxrun") );
            return ds;
        }
    }
    
    public DirectoryStream<DatacatObject> getChildrenStream(Long parentPk, final String parentPath) throws SQLException, IOException{
        String sql = "WITH OBJECTS (type, pk, name, parent) AS ( "
                + "    SELECT 'FOLDER', datasetlogicalfolder, name, parent "
                + "      FROM datasetlogicalfolder "
                + "  UNION ALL "
                + "    SELECT 'GROUP', datasetGroup, name, datasetLogicalFolder "
                + "      FROM DatasetGroup "
                + "  UNION ALL "
                + "    SELECT   'DATASET', dataset, datasetName, "
                + "      CASE WHEN datasetlogicalfolder is not null THEN datasetlogicalfolder else datasetgroup END "
                + "      FROM VerDataset "
                + ") "
                + "SELECT type, pk, name, parent FROM OBJECTS "
                + "  WHERE parent = ? "
                + "  ORDER BY name";

        final PreparedStatement stmt = getConnection().prepareStatement( sql );
        stmt.setLong( 1, parentPk);
        final ResultSet rs = stmt.executeQuery();
        return new DirectoryStream<DatacatObject>() {

            @Override
            public Iterator<DatacatObject> iterator(){
                return new Iterator<DatacatObject>() {
                    DatacatObject next = null;

                    @Override
                    public boolean hasNext(){
                        try{
                            return checkNext();
                        } catch (SQLException | NoSuchElementException ex){
                            return false;
                        }
                    }

                    private boolean checkNext() throws SQLException {
                        if(next != null){
                            return true;
                        }
                        if(rs.next()){
                            next = getBuilder( rs ).path( parentPath ).build();
                            return true;
                        }
                        throw new NoSuchElementException();
                    }

                    @Override
                    public DatacatObject next(){
                        try {
                            if(checkNext()){
                                DatacatObject ret = next;
                                next = null;
                                return ret;
                            }
                        } catch(SQLException ex) {
                            System.out.println(ex.toString());
                            throw new NoSuchElementException(ex.getMessage());
                        }
                        throw new NoSuchElementException();
                    }

                    @Override
                    public void remove(){
                        throw new UnsupportedOperationException( "Remove not supported" );
                    }

                };
            }

            @Override
            public void close() throws IOException{
                try {
                    stmt.close();
                } catch(SQLException ex) {
                    throw new IOException("Error closing statement", ex);
                }
            }
        };
      
    }

}
