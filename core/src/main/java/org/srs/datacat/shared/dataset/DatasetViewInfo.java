
package org.srs.datacat.shared.dataset;

import com.google.common.base.Optional;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlTransient;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.HasDatasetViewInfo;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;

/**
 *
 * @author bvan
 */
@XmlTransient
public class DatasetViewInfo {
    
    private final Optional<DatasetVersion> optionaVersion;
    private final Optional<Set<DatasetLocation>> optionalLocations;
    
    public DatasetViewInfo(DatasetVersion version, DatasetLocation location){
        this.optionaVersion = Optional.fromNullable(version);
        if(location != null){
            Set<DatasetLocation> loc = new HashSet<>(Arrays.asList(location));
            this.optionalLocations = Optional.of(loc);
        } else {
            this.optionalLocations = Optional.absent();
        }
    }
    
    public DatasetViewInfo(DatasetVersion version, Collection<DatasetLocation> locations){
        this.optionaVersion = Optional.fromNullable(version);
        if(locations != null){
            Set<DatasetLocation> locs = new HashSet<>(locations);
            this.optionalLocations = Optional.of(locs);
        } else {
            this.optionalLocations = Optional.absent();
        }
    }
    
    private HashMap<String, DatasetLocation> toMap(Collection<DatasetLocation> locations){
        HashMap<String, DatasetLocation> locationMap = new HashMap<>();
        for(DatasetLocation l: locations){
            if(l.isMaster() != null && l.isMaster()){
                locationMap.put( DatasetView.CANONICAL_SITE, l );
            }
            locationMap.put(l.getSite(), l);
        }
        return locationMap;
    }
    
    public DatasetVersion getVersion(){
        return optionaVersion.orNull();
    }
    
    public DatasetLocation getLocation(DatasetView view){
        if(optionalLocations.isPresent()){
            return getLocation(view.getSite());
        }
        return null;
    }
    
    public DatasetLocation getLocation(String site){
        if(optionalLocations.isPresent()){
            return toMap(optionalLocations.get()).get(site);
        }
        return null;
    }
    
    public Set<DatasetLocation> getLocations(){
        if(optionalLocations.isPresent()){
            return optionalLocations.get();
        }
        return null;
    }
    
    public Optional<DatasetVersion> versionOpt(){
        return this.optionaVersion;
    }
    
    public Optional<DatasetLocation> singularLocationOpt(){
        if(optionalLocations.isPresent()){
            if(optionalLocations.get().size() == 1){
                return Optional.fromNullable(optionalLocations.get()
                        .toArray(new DatasetLocation[0])[0]);
            }
        }
        return Optional.absent();
    }
    
    public Optional<DatasetLocation> canonicalLocationOpt(){
        DatasetView view = new DatasetView(DatasetView.EMPTY_VER, DatasetView.CANONICAL_SITE);
        Optional<Set<DatasetLocation>> maybeLocations = fromView(view).locationsOpt();
        if(maybeLocations.isPresent() && !maybeLocations.get().isEmpty()){
            return Optional.fromNullable(maybeLocations.get().toArray(new DatasetLocation[0])[0]);
        }
        return Optional.absent();
    }

    public Optional<Set<DatasetLocation>> locationsOpt(){
        if(optionalLocations.isPresent()){
            return optionalLocations;
        }
        return Optional.absent();
    }
    
    public DatasetViewInfo fromView(DatasetView view){
        DatasetVersion retVersion = null;
        Collection<DatasetLocation> retLocations = null;
        
        if(optionaVersion.isPresent()){
            switch (view.getVersionId()){
                case DatasetView.CURRENT_VER:
                    if(optionaVersion.get().isLatest()){
                        retVersion = optionaVersion.get();
                    }
                    break;
                case DatasetView.EMPTY_VER:
                case DatasetView.NEW_VER:
                    break;
                default:
                    if(view.getVersionId() == optionaVersion.get().getVersionId()){
                        retVersion = optionaVersion.get();
                    }
            }
        }
        
        if(!optionalLocations.isPresent()){
            if(view.allSites() || view.zeroOrMoreSites()){
                retLocations = optionalLocations.get();
            } else if(toMap(optionalLocations.get()).containsKey(view.getSite())){
                retLocations = Arrays.asList(toMap(optionalLocations.get()).get(view.getSite()));
            }
        }
        return new DatasetViewInfo(retVersion, retLocations);
    }
}
