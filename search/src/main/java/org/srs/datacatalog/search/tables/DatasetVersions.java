
package org.srs.datacatalog.search.tables;

import java.sql.Timestamp;
import java.util.Map;
import org.srs.datacat.model.DatasetView;
import org.zerorm.core.Column;
import org.zerorm.core.Param;
import org.zerorm.core.Select;
import org.zerorm.core.Val;
import org.zerorm.core.primaries.Case;

/**
 *
 * @author bvan
 */
public class DatasetVersions extends MetajoinedStatement {
    public VerDataset ds = new VerDataset().as( "ds", VerDataset.class );
    public Version v = new Version().as( "v", Version.class );
    public Location l = new Location().as( "l", Location.class );

    public DatasetVersions(DatasetView dsView){
        ds.dataset.as("pk");
        from( ds ).selection( ds.getColumns() )
                .selection( v.getColumns() )
                .selection( new Case(v.datasetVersion.eq(ds.latestVersion), new Val(1), new Val(0)).as("latest"))
                .selection( l.getColumns() )
                .selection( new Case(l.datasetLocation.eq( v.masterLocation), new Val(1), new Val(0)).as("master"));
        
        if(dsView.isCurrent()){
            leftOuterJoin( v, ds.latestVersion.eq( v.datasetVersion ) );
        } else {
            leftOuterJoin( v, ds.dataset.eq( v.dataset ) );
            Param<Integer> p = v.versionId.checkedParam( "vid", dsView.getVersionId());
            where(v.versionId.eq( p ) );
        }
        
        if(dsView.isCanonical()){
            leftOuterJoin( l, v.masterLocation.eq( l.datasetLocation ) );
        } else {
            leftOuterJoin( l, v.datasetVersion.eq( l.datasetVersion ) );
            if(!dsView.allSites()){
                Param<String> pl = l.datasetSite.checkedParam();
                pl.setValue(dsView.getSite());
                where( l.datasetSite.eq( pl ) );
            }
        }
    }

    @Override
    public Metatable getMetatableForType(String alias, Class type){
        Metatable ms = null;
        if(Number.class.isAssignableFrom( type ) || Boolean.class.isAssignableFrom( type )){
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
    
    @Override
    public Map<String, Select> getMetajoins(){
        return metaJoins;
    }

    @Override
    public Column getMetajoinColumn(){
        return v.datasetVersion;
    }
    
    @Override
    public String getMetanamePrefix(){
        return "dsmv";
    }
}
