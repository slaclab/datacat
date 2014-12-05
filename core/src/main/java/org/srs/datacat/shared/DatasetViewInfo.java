package org.srs.datacat.shared;

import com.google.common.base.Optional;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.srs.datacat.model.DatasetLocationModel;
import org.srs.datacat.model.DatasetVersionModel;
import org.srs.datacat.model.DatasetView;

/**
 *
 * @author bvan
 */
public class DatasetViewInfo {

    private final Optional<DatasetVersionModel> versionOpt;
    private final Optional<Set<DatasetLocationModel>> locationOpt;

    public DatasetViewInfo(DatasetVersionModel version, DatasetLocationModel location){
        this.versionOpt = Optional.fromNullable(version);
        if(location != null){
            Set<DatasetLocationModel> loc = new HashSet<>(Arrays.asList(location));
            this.locationOpt = Optional.of(loc);
        } else {
            this.locationOpt = Optional.absent();
        }
    }

    public DatasetViewInfo(DatasetVersionModel version, Collection<DatasetLocationModel> locations){
        this.versionOpt = Optional.fromNullable(version);
        if(locations != null && !locations.isEmpty()){
            Set<DatasetLocationModel> locs = new HashSet<>(locations);
            this.locationOpt = Optional.of(locs);
        } else {
            this.locationOpt = Optional.absent();
        }
    }

    private HashMap<String, DatasetLocationModel> toMap(Collection<DatasetLocationModel> locations){
        HashMap<String, DatasetLocationModel> locationMap = new HashMap<>();
        for(DatasetLocationModel l: locations){
            if(l.isMaster() != null && l.isMaster()){
                locationMap.put(DatasetView.CANONICAL_SITE, l);
            }
            locationMap.put(l.getSite(), l);
        }
        return locationMap;
    }

    public DatasetVersionModel getVersion(){
        return versionOpt.orNull();
    }

    public DatasetLocationModel getLocation(DatasetView view){
        if(locationOpt.isPresent()){
            return getLocation(view.getSite());
        }
        return null;
    }

    public DatasetLocationModel getLocation(String site){
        if(locationOpt.isPresent()){
            return toMap(locationOpt.get()).get(site);
        }
        return null;
    }

    public Set<DatasetLocationModel> getLocations(){
        if(locationOpt.isPresent()){
            return locationOpt.get();
        }
        return null;
    }

    public Optional<DatasetVersionModel> versionOpt(){
        return this.versionOpt;
    }

    public Optional<DatasetLocationModel> singularLocationOpt(){
        if(locationOpt.isPresent()){
            if(locationOpt.get().size() == 1){
                return Optional.fromNullable(locationOpt.get()
                        .toArray(new DatasetLocationModel[0])[0]);
            }
        }
        return Optional.absent();
    }

    public Optional<DatasetLocationModel> canonicalLocationOpt(){
        DatasetView view = new DatasetView(DatasetView.EMPTY_VER, DatasetView.CANONICAL_SITE);
        Optional<Set<DatasetLocationModel>> maybeLocations = fromView(view).locationsOpt();
        if(maybeLocations.isPresent() && !maybeLocations.get().isEmpty()){
            return Optional.fromNullable(maybeLocations.get().toArray(new DatasetLocationModel[0])[0]);
        }
        return Optional.absent();
    }

    public Optional<Set<DatasetLocationModel>> locationsOpt(){
        if(locationOpt.isPresent()){
            return locationOpt;
        }
        return Optional.absent();
    }

    public DatasetViewInfo fromView(DatasetView view){
        DatasetVersionModel retVersion = null;
        Collection<DatasetLocationModel> retLocations = null;

        if(versionOpt.isPresent()){
            switch(view.getVersionId()){
                case DatasetView.CURRENT_VER:
                    if(versionOpt.get().isLatest()){
                        retVersion = versionOpt.get();
                    }
                    break;
                case DatasetView.EMPTY_VER:
                case DatasetView.NEW_VER:
                    break;
                default:
                    if(view.getVersionId() == versionOpt.get().getVersionId()){
                        retVersion = versionOpt.get();
                    }
            }
        }

        if(!locationOpt.isPresent()){
            if(view.allSites() || view.zeroOrMoreSites()){
                retLocations = locationOpt.get();
            } else if(toMap(locationOpt.get()).containsKey(view.getSite())){
                retLocations = Arrays.asList(toMap(locationOpt.get()).get(view.getSite()));
            }
        }
        return new DatasetViewInfo(retVersion, retLocations);
    }
}
