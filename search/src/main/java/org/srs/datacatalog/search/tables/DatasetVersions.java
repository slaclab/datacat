
package org.srs.datacatalog.search.tables;

import java.sql.Timestamp;
import java.util.Map;
import org.zerorm.core.Column;
import org.zerorm.core.Expr;
import org.zerorm.core.Select;

/**
 *
 * @author bvan
 */
public class DatasetVersions extends MetajoinedStatement {
    public VerDataset ds = new VerDataset().as( "ds", VerDataset.class );
    public Version v = new Version().as( "v", Version.class );
    public Location l = new Location().as( "l", Location.class );

    private DatasetVersions(){
        from( ds ).selection( ds.columns() ).selection( v.columns() ).selection( l.columns() );
    }

    public void init(Expr datasetVersionExpr){
        leftOuterJoin( v, datasetVersionExpr ).
                leftOuterJoin( l, v.masterLocation.eq( l.datasetLocation ) );
    }

    @Override
    public Metatable getMetatableForType(String alias, Class type){
        Metatable ms = null;
        if(Number.class.isAssignableFrom( type )){
            ms = new DatasetMetanumber().as( alias, Metatable.class );
        }
        else if(String.class.isAssignableFrom( type )){
            ms = new DatasetMetastring().as( alias, Metatable.class );
        }
        else if(Timestamp.class.isAssignableFrom( type )){
            ms = new DatasetMetatimestamp().as( alias, Metatable.class );
        }
        return ms;
    }
    
    public static class LatestDatasetVersions extends DatasetVersions {        
        public LatestDatasetVersions(){
            super();
            init(ds.latestVersion.eq( v.datasetVersion ) );
        }
    }
    
    public static class AllDatasetVersions extends DatasetVersions {
        
        public AllDatasetVersions(){
            super();
            init( ds.dataset.eq( v.dataset ) );
        }
    }
    
    public static class SpecificDatasetVersions extends DatasetVersions {
        
        public SpecificDatasetVersions(Long version){
            super();
            init( ds.dataset.eq( v.dataset ) );
            if(version != null){
                where(v.datasetVersion.eq( v ) );
            }
        }
    }
    
    @Override
    public Map<String, Select> getMetajoins(){
        return metaJoins;
    }

    @Override
    public Column getMetajoinColumn(){
        return l.datasetVersion;
    }
    
    @Override
    public String getMetanamePrefix(){
        return "dsmv";
    }
}
