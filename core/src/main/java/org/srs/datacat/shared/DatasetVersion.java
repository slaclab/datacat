
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.srs.datacat.model.DatasetVersionModel;
import java.sql.Timestamp;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.srs.datacat.model.DatasetView.VersionId;
import org.srs.datacat.shared.DatasetVersion.Builder;
import org.srs.rest.shared.RestDateAdapter;

/**
 *
 * @author bvan
 */
@XmlRootElement
@XmlType(name="version")
@JsonTypeName(value="version")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="$type", defaultImpl=DatasetVersion.class)
@JsonDeserialize(builder = Builder.class)
public class DatasetVersion extends DatacatObject implements DatasetVersionModel {
    private Integer versionId;
    private String datasetSource;
    private Long processInstance;
    private String taskName;
    private Boolean isLatest;
    private Timestamp versionRegistrationDate;
    
    public DatasetVersion(){ super(); }
    
    /**
     * Copy constructor
     * @param dataset 
     */
    public DatasetVersion(DatasetVersion dataset){
        super(dataset);
        this.versionId = dataset.versionId;
        this.datasetSource = dataset.datasetSource;
        this.processInstance = dataset.processInstance;
        this.taskName = dataset.taskName;
        this.isLatest = dataset.isLatest;
    }

    public DatasetVersion(Builder builder){
        super( builder );
        this.versionId = builder.versionId;
        this.datasetSource = builder.datasetSource;
        this.processInstance = builder.processInstance;
        this.taskName = builder.taskName;
        this.isLatest = builder.latest;
    }
    
    @Override
    public Integer getVersionId() { return this.versionId;}

    @Override
    @XmlTransient
    public String getDatasetSource() { return this.datasetSource;}
    
    @Override
    @XmlElement(required=false) 
    public Long getProcessInstance() { return this.processInstance;}
    
    @Override
    @XmlElement(required=false)
    public String getTaskName() { return this.taskName;}
    
    @Override
    @XmlElement(required=false)
    public Boolean isLatest() { return this.isLatest; }

    @Override
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    @XmlElement(name="created", required=false)
    public Timestamp getDateCreated() { return this.versionRegistrationDate;}

    @Override
    @XmlTransient
    public Timestamp getDateModified(){ return null; }
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder("DatasetVersion{ ");
        sb.append(super.toString()).append( "\t");
        sb.append(omitNull("versionId:", versionId));
        sb.append(omitNull("datasetSource:", datasetSource));
        sb.append(omitNull("processInstance:", processInstance));
        sb.append(omitNull("taskName:", taskName));
        sb.append(omitNull("isLatest:", isLatest));
        sb.append(omitNull("versionRegistrationDate:", versionRegistrationDate));
        sb.append("}");
        return sb.toString();
    }
    
    public void validateFields(){
        if(versionId == null){
            versionId = -1;
        }
    }
    
    @XmlTransient
    public static class Builder extends DatacatObject.Builder<Builder>{
        public Boolean latest;
        public String fileFormat;
        public String datasetDataType;
        public Integer versionId;
        public String datasetSource;
        public Long processInstance;
        public String taskName;
        public Timestamp created;
        public Timestamp modified;
        
        public Builder(){} 
        
        public Builder(DatasetVersion.Builder builder){
            super(builder);
            this.versionId = builder.versionId;
            this.datasetSource = builder.datasetSource;
            this.processInstance = builder.processInstance;
            this.taskName = builder.taskName;
            this.latest = builder.latest;
        }
        
        public Builder(DatasetVersion version){
            super(version);
            this.versionId = version.versionId;
            this.datasetSource = version.datasetSource;
            this.processInstance = version.processInstance;
            this.taskName = version.taskName;
            this.latest = version.isLatest;
        }

        public Builder(Dataset.Builder builder){
            this.pk = builder.versionPk;
            this.parentPk = builder.pk;
            this.metadata = builder.versionMetadata;
            this.numberMetadata = builder.versionNumberMetadata;
            this.stringMetadata = builder.versionStringMetadata;
            this.versionId = builder.versionId;
            this.datasetSource = builder.datasetSource;
            this.processInstance = builder.processInstance;
            this.taskName = builder.taskName;
            this.latest = builder.latest;
            this.created = builder.versionCreated;
            this.modified = builder.versionModified;
        }
        
        public static Builder create(){
            return new Builder();
        }

        @JsonIgnore public Builder versionId(Integer val){ this.versionId = val; return this; }
        @JsonSetter public Builder versionId(VersionId val){ this.versionId = val.getId(); return this; }
        public Builder datasetSource(String val){ this.datasetSource = val;  return this; }
        @JsonSetter public Builder processInstance(Long val){ this.processInstance = val; return this; }
        @JsonSetter public Builder taskName(String val){ this.taskName = val; return this; }
        @JsonSetter public Builder latest(Boolean val){ this.latest = val; return this; }
        @JsonSetter public Builder created(Timestamp val) { this.created = val; return this; }
        @JsonSetter public Builder modified(Timestamp val) { this.modified = val; return this; }

        @Override
        public DatasetVersion build(){
            return new DatasetVersion(this);
        }
    }
    
}
