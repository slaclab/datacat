
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
import org.srs.datacat.model.HasDatasetViewInfo;
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
public class FullDataset extends Dataset implements DatasetVersionModel, HasDatasetViewInfo {
    private DatasetViewInfo viewInfo;
    
    private FullDataset(){}
    
    /**
     * Copy constructor. Can convert other datasets, if needed.
     * @param dataset 
     */
    public FullDataset(Dataset dataset){
        super(dataset);
        if(dataset instanceof HasDatasetViewInfo){
            this.viewInfo = ((HasDatasetViewInfo) dataset).getDatasetViewInfo();
        } else {
            this.viewInfo = null;
        }
    }

    public FullDataset(Dataset.Builder builder){
        super(builder);
        this.viewInfo = new DatasetViewInfo(builder.version, builder.locations);
    }
    
    @XmlTransient
    public DatasetVersion getVersion(){
        return viewInfo.getVersion();
    }
    
    @Override
    @XmlTransient
    public DatasetViewInfo getDatasetViewInfo(){
        return this.viewInfo;
    }
    
    @XmlElement(required=false)
    public Long getVersionPk(){
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getPk(); 
        }
        return null;
    }
    
    @XmlElementWrapper(name="versionMetadata")
    @XmlElement(required=false, name="entry")
    @JsonProperty("versionMetadata")
    public List<MetadataEntry> getVersionMetadata(){ 
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getMetadata();
        }
        return null; 
    }
    
    @Override
    @XmlElement(required=false)
    public String getDatasetSource(){ 
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getDatasetSource();
        }
        return null; 
    }

    @Override
    @XmlElement(required=false)
    public Boolean isLatest(){ 
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().isLatest(); 
        }
        return null;
    }

    @Override
    @XmlElement(required=false)
    public Long getProcessInstance(){ 
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getProcessInstance();
        }
        return null; }

    @Override
    @XmlElement(required=false)
    public String getTaskName(){ 
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getTaskName();
        }
        return null; 
    }

    @Override
    @XmlElement(required=false)
    public Integer getVersionId(){ 
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getVersionId();
        }
        return null; 
    }

    @XmlElement(name="versionCreated", required=false)
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    public Timestamp getDateVersionCreated(){ 
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getDateCreated();
        }
        return null; 
    }
    
    @XmlElement(required=false)
    public Set<DatasetLocation> getLocations(){
        return viewInfo.getLocations();
    }

    @XmlTransient
    public static class Builder extends Dataset.Builder{
        
        public Builder(){}
        
        public Builder(Dataset.Builder builder){
            super(builder);
        }
        
        @Override
        public FullDataset build(){
            return new FullDataset( this );
        }
    }
    
}
