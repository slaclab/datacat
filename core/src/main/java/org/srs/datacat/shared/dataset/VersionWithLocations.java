
package org.srs.datacat.shared.dataset;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
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
    private List<DatasetLocation> dsLocations;
    
    private VersionWithLocations(){}
    
    public VersionWithLocations(DatasetVersion version){
        super(version);
        if(version instanceof VersionWithLocations){
            List<DatasetLocation> locs = ((VersionWithLocations) version).dsLocations;
            this.dsLocations = new ArrayList<>(locs);
        } else {
            this.dsLocations = new ArrayList<>();
        }
    }
    
    public VersionWithLocations(Builder builder){
        super(builder);
        this.dsLocations = builder.locations;
    }
    
    @XmlElement(name="locations", required=false)
    public List<DatasetLocation> getLocations(){
        return dsLocations;
    }
    
    public DatasetLocation getLocation(String name){
        for(DatasetLocation loc: dsLocations){
            if(loc.getSite().equals( name )){
                return loc;
            }
        }
        return null;
    }

    /**
     * Return the master location if it exists. Otherwise, return the location with the highest PK,
     * or null
     */
    @XmlTransient 
    public DatasetLocation getPriorityLocation(){
        DatasetLocation maxLocation = null;
        for(DatasetLocation loc: dsLocations){
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
        public List<DatasetLocation> locations = null;
        
        public Builder(){}
        public Builder(DatasetBuilder builder){
            super(builder);
            this.locations = builder.locations;
        }

        @JsonSetter 
        public Builder locations(List<DatasetLocation> val) { this.locations = val; return this; }

        @Override
        public VersionWithLocations build(){
            return new VersionWithLocations(this);
        }

    }

}
