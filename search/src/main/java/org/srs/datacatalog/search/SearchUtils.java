
package org.srs.datacatalog.search;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.freehep.commons.lang.AST;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.dataset.FlatDataset;
import org.srs.datacat.shared.dataset.FullDataset;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DirectoryWalker;
import org.srs.vfs.PathUtils;
import org.zerorm.core.Select;

/**
 *
 * @author Brian Van Klaveren<bvan@slac.stanford.edu>
 */
public class SearchUtils {
    
    
    public static String getErrorString(AST ast, final String ident){
        if(ast.toString().length() < 32){
            return ast.toString();
        }
        
        final StringBuilder startOfError = new StringBuilder();
        AST.Visitor errorVisitor = new AST.Visitor() {
            public boolean visit(AST.Node n){
                if(n.getLeft() != null && n.getRight() != null) {
                    startOfError.append( "( " );
                }
                boolean continueVisit = true;
                if(n.getLeft() != null) {
                    continueVisit = n.getLeft().accept( this );
                }
                if(continueVisit && n.getValue() != null){
                    continueVisit = !ident.equals( n.getValue() );
                    startOfError.append( " " + n.getValue().toString() + " " );
                }
                if(continueVisit && n.getRight() != null){
                    continueVisit = n.getRight().accept( this );
                    if(continueVisit && n.getLeft() != null && n.getRight() != null){
                        startOfError.append( " )" );
                    }
                }

                if(!continueVisit){
                    int partial = ident.length() + 25;
                    startOfError.delete( 0, startOfError.length() - partial );
                    startOfError.insert( 0, "..." );
                }
                return continueVisit;
            }
        };
        ast.getRoot().accept( errorVisitor );
        return startOfError.toString();
    }

    public static Dataset datasetFactory(ResultSet rs, DatasetView dsView, List<String> includedMetadata) throws SQLException{
        Dataset.Builder builder;
        if(dsView.isAll()){
            builder = new FullDataset.Builder();
        } else {
            builder = new FlatDataset.Builder();
        }

        String name = rs.getString("name");
        builder.pk(rs.getLong("pk"));
        builder.parentPk(rs.getLong("parent"));
        builder.name(name);
        builder.path(PathUtils.resolve(rs.getString( "containerpath"), name));
        builder.fileFormat(rs.getString("fileformat"));
        builder.dataType(rs.getString("datatype"));
        
        long versionPk = rs.getLong("datasetversion");
        builder.versionPk(versionPk);
        builder.versionId(rs.getInt("versionid"));
        builder.latest(rs.getBoolean( "latest"));
        
        ArrayList<DatasetLocation> locations = new ArrayList<>();
        HashMap<String, Object> metadata = new HashMap<>();
        
        for(String s: includedMetadata){
            Object o = rs.getObject(s);;
            if(o != null){
                if(o instanceof BigDecimal){
                    BigDecimal v = (BigDecimal) o;
                    o = v.scale() == 0 ? v.toBigIntegerExact() : v;
                }
                metadata.put(s, o);
            }
        }
        
        while(!rs.isClosed() && rs.getInt("datasetversion") == versionPk){
            DatasetLocation next = processLocation(rs);
            if(next != null){
                locations.add(next);
            }
            if(!rs.next()){
                rs.close();
            }
        }
        
        if(builder instanceof FullDataset.Builder){
            builder.locations(locations);
        } else if(!locations.isEmpty()){
            builder.location(locations.get(0));
        }

        builder.metadata( metadata );
        return builder.build();
    }
    
    private static DatasetLocation processLocation(ResultSet rs) throws SQLException{
        DatasetLocation.Builder builder = new DatasetLocation.Builder();
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
        builder.checksum(rs.getLong("checksum"));
        builder.master( rs.getBoolean( "master"));
        return builder.build();
    }
    
