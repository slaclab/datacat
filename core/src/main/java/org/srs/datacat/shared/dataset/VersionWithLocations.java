
package org.srs.datacat.shared.dataset;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.dataset.VersionWithLocations.Builder;

/**
 *
 * @author bvan
 */
@XmlRootElement
@XmlType(name="versionWithLocations")
@JsonTypeName(value="version#withLocations")
@JsonDeserialize(builder = Builder.class)
public class VersionWithLocations extends DatasetVersion {
    private HashMap<String, DatasetLocation> dsLocations = new HashMap<>(3);
    
    private VersionWithLocations(){}
    
    public VersionWithLocations(DatasetVersion version){
        super(version);
        if(version instanceof VersionWithLocations){
            this.dsLocations = new HashMap<>(dsLocations);
        }
    }
    
    public VersionWithLocations(Builder builder){
        super(builder);
        if(builder.locations != null){
            initLocations(builder.locations);
        }
    }
    
    private void initLocations(Collection<DatasetLocation> locations){
        for(DatasetLocation l: locations){
            if(l.isMaster()){
                dsLocations.put( DatasetView.CANONICAL_SITE, l );
            }
            dsLocations.put( l.getSite(), l );
        }
    }
    
    @XmlElement(name="locations", required=false)
    public Set<DatasetLocation> getLocations(){
        return new HashSet<>(dsLocations.values());
    }
    
    public DatasetLocation getLocation(String name){
        return dsLocations.get( name );
    }

    /**
     * Return the master location if it exists. Otherwise, return the location with the highest PK,
     * or null
     */
    @XmlTransient 
    public DatasetLocation getPriorityLocation(){
        DatasetLocation maxLocation = null;
        for(DatasetLocation loc: new HashSet<>(dsLocations.values())){
            if(loc.isMaster() != null && loc.isMaster()){
                return loc;
            } else if(maxLocation == null){
                maxLocation = loc;
            } else if(loc.getPk() != null && loc.getPk() > maxLocation.getPk()){
                maxLocation = loc;
            }
        }
        return maxLocation;
    }

    @XmlTransient
    public static class Builder extends DatasetVersion.Builder {
        public Collection<DatasetLocation> locations = null;
        
        public Builder(){}
        public Builder(Dataset.Builder builder){
            super(builder);
            this.locations = builder.locations;
        }
        
        public Builder(DatasetVersion version){
            super(version);
            if(version instanceof VersionWithLocations){
                this.locations = ((VersionWithLocations) version).getLocations();
            }
        }

        @JsonSetter 
        public Builder locations(Collection<DatasetLocation> val) { this.locations = val; return this; }

        @Override
        public VersionWithLocations build(){
            return new VersionWithLocations(this);
        }

    }

}
