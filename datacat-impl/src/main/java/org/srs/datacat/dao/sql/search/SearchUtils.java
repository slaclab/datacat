
package org.srs.datacat.dao.sql.search;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.DirectoryStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.freehep.commons.lang.AST;
import org.srs.datacat.model.DatacatNode;
import org.zerorm.core.Select;

import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.dataset.DatasetLocationModel;
import org.srs.datacat.model.ModelProvider;
import org.srs.datacat.model.RecordType;
import org.srs.datacat.shared.DatasetLocation;

import org.srs.vfs.PathUtils;


/**
 *
 * @author bvan
 */
public final class SearchUtils {
    
    private SearchUtils(){}
    
    public static String getErrorString(AST ast, final String ident){
        if(ast.toString().length() < 32){
            return ast.toString();
        }
        
        final StringBuilder startOfError = new StringBuilder();
        AST.Visitor errorVisitor = new AST.Visitor() {
            @Override
            public boolean visit(AST.Node n){
                if(n.getLeft() != null && n.getRight() != null) {
                    startOfError.append( "( " );
                }
                boolean continueVisit = true;
                if(n.getLeft() != null) {
                    continueVisit = n.getLeft().accept( this );
                }
                if(continueVisit && n.getValue() != null){
                    boolean isTarget = ident.equals(n.getValue());
                    startOfError.append(isTarget ? "<" : " ")
                            .append(n.getValue().toString())
                            .append(isTarget ? ">" : " ");
                }
                if(continueVisit && n.getRight() != null){
                    continueVisit = n.getRight().accept( this );
                    if(continueVisit && n.getLeft() != null && n.getRight() != null){
                        startOfError.append( " )" );
                    }
                }

                return continueVisit;
            }
        };
        ast.getRoot().accept( errorVisitor );
        return startOfError.toString();
    }

    public static DatasetModel datasetFactory(ResultSet rs, ModelProvider modelProvider, 
            List<String> includedMetadata) throws SQLException{
        DatasetModel.Builder builder = modelProvider.getDatasetBuilder();

        String name = rs.getString("name");
        builder.pk(rs.getLong("pk"));
        builder.parentPk(rs.getLong("parent"));
        builder.name(name);
        builder.path(PathUtils.resolve(rs.getString( "containerpath"), name));
        builder.fileFormat(rs.getString("fileformat"));
        builder.dataType(rs.getString("datatype"));
        builder.created(rs.getTimestamp("created"));
        
        long versionPk = rs.getLong("datasetversion");
        builder.versionPk(versionPk);
        builder.versionId(rs.getInt("versionid"));
        builder.latest(rs.getBoolean( "latest"));
        
        ArrayList<DatasetLocationModel> locations = new ArrayList<>();
        HashMap<String, Object> metadata = new HashMap<>();
        
        for(String s: includedMetadata){
            Object o = rs.getObject(s);
            if(o != null){
                if(o instanceof Number){
                    BigDecimal v = rs.getBigDecimal(s);
                    o = v.scale() == 0 ? v.toBigIntegerExact() : v;
                }
                metadata.put(s, o);
            }
        }
        
        while(!rs.isClosed() && rs.getInt("datasetversion") == versionPk){
            DatasetLocationModel next = processLocation(rs, modelProvider);
            if(next != null){
                locations.add(next);
            }
            if(!rs.next()){
                rs.close();
            }
        }
        builder.locations(locations);
        builder.metadata( metadata );
        return builder.build();
    }
    
    private static DatasetLocationModel processLocation(ResultSet rs, 
            ModelProvider modelProvider) throws SQLException{
        DatasetLocation.Builder builder = (DatasetLocation.Builder) modelProvider.getLocationBuilder();
        Long pk = rs.getLong("datasetlocation");
        if(rs.wasNull()){
            return null;
        }
        builder.pk(pk);
        builder.site(rs.getString("site"));
        builder.resource(rs.getString("path"));
        builder.runMin(rs.getLong("runmin"));
        builder.runMax(rs.getLong("runmax"));
        builder.eventCount(rs.getLong("eventCount"));
        builder.size(rs.getLong("fileSizeBytes"));
        BigDecimal bd = rs.getBigDecimal("checksum");
        if(bd != null){
            builder.checksum(bd.unscaledValue().toString(16));
        }
        builder.master(rs.getBoolean( "master"));
        return builder.build();
    }
    
    public static MetanameContext buildDatasetMetaInfoGlobalContext(Connection conn) throws IOException {
        
        String sql = "select metaname, ValueType, prefix " +
                        "    from DatasetMetaInfo vx " +
                        "    left outer join ( " +
                        "            select substr(v1.metaname,0,4) prefix,  " +
                        "                    count(v1.metaname) prefixcount " +
                        "            from  " +
                        "            DatasetMetaInfo v1 " +
                        "            group by substr(v1.metaname,0,4) " +
                        "            having count(v1.metaname) > 5 " +
                        "    ) v0 on substr(vx.metaname,0,4) = prefix " +
                        "    order by prefix asc ";
        try (PreparedStatement stmt = conn.prepareStatement( sql )) {
            ResultSet rs = stmt.executeQuery();

            MetanameContext dmc = new MetanameContext();
            ArrayList<String> postfixes = new ArrayList<>();
            String lastPrefix = null;
            while(rs.next()){
                String maybePrefix = rs.getString( "prefix");
                String metaname = rs.getString( "metaname");
                String valueType = rs.getString( "ValueType");
                Class type = String.class;
                switch (valueType){
                    case "S":
                        type = String.class;
                        break;
                    case "N":
                        type = Number.class;
                        break;
                    case "T":
                        type = Timestamp.class;
                        break;
                    default:
                        break;
                }
                
                if(maybePrefix != null && !maybePrefix.isEmpty()){
                    if (lastPrefix != null && !lastPrefix.equals( maybePrefix) ){
                        dmc.add( new MetanameContext.Entry(lastPrefix, postfixes, lastPrefix.length(), type ));
                        postfixes.clear();
                    }
                    lastPrefix = maybePrefix;
                    postfixes.add( metaname );
                } else {
                    dmc.add( new MetanameContext.Entry( metaname , type ));
                }
            }
            return dmc;
        } catch (SQLException e){
            throw new IOException("Error retrieving metadata for search", e);
        }
    }
    