    public static MetanameContext buildMetanameGlobalContext(Connection conn) throws SQLException {
        
        String sql = "select metaname, prefix " +
                        "    from datasetmetaname vx " +
                        "    left outer join ( " +
                        "            select substr(v1.metaname,0,4) prefix,  " +
                        "                    count(v1.metaname) prefixcount " +
                        "            from  " +
                        "            datasetmetaname v1 " +
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
                if(maybePrefix != null){
                    if (lastPrefix != null && !lastPrefix.equals( maybePrefix) ){
                        dmc.add( new MetanameContext.Entry(lastPrefix, postfixes, lastPrefix.length() ));
                        postfixes.clear();
                    }
                    lastPrefix = maybePrefix;
                    postfixes.add( metaname );
                } else {
                        dmc.add( new MetanameContext.Entry( metaname ));
                }
            }
            return dmc;
        }
    }
    
    public static MetanameContext buildMetaInfoGlobalContext(Connection conn) throws SQLException {
        
        String sql = "select metaname, ValueType, prefix " +
                        "    from datasetmetainfo vx " +
                        "    left outer join ( " +
                        "            select substr(v1.metaname,0,4) prefix,  " +
                        "                    count(v1.metaname) prefixcount " +
                        "            from  " +
                        "            datasetmetainfo v1 " +
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
                Class type = null;
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
                        type = String.class;
                }
                
                if(maybePrefix != null){
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
        }
    }
    
    public static MetanameContext buildGroupMetanameContext(Connection conn) throws SQLException{
        String sql = "select metaname from datasetgroupmetaname";
        return buildContainerMetanameContext( conn, sql );
    }

    public static MetanameContext buildFolderMetanameContext(Connection conn) throws SQLException{
        String sql = "select metaname from logicalfoldermetaname";
        return buildContainerMetanameContext( conn, sql );
    }

    protected static MetanameContext buildContainerMetanameContext(Connection conn, String sql)
            throws SQLException{
        try(PreparedStatement stmt = conn.prepareStatement( sql )) {
            ResultSet rs = stmt.executeQuery();
            MetanameContext mnc = new MetanameContext();
            while(rs.next()){
                mnc.add( new MetanameContext.Entry( rs.getString( "metaname" ) ) );
            }
            return mnc;
        }
    }
    
    public static void populateParentTempTable(Connection conn, DirectoryWalker.ContainerVisitor visitor) throws SQLException {
        String sql = "INSERT INTO ContainerSearch (DatasetLogicalFolder, DatasetGroup, ContainerPath) VALUES (?,?,?)";
        try (PreparedStatement stmt  = conn.prepareStatement( sql )){
            while(visitor.files.peek() != null){
                DcFile file = visitor.files.remove();
                boolean isGroup = file.getType() instanceof DcFile.GroupType;
                stmt.setNull( isGroup ? 1 : 2, Types.VARCHAR);
                stmt.setLong( isGroup ? 2 : 1, file.fileKey());
                stmt.setString( 3, file.getPath().toString());
                stmt.executeUpdate();
            }
        }
        /*try (PreparedStatement stmt2 = conn.prepareStatement( "SELECT ContainerPath FROM ContainerSearch")){
            ResultSet rs = stmt2.executeQuery();
            while(rs.next()){
                System.out.println(rs.getString("ContainerPath"));
            }
        }*/

    }
    
    public static void pruneParentTempTable(Connection conn, DirectoryWalker.ContainerVisitor visitor) throws SQLException {
        String sql = 
                "DELETE FROM ContainerSearch "
                + " WHERE (DatasetLogicalFolder is not null AND DatasetLogicalFolder NOT IN (%s)) "
                + " OR (DatasetGroup is not null AND DatasetGroup NOT IN (%s))";
        try (PreparedStatement stmt = conn.prepareStatement( sql )){
            while(visitor.files.peek() != null){
                DcFile file = visitor.files.remove();
                boolean isGroup = file.getType() instanceof DcFile.GroupType;
                stmt.setNull( isGroup ? 1 : 2, Types.VARCHAR);
                stmt.setLong( isGroup ? 2 : 1, file.fileKey());
                stmt.setString( 3, file.getPath().toString());
                stmt.executeUpdate();
            }
        }
    }
    
    public static List<Dataset> getResults(final Connection conn, final Select sel, DatasetView dsView, List<String> metadataNames) throws SQLException{
        ArrayList<Dataset> datasets = new ArrayList<>();        
        try(PreparedStatement stmt = sel.prepareAndBind( conn )){
            ResultSet rs = stmt.executeQuery();
            if(!rs.next()){
                rs.close();
            }
            while(!rs.isClosed()) {
                datasets.add(SearchUtils.datasetFactory(rs, dsView, metadataNames));
            }
        }
        return datasets;
    }
    
    /*
    public static List<Dataset> getResultsDeferredFill(final Connection conn, final Select sel, final boolean keepAlive) throws SQLException{
        
        final Iterator<Dataset> iter = new Iterator<Dataset>() {
            PreparedStatement stmt = sel.prepareAndBind( conn );
            ResultSet rs = stmt.executeQuery();
            boolean advance = true;
            boolean hasNext = false;

            @Override
            public boolean hasNext(){
                try {
                    if(advance){
                        hasNext = rs.next();
                        advance = false;
                    }
                    if(!hasNext){
                        if(stmt != null){ stmt.close(); }
                        if(conn != null && !keepAlive){ conn.close(); }
                    }
                } catch(SQLException ex) { throw new RuntimeException("Error handling list",ex);}
                return hasNext;
            }

            @Override
            public Dataset next(){
                advance = hasNext() == true ? true : false;
                try {
                    return SearchUtils.datasetFactory(rs, null);
                } catch(SQLException ex) { throw new RuntimeException("Error handling list", ex);}
            }

            @Override
            public void remove(){ throw new UnsupportedOperationException( "Not implemented" ); }

            // Last chance effort to try to close the statement
            @Override
            protected void finalize() throws Throwable{
                try {
                    if(stmt != null){ stmt.close(); }
                    if(conn != null && !keepAlive){ conn.close(); }
                } catch(SQLException ex) { }
                super.finalize();
            }
        };
        
        return new ArrayList<Dataset>(){
            boolean initialized = false;
            @Override
            public Iterator iterator(){ return !initialized ? iter: super.iterator(); }
            
            @Override
            public Dataset get(int index){
                verifyInitialized();
                return super.get( index );
            }

            @Override
            public int size(){
                verifyInitialized();
                return super.size();
            }

            @Override
            public Object[] toArray(){
                verifyInitialized();
                return super.toArray();
            }
            
            public void verifyInitialized(){
                if(!initialized){
                    while(iter.hasNext()){
                        add(iter.next());
                    }
                }
                initialized = true;
            }
        };
    }*/
    
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
