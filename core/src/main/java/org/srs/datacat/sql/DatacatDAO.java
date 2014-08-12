/*
package org.srs.datacat.sql;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.glassfish.jersey.uri.UriComponent;
import org.srs.datacat.schema.DatacatObjectUnion;
import org.srs.datacat.schema.DatasetContainerUnion;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.DatacatObjectBuilder;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetGroup;
import org.srs.datacat.shared.LogicalFolder;
import org.srs.datacat.shared.container.BasicStat;
import org.srs.datacat.shared.container.BasicStat.StatType;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.shared.DatacatObjectBuilder.DatasetContainerBuilder;
import org.srs.datacat.shared.container.DatasetStat;
import org.srs.datacat.shared.dataset.DatasetBuilder;
import org.srs.rest.shared.sql.SingleResultSetHandler;
//import org.zerorm.core.Param;
//import org.zerorm.core.Select;

/*
 * Do all the nasty database stuff here
 * @author bvan

public class DatacatDAO extends BaseDAO {

    public enum PrimaryKeyColumn {
        DATASETLOGICALFOLDER ("PARENT"),
        DATASETGROUP ("DatasetLogicalFolder"),
        DATASET (null),
        DATASETVERSION ("DATASET"),
        DATASETLOCATION ("DatasetVersion");
        
        private String pCol;
        PrimaryKeyColumn(String pCol){this.pCol = pCol;}
        
        public String getParentColumn(){ return pCol;}
    }
  
    public DatacatDAO(Connection conn){
        super(conn);
    }

    public DatacatObject findAnyObject(Path path) throws SQLException, FileNotFoundException {
        return findAnyObject(path, StatType.NONE);
    }
    
    public DatacatObject findAnyObject(Path fullPath, final StatType statType) throws SQLException, FileNotFoundException {
        final DatacatObject parent;
        boolean hasParentPath = fullPath.getParent() != null && !"/".equals( fullPath.getParent().toString());
        if(hasParentPath){
            parent = findContainer(fullPath.getParent(), StatType.NONE);
        } else {
            return findContainer(fullPath, statType);
        }
        
        HashMap<String, List<String>> versionLocationParams = new HashMap<>();
        String finalPath = DatacatDAO.decodeDatasetMatrix( fullPath, versionLocationParams);

        DatacatObjectUnion ut = new DatacatObjectUnion();
        Param<String> nameParam = ut.name.checkedParam( "name", finalPath );
        Param<Long> parentParam = ut.parent.checkedParam( "pk", parent.getPk() );

        Select statement = 
                new Select(ut.getColumns())
                .from( ut.as( "objects"))
                .where( ut.name.eq( nameParam ), ut.parent.eq( parentParam ));
        
        DatacatObjectBuilder builder = null;
        try (PreparedStatement stmt = statement.prepareAndBind(getConnection())){            
            ResultSet rs = stmt.executeQuery();
            assertObjectExists( rs );
            builder = DatacatDAO.getBuilder( rs );
            builder.path( parent.getPath() + "/" + parent.getName() );
        }
        completeObject(builder, versionLocationParams);
        return builder.build();
    }
    
    public DatacatObject findContainer(Path fullPath, final StatType statType) throws SQLException, FileNotFoundException {
        
        DatasetContainerUnion dcu =  new DatasetContainerUnion();
        Param<Long> parent = dcu.parent.checkedParam();
        Param<String> name = dcu.name.checkedParam();

        Select statement = new Select( dcu.getColumns() )
                .from(dcu.as( "objects" ))
                .where( dcu.parent.eq( parent ), dcu.name.eq( name ) );
        parent.setValue( 0L );
        DatasetContainerBuilder builder = simpleRecursiveQuery( statement, parent, name, fullPath, statType );
        completeObject(builder, null);
        
        return builder.build();
    }
    
    public ArrayList<DatacatObject> getChildren(Path fullPath, final StatType type, boolean includeDatasets) throws SQLException, FileNotFoundException {
        final DatacatObject parent = findContainer(fullPath.getParent(), StatType.BASIC);
        final String path = fullPath.toString();
        String sql = 
                "select type,pk,name,parent from ( " +
                "    select 'GROUP' type, g.datasetgroup pk, g.name name, g.datasetlogicalfolder parent " +
                "        from datasetgroup g " +
                "    UNION ALL " +
                "    select 'FOLDER' type, f.datasetlogicalfolder pk, f.name name, f.parent parent " +
                "        from datasetlogicalfolder f " +
                ( includeDatasets ? 
                "    UNION ALL " +
                "    select 'DATASET' type, d.dataset pk, d.datasetname name, " +
                ((parent instanceof LogicalFolder) ? " d.datasetlogicalfolder parent " : " d.datasetgroup parent " ) + // fix
                "        from verdataset d "
                : "" ) + 
                ") where parent = ?" + 
                "order by name";

        
        SingleResultSetHandler<ArrayList<DatacatObject>> rh = new SingleResultSetHandler<ArrayList<DatacatObject>>() {
            ArrayList<DatacatObject> children = new ArrayList<>();

            @Override
            public void finalize(ResultSet rs) throws SQLException {
                long t0 = System.currentTimeMillis();

                do{
                    DatacatObjectBuilder builder= getBuilder( rs );
                    builder.path(path);
                    if( ((System.currentTimeMillis() - t0 ) < 5000) || type != StatType.LAZY){
                        DatacatDAO.this.maybeSetStat(builder, type);
                    }
                    children.add( builder.build() );
                } while(rs.next());
                setObject(children);
            }
        };
        ArrayList par = new ArrayList(){{add(parent.getPk());}};
        simpleQuery(sql, par,rh );
        return rh.getObject();
    }
    
    String vdColumns = "vd.datasetfileformat,vd.datasetdatatype,vd.latestversion,vd.registered vregistered";
    String dsvColumns = "dsv.datasetversion, dsv.versionid, dsv.datasetsource, dsv.masterlocation";
    String vdlColumns = "vdl.datasetlocation, vdl.datasetsite, vdl.path, vdl.runmin, vdl.runmax, "
            + "vdl.numberevents, vdl.filesizebytes, vdl.checksum, vdl.lastmodified, "
            + "vdl.lastscanned, vdl.scanstatus, vdl.registered lregistered";
    String latestTest = "CASE WHEN vd.latestversion = dsv.datasetversion THEN 1 ELSE 0 END isLatest";
    String masterTest = "CASE WHEN dsv.masterlocation = vdl.datasetlocation THEN 1 ELSE 0 END isMaster";

    String allDSColumns = vdColumns + "," + dsvColumns + "," + vdlColumns;
    
    
    public ArrayList<Dataset> getDatasets(Path fullPath) throws SQLException, FileNotFoundException {
        final DatacatObject parent = findAnyObject(fullPath, StatType.BASIC);
        
        if( !(parent instanceof DatasetContainer)){
            throw new FileNotFoundException("Supplied path is not a dataset container");
        }
        
        String sql = "select 'DATASET' type, vd.dataset pk, vd.%s parent, vd.datasetname name," 
                + allDSColumns + "," + latestTest + "," + masterTest
                + " from verdataset vd "
                + "join datasetversion dsv on (vd.latestversion = dsv.datasetversion) "
                + "join verdatasetlocation vdl on (dsv.masterlocation = vdl.datasetlocation) "
                + "where vd.%s = ?";
        if(parent instanceof LogicalFolder){
            sql = String.format( sql, "datasetlogicalfolder","datasetlogicalfolder" );
        } else {
            sql = String.format( sql, "datasetgroup", "datasetgroup" );
        }
        
        final String parentPath = parent.getPath() + "/" + parent.getName();
        SingleResultSetHandler<ArrayList<Dataset>> rh = new SingleResultSetHandler<ArrayList<Dataset>>() {
            ArrayList<Dataset> children = new ArrayList<>();
            
            @Override
            public void finalize(ResultSet rs) throws SQLException {
                do{
                    DatasetBuilder builder = (DatasetBuilder) DatacatDAO.getBuilder(rs);
                    builder.path(parentPath);
                    completeFlatDataset(builder, rs);
                    children.add( builder.build() );
                } while(rs.next());
                setObject(children);
            }
        };
        ArrayList par = new ArrayList(){{add(parent.getPk());}};
        simpleQuery(sql, par,rh );
        return rh.getObject();
    }

    public ArrayList<DatacatObject> rootChildren() throws SQLException, FileNotFoundException {
        
        String sql = 
                "select type,pk,name,parent from ( " +
                "    select 'GROUP' type, g.datasetgroup pk, g.name name, g.datasetlogicalfolder parent " +
                "        from datasetgroup g " +
                "    UNION ALL " +
                "    select 'FOLDER' type, f.datasetlogicalfolder pk, f.name name, f.parent parent " +
                "        from datasetlogicalfolder f " +
                "    UNION ALL " +
                "    select 'DATASET' type, d.dataset pk, d.datasetname name, " +
                " d.datasetlogicalfolder parent " +
                "        from verdataset d " +
                ") where parent = ? " + 
                "order by name";
        
        SingleResultSetHandler<ArrayList<DatacatObject>> rh = new SingleResultSetHandler<ArrayList<DatacatObject>>() {
            ArrayList<DatacatObject> rootObjects = new ArrayList<>();

            @Override
            public void finalize(ResultSet rs) throws SQLException {
                // do..while because first resultset has been checked
                do{
                    DatacatObjectBuilder builder = DatacatDAO.getBuilder(rs);
                    DatacatDAO.this.maybeSetStat( builder, StatType.BASIC );
                    rootObjects.add( builder.build() );
                } while(rs.next());
                setObject(rootObjects);
            }
        };
        ArrayList par = new ArrayList(){{add( 0 );}};
        simpleQuery(sql, par,rh);
        return rh.getObject();
    }

    public ArrayList<DatacatObject> rootChildrenRecursive() throws SQLException, FileNotFoundException {
        
        String sql = 
                "select type,pk,name,parent from ( "
                + "select 'FOLDER' type, child.datasetlogicalfolder pk, child.name name, child.parent parent from datasetlogicalfolder child "
                + "union all "
                + "select 'GROUP' type, dsg.datasetgroup pk, dsg.name name, dsg.datasetlogicalfolder parent from datasetgroup dsg "
                + ") "
                + "start with pk = ? "
                + "connect by prior pk = parent "
                + "order siblings by name ";
        
        SingleResultSetHandler<ArrayList<DatacatObject>> rh = new SingleResultSetHandler<ArrayList<DatacatObject>>() {
            @Override
            public void finalize(ResultSet rs) throws SQLException {
                ArrayList<LogicalFolder> pStack = new ArrayList<>();
                LogicalFolder root = (LogicalFolder) DatacatDAO.getBuilder( rs ).build();
                pStack.add( root );

                while (rs.next()) {
                    DatacatObject c = DatacatDAO.getBuilder( rs ).build();
                    LogicalFolder p = pStack.get( pStack.size() - 1 );
                    for ( ; !c.getParentPk().equals( p.getPk() ); p = pStack.get( pStack.size() - 1 ) ) {
                        pStack.remove( pStack.size() - 1 );
                    }
                    pStack.add( p );
                    p.addChild( c );
                    if ( c instanceof LogicalFolder ) {
                        pStack.add( (LogicalFolder) c );
                    }
                }
                setObject( root.getChildren() );
            }
        };
        ArrayList par = new ArrayList() {{add( 0 );}};
        simpleQuery( sql, par, rh );
        return rh.getObject();
    }
    
    public static DatacatObjectBuilder getBuilder(ResultSet rs) throws SQLException {
        String type = rs.getString("type");
        DatacatObjectBuilder o = DatacatObject.builder( type )
            .pk(rs.getLong("pk"))
            .parentPk(rs.getLong("parent"))
            .name(rs.getString("name"));
        return o;
    }
    
    /*private void setMetadataFromDB(DatacatObjectBuilder builder) throws SQLException{
        HashMap<String, String> smap = new HashMap<>();
        HashMap<String, Number> nmap = new HashMap<>();
        
        String tableType = null;
        String column = null;
        Long pk = builder.pk;
        if(builder instanceof DatasetBuilder){
            tableType = "verdataset";
            column = "datasetversion";
            pk = ((DatasetBuilder) builder).versionPk;
        }  else if (builder instanceof LogicalFolder.Builder){
            tableType = "logicalfolder";
            column = tableType;
        } else if(builder instanceof DatasetGroup.Builder){
            tableType = "datasetgroup";
            column = tableType;
        }
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
    
    private void completeObject(DatacatObjectBuilder builder, HashMap<String, List<String>> verLocParams) throws SQLException {

        if(builder instanceof DatasetBuilder){
            completeDataset( (DatasetBuilder) builder, verLocParams );
        } else if(builder instanceof DatasetGroup.Builder){
            completeContainer( (DatasetGroup.Builder) builder,
                    "select description from datasetgroup where datasetgroup = ?" );
        } else if(builder instanceof LogicalFolder.Builder){
            completeContainer( (LogicalFolder.Builder) builder, 
                    "select description from datasetlogicalfolder where datasetlogicalfolder = ?" );
        }
        setContainerMetadata( builder );
    }
    
    /*private void completeContainer(DatasetContainerBuilder builder, String sql) throws SQLException{
        try(PreparedStatement stmt = getConnection().prepareStatement( sql )) {
            stmt.setLong( 1, builder.pk );
            ResultSet rs = stmt.executeQuery();
            rs.next();
            builder.description(rs.getString( "description" ));
        }
    }
    
    private void completeDataset(DatasetBuilder builder, HashMap<String, List<String>> verLocParams) throws SQLException{
        boolean hasVersions = false;
        boolean hasLocations = false;
        String verJoin = "join datasetversion dsv on (vd.latestversion = dsv.datasetversion) ";
        String verClause = "";
        if(verLocParams.containsKey( "ver" )){
            hasVersions = true;
            verJoin = "join datasetversion dsv on (dsv.dataset = dv.dataset) ";
            String versions = "";
            for(Iterator<String> vIter = verLocParams.get( "ver").iterator(); 
                    vIter.hasNext();
                    versions += vIter.next() + (vIter.hasNext() ? "," :"")
            );
            verClause = String.format(" AND dsv.versionid in (%s)", versions);
        }
        
        String locJoin = "join verdatasetlocation vdl on (dsv.masterlocation = vdl.datasetlocation) ";
        String locClause = "";
        if(verLocParams.containsKey( "loc" )){
            hasLocations = true;
            locJoin = "join verdatasetlocation vdl on (vdl.datasetversion = dsv.datasetversion) ";
            String locations = "";
            for(Iterator<String> lIter = verLocParams.get( "loc").iterator(); 
                    lIter.hasNext();
                    locations += lIter.next() + (lIter.hasNext() ? "," :"")
            );
            locClause = String.format(" AND vdl.site in (%s)", locations);
        }
        
        String sql = "select " + allDSColumns + "," + latestTest + "," + masterTest + " from verdataset vd "
                + verJoin
                + locJoin
                + "where vd.dataset = ? " + verClause + locClause
                + " order by dsv.datasetversion, vdl.datasetlocation";
        
        
        try(PreparedStatement stmt = getConnection().prepareStatement( sql )) {
            stmt.setLong( 1, builder.pk );
            ResultSet rs = stmt.executeQuery();
            rs.next();
            completeFlatDataset(builder, rs);
        }
    }
    
    private void completeFlatDataset(DatasetBuilder builder, ResultSet rs) throws SQLException{
        builder.datasetFileFormat(  rs.getString( "datasetfileformat" ) )
                .datasetDataType( rs.getString( "datasetdatatype" ) );
        
        builder.versionPk( rs.getLong( "datasetVersion"));
        builder.versionId( rs.getInt( "versionid" ) );
        builder.datasetSource( rs.getString( "datasetsource" ) );
        
        builder.versionCreated( rs.getTimestamp( "vregistered" ) );
        builder.latest( rs.getBoolean( "isLatest"));

        builder.locationPk( rs.getLong( "datasetLocation" ) );
        builder.site( rs.getString( "datasetsite" ) );
        builder.fileSystemPath( rs.getString( "path" ) );
        builder.runMin( rs.getLong( "runmin" ) );
        builder.runMax( rs.getLong( "runmax" ) );
        builder.eventCount( rs.getLong( "numberevents" ) );
        builder.fileSize( rs.getLong( "filesizebytes" ) );
        builder.checkSum( rs.getLong( "checksum" ) );
        builder.locationModified( rs.getTimestamp( "lastmodified") );
        builder.locationScanned( rs.getTimestamp( "lastscanned") );
        builder.locationCreated( rs.getTimestamp( "lregistered" ));
        builder.master( rs.getBoolean( "isMaster"));
        
        /*
        if(d instanceof ExtendedDataset){
            dsv.setLocation( vdl );
            ((ExtendedDataset) d).setVersion( dsv );
        } else {
            ((LatestDataset) d).setLocation( vdl );
            ((LatestDataset) d).setVersion( dsv );
        }
        
    }
    
    private void maybeSetStat(DatacatObjectBuilder builder, StatType statType) throws SQLException {
        if(!(builder instanceof DatasetContainerBuilder)){
            return;
        }
        if(statType == StatType.NONE){
            return;
        }
        setBasicStat( (DatasetContainerBuilder) builder);
        if(statType == StatType.DATASET){
            setExtendedStat( (DatasetContainerBuilder) builder);
        }
    }
    
    private void setBasicStat(DatasetContainerBuilder builder) throws SQLException{
        DatacatObject container = builder.build();
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
            builder.stat( cs );
        }
    }
    
    private void setExtendedStat(DatasetContainerBuilder builder) throws SQLException{
        setDatasetStat( builder );
    }
    
    private void setDatasetStat(DatasetContainerBuilder builder) throws SQLException{
        String primaryTable = "datasetgroup";
        
        if(builder instanceof LogicalFolder.Builder){
            primaryTable = "datasetlogicalfolder";
        }
        
        String statSQL = "select count(*) files, Sum(l.NumberEvents) events, Sum(l.filesizebytes) totalsize, min(l.runMin) minrun, max(l.runmax) maxrun " +
                    "from " + primaryTable + " g " + 
                    "join verdataset d on (g." + primaryTable +" =d." + primaryTable + ") " + 
                    "join datasetversion dv on (d.latestversion=dv.datasetversion) " + 
                    "join verdatasetlocation l on (dv.masterLocation=l.datasetlocation) " + 
                    "where g." + primaryTable + " = ? ";
        
        try(PreparedStatement stmt = getConnection().prepareStatement( statSQL )) {
            stmt.setLong( 1, builder.pk );
            ResultSet rs = stmt.executeQuery();
            if(!rs.next()){
                return;
            }
            DatasetStat ds = new DatasetStat(builder.stat);
            ds.setDatasetCount( rs.getLong( "files") );
            ds.setEventCount( rs.getLong( "events") );
            ds.setDiskUsageBytes( rs.getLong( "totalsize") );
            ds.setRunMin( rs.getLong( "minrun") );
            ds.setRunMax( rs.getLong( "maxrun") );
            builder.stat(ds);
        }
    }
    
    void simpleQuery(String sql, List params, SingleResultSetHandler rsa) throws SQLException, FileNotFoundException {
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)){
            for(int j = 0; j < params.size(); j++){
                stmt.setObject(j+1, params.get(j));
            }
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                rsa.finalize(rs);
            } else {
                throw (new FileNotFoundException("Unable to resolve objects"));
            }
        }
    }
    
    DatasetContainerBuilder simpleRecursiveQuery(Select statement, Param<Long> parentPk, Param<String> itemName, Path recursepath, StatType statType) throws SQLException, FileNotFoundException {
        DatasetContainerBuilder builder = null;
        String fullPath = "/";
        try (PreparedStatement stmt = statement.prepare( getConnection()) ){
            for(int i = 0; i < recursepath.getNameCount(); i++){
                Path pathItem = recursepath.getName(i);
                itemName.setValue( pathItem.getFileName().toString() );
                statement.bindAll( stmt );
                ResultSet rs = stmt.executeQuery();
                assertObjectExists( rs );
                parentPk.setValue(rs.getLong( 2 ));
                builder = (DatasetContainerBuilder) DatacatDAO.getBuilder(rs);
                builder.path(fullPath);
                fullPath += pathItem.toString() + "/";
            }
            maybeSetStat( builder, statType);
        }
        return builder;
    }
 
    private final static String verPattern = ";ver=\\w+";
    private final static String versionPattern = ";version=\\w+";
    private final static String locPattern = ";loc=\\w+";
    private final static String locationPattern = ";location=\\w+";
    public static String decodeDatasetMatrix(Path path, Map<String, List<String>> verLocParams) {
        String pathSegment = path.getFileName().toString();
        Map<String, List<String>> matMap = UriComponent.decodeMatrix( pathSegment, true );
        for(String str: matMap.keySet()){
            if("ver".equalsIgnoreCase(str) || "version".equalsIgnoreCase(str)){
                verLocParams.put( "ver", matMap.get( str ) );
            }
            if("loc".equalsIgnoreCase(str) || "location".equalsIgnoreCase(str)){
                verLocParams.put( "loc", matMap.get( str ) );
            }
        }
        pathSegment = pathSegment.replaceAll( verPattern, "" );
        
        pathSegment = pathSegment.replaceAll( versionPattern, "" );
        pathSegment = pathSegment.replaceAll( locPattern, "" );
        pathSegment = pathSegment.replaceAll( locationPattern, "" );
        return pathSegment;
    }
}
*/