
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.srs.datacat.model.dataset.DatasetVersionModel;
import java.sql.Timestamp;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Objects;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.DatasetView.VersionId;
import org.srs.datacat.shared.DatasetVersion.Builder;

/**
 * A DatasetVersion represents a specific version for a given Dataset.
 * When combined with one or more DatasetLocations, we can obtain a DatasetView.
 * 
 * @author bvan
 */
@JsonTypeName(value="version")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="_type", defaultImpl=DatasetVersion.class)
@JsonDeserialize(builder = Builder.class)
public class DatasetVersion extends DatacatObject implements DatasetVersionModel {
    private Integer versionId;
    private String datasetSource;
    private Long processInstance;
    private String taskName;
    private Boolean isLatest;
    private Timestamp versionRegistrationDate;
    
    public static final DatasetVersion NEW_VERSION = 
            new DatasetVersion.Builder().versionId( DatasetView.NEW_VER).build();
    public static final DatasetVersion CURRENT_VERSION = 
            new DatasetVersion.Builder().versionId( DatasetView.CURRENT_VER).build();
    
    public DatasetVersion(){ super(); }
    
    /**
     * Copy constructor.
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
    public Integer getVersionId() { return this.versionId; }

    @Override
    @JsonIgnore
    public String getDatasetSource() { return this.datasetSource; }
    
    @Patchable(column="ProcessInstance")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getProcessInstance() { return this.processInstance; }

    @Patchable(column="TaskName")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getTaskName() { return this.taskName; }
    
    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Boolean isLatest() { return this.isLatest; }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Timestamp getDateCreated() { return this.versionRegistrationDate; }

    @Override
    @JsonIgnore
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
        Objects.requireNonNull(versionId, "Version ID required");
    }
    
    /**
     * Builder.
     */
    public static class Builder extends DatacatObject.Builder<Builder> implements DatasetVersionModel.Builder<Builder> {
        public Boolean latest;
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
        
        public Builder(DatasetVersionModel version){
            super(version);
            this.versionId = version.getVersionId();
            this.datasetSource = version.getDatasetSource();
            this.latest = version.isLatest();
            
            if(version instanceof DatasetVersion){
                this.processInstance = ((DatasetVersion) version).processInstance;
                this.taskName = ((DatasetVersion) version).taskName;
            }
        }

        public Builder(Dataset.Builder builder){
            this.pk = builder.versionPk;
            this.parentPk = builder.pk;
            this.metadata = builder.versionMetadata;
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

        @Override
        public Builder create(DatasetVersionModel val){
            return new Builder(val);
        }
    }
    
}
