
package org.srs.datacat.shared.dataset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.srs.datacat.model.DatasetVersionModel;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.dataset.FullDataset.Builder;
import org.srs.rest.shared.RestDateAdapter;
import org.srs.rest.shared.metadata.MetadataEntry;

/**
 * Dataset with a Version, which also must contain locations
 * @author bvan
 */
@XmlRootElement
@XmlType(name="fullDataset")
@JsonTypeName(value="dataset#full")
@JsonDeserialize(builder = Builder.class)
public class FullDataset extends Dataset implements DatasetVersionModel {
    private DatasetVersion dsVersion;
    
    private FullDataset(){}
    
    /**
     * Copy constructor. Can convert other datasets, if needed.
     * @param dataset 
     */
    public FullDataset(Dataset dataset){
        super(dataset);
        if(dataset instanceof FullDataset){
            FullDataset ds = ((FullDataset) dataset);
            if(ds.dsVersion instanceof VersionWithLocations){
                this.dsVersion = new VersionWithLocations(ds.dsVersion);
            } else {
                this.dsVersion = new DatasetVersion(ds.dsVersion);
            }
        }
        else if(dataset instanceof FlatDataset){
            FlatDataset ds = ((FlatDataset) dataset);
            if(ds.getVersion() instanceof VersionWithLocations){
                this.dsVersion = new VersionWithLocations(ds.getVersion());
            } else {
                this.dsVersion = new DatasetVersion(ds.getVersion());
            }
        }
    }

    public FullDataset(Dataset.Builder builder){
        super(builder);
        dsVersion = builder.version;
    }
    
    @XmlTransient
    public DatasetVersion getVersion(){
        return dsVersion;
    }
    
    @XmlElement(required=false)
    public Long getVersionPk(){ return dsVersion.getPk(); }
    
    @Override
    @XmlElement(required=false)
    public Integer getVersionId(){ return dsVersion.getVersionId(); }

    @Override
    @XmlElement(required=false)
    public Boolean isLatest(){ return dsVersion.isLatest(); }

    @XmlElement(name="versionCreated", required=false)
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    public Timestamp getDateVersionCreated(){ return dsVersion.getDateCreated(); }
    
    @XmlElement(required=false)
    public Set<DatasetLocation> getLocations(){
        if(dsVersion instanceof VersionWithLocations){
            return ((VersionWithLocations) dsVersion).getLocations();
        }
        return null;
    }

    @Override
    @XmlElement(required=false)
    public String getDatasetSource(){ return dsVersion.getDatasetSource(); }
    
    @XmlElementWrapper(name="versionMetadata")
    @XmlElement(required=false, name="entry")
    @JsonProperty("versionMetadata")
    public List<MetadataEntry> getVersionMetadata(){ return dsVersion.getMetadata(); }
    
    @Override
    @XmlElement(required=false)
    public Long getProcessInstance(){ return dsVersion.getProcessInstance(); }

    @Override
    @XmlElement(required=false)
    public String getTaskName(){ return dsVersion.getTaskName(); }

    @XmlTransient
    public static class Builder extends Dataset.Builder{
        
        public Builder(){}
        
        public Builder(Dataset.Builder builder){
            super(builder);
        }
        
        @Override
        public FullDataset build(){
            if(version == null){
                VersionWithLocations dv = null;
                if((dsType & VERSION) > 0){
                    dv = new VersionWithLocations(new VersionWithLocations.Builder(this));
                }
                version( dv );
            } else if(!(version instanceof VersionWithLocations)){
                VersionWithLocations.Builder builder = new VersionWithLocations.Builder(version);
                builder.locations(locations);
                version(builder.build());
            }
            return new FullDataset( this );
        }
    }
    
}