    public static MetanameContext buildGroupMetanameContext(Connection conn) throws SQLException{
        String sql = "select metaname from DatasetGroupMetaName";
        return buildContainerMetanameContext( conn, sql );
    }

    public static MetanameContext buildFolderMetanameContext(Connection conn) throws SQLException{
        String sql = "select metaname from LogicalFolderMetaName";
        return buildContainerMetanameContext( conn, sql );
    }

    protected static MetanameContext buildContainerMetanameContext(Connection conn, String sql) throws SQLException{
        try(PreparedStatement stmt = conn.prepareStatement( sql )) {
            ResultSet rs = stmt.executeQuery();
            MetanameContext mnc = new MetanameContext();
            while(rs.next()){
                mnc.add( new MetanameContext.Entry( rs.getString( "metaname" ) ) );
            }
            return mnc;
        }
    }
    
    public static void populateParentTempTable(Connection conn, 
            LinkedList<DatacatNode> containers) throws SQLException {

        if(conn.getMetaData().getDatabaseProductName().contains("MySQL")){
            String dropSql = "drop temporary table if exists ContainerSearch";
            String tableSql = 
                    "create temporary table ContainerSearch ( "
                    + "    DatasetLogicalFolder bigint, "
                    + "    DatasetGroup         bigint, "
                    + "    ContainerPath varchar(500) "
                    + ")";
            try (PreparedStatement stmt  = conn.prepareStatement(dropSql)){
                stmt.execute();
            }
            try (PreparedStatement stmt  = conn.prepareStatement(tableSql)){
                stmt.execute();
            }
        }
        
        String sql = "INSERT INTO ContainerSearch (DatasetLogicalFolder, DatasetGroup, ContainerPath) VALUES (?,?,?)";
        try (PreparedStatement stmt  = conn.prepareStatement(sql)){
            while(containers.peek() != null){
                DatacatRecord file = containers.remove();
                boolean isGroup = file.getType() == RecordType.GROUP;
                stmt.setNull( isGroup ? 1 : 2, Types.VARCHAR);
                stmt.setLong( isGroup ? 2 : 1, file.getPk());
                stmt.setString( 3, file.getPath());
                stmt.executeUpdate();
            }
        }
    }
    
    /*
    public static void pruneParentTempTable(Connection conn, 
    DirectoryWalker.ContainerVisitor visitor) throws SQLException {
        String sql = 
                "DELETE FROM ContainerSearch "
                + " WHERE (DatasetLogicalFolder is not null AND DatasetLogicalFolder NOT IN (%s)) "
                + " OR (DatasetGroup is not null AND DatasetGroup NOT IN (%s))";
        try (PreparedStatement stmt = conn.prepareStatement( sql )){
            while(visitor.files.peek() != null){
                DatacatRecord file = visitor.files.remove();
                boolean isGroup = file.getType() == RecordType.GROUP;
                stmt.setNull( isGroup ? 1 : 2, Types.VARCHAR);
                stmt.setLong( isGroup ? 2 : 1, file.getPk());
                stmt.setString( 3, file.getPath());
                stmt.executeUpdate();
            }
        }
    }
    */
    
    public static DirectoryStream<DatasetModel> getResults(final Connection conn, final ModelProvider modelProvider, 
            final Select sel, final List<String> metadataNames) throws SQLException{
        final PreparedStatement stmt = sel.prepareAndBind(conn);
        final ResultSet rs = stmt.executeQuery();
        if(!rs.next()){
            rs.close();
        }
        DirectoryStream<DatasetModel> stream = new DirectoryStream<DatasetModel>() {
            Iterator<DatasetModel> iter = null;

            @Override
            public Iterator<DatasetModel> iterator(){
                if(iter == null){
                    iter = new Iterator<DatasetModel>() {
                        private DatasetModel ds = null;
                        @Override
                        public boolean hasNext(){
                            try {
                                if(ds == null){
                                    if(rs.isClosed()){
                                        return false;
                                    }
                                    ds = SearchUtils.datasetFactory(rs, modelProvider, metadataNames);
                                    return true;
                                }
                                return true;
                            } catch(NoSuchElementException ex) {
                                return false;
                            } catch(SQLException ex){
                                throw new RuntimeException("Error processing search results", ex);
                            }
                        }

                        @Override
                        public DatasetModel next(){
                            if(!hasNext()){
                                throw new NoSuchElementException();
                            }
                            DatasetModel ret = ds;
                            ds = null;
                            return ret;
                        }

                        @Override
                        public void remove(){
                            throw new UnsupportedOperationException();
                        }

                    };
                }
                return iter;
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

        return stream;
    }
       
    public static Class<?> getParamType(Object tRight){
        if(tRight instanceof List){
            List r = ((List) tRight);
            tRight = Collections.checkedList( r, r.get( 0 ).getClass() ).get( 0 );
        }
        if(tRight instanceof Number){
            return Number.class;
        }
        if(tRight instanceof Class){
            return (Class<?>) tRight;
        }
        return tRight.getClass();
    }
}
