
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.srs.datacat.model.DatasetModel;
import java.sql.Timestamp;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.srs.datacat.model.DatasetLocationModel;
import org.srs.datacat.model.DatasetVersionModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.Dataset.Builder;
import org.srs.datacat.shared.adapters.RestDateAdapter;
import org.srs.datacat.shared.metadata.MetadataEntry;

/**
 * Represents an entire dataset. Subclasses may include information on all DatasetVersions.
 * @author bvan
 */
@JsonTypeName(value="dataset")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="_type", defaultImpl=Dataset.class)
@JsonDeserialize(builder=Builder.class)
@JsonPropertyOrder({"_type", "name",  "path", "pk", "parentPk",
    "metadata", "dataType", "fileFormat", "created"})
public class Dataset extends DatacatObject implements DatasetModel {
    
    private String fileFormat;
    private String dataType;
    private Timestamp dateCreated;
    
    public Dataset(){ super(); }
    
    public Dataset(DatacatObject o){ super(o); }
    
    /**
     * Copy constructor.
     *
     * @param dataset 
     */
    public Dataset(Dataset dataset){
        super(dataset);
        this.dataType = dataset.dataType;
        this.fileFormat = dataset.fileFormat;
        this.dateCreated = dataset.dateCreated;
    }
    
    public Dataset(DatacatObject.Builder builder){
        super(builder);
    }
    
    public Dataset(Builder builder){
        super(builder);
        this.dataType = builder.dataType;
        this.fileFormat = builder.fileFormat;
        this.dateCreated = builder.created;
    }
    
    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDataType() { return this.dataType; }
        
    @Override
    
    public String getFileFormat() { return this.fileFormat; }
    
    @Override
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    @JsonProperty("created")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Timestamp getDateCreated(){ return this.dateCreated; }
    
    @Override
    @JsonIgnore
    public Timestamp getDateModified(){
        return null;
    }
    
    @Override
    public String toString() {
        return super.toString() + "\tType: " + dataType + "\tCreated: " + dateCreated;
    }
   
    @JsonIgnore
    public List<DatasetView> getDatasetViews(){
        return Collections.singletonList(DatasetView.EMPTY);
    }

    /**
     * Dataset Builder.
     * 
     * @author bvan
     */
    public static class Builder extends DatacatObject.Builder<Builder> {

        public static final int NONE = 0;
        public static final int BASE = 1 << 1;
        public static final int VERSION = 1 << 2;
        //public static final int VERSIONS = 1 << 3;
        public static final int LOCATION = 1 << 4;
        public static final int LOCATIONS = 1 << 5;
        public static final int FLAT = BASE | VERSION | LOCATION;
        public static final int FULL = BASE | VERSION | LOCATIONS;
        //public static final int MULTI = BASE | VERSIONS | LOCATIONS;
        public int dsType = BASE;
        public DatasetVersionModel version;
        public DatasetLocationModel location;
        public HashMap<String, Object> versionMetadata = new HashMap<>();
        public List<DatasetVersion> versions;
        public Collection<DatasetLocationModel> locations;
        // mixins
        public Timestamp created;
        public Long versionPk;
        public Long locationPk;
        public Boolean latest;
        public Boolean master;
        public String fileFormat;
        public String dataType;
        public Integer versionId;
        public String datasetSource;
        public Long processInstance;
        public String taskName;
        public Timestamp versionCreated;
        public Timestamp versionModified;
        public Long size;
        public String resource;
        public String site;
        public Long eventCount;
        public Long runMin;
        public Long runMax;
        public Long checksum;
        public Timestamp locationCreated;
        public Timestamp locationModified;
        public Timestamp locationScanned;
        public String scanStatus;

        public Builder(){
            super();
        }

        /**
         * Copy constructor.
         * 
         * @param builder
         */
        public Builder(Builder builder){
            super( builder );
            this.dsType = builder.dsType;
            this.version = builder.version;
            this.location = builder.location;
            this.versions = builder.versions;
            this.locations = builder.locations;
            this.created = builder.created;
            this.versionPk = builder.versionPk;
            this.locationPk = builder.locationPk;
            this.latest = builder.latest;
            this.master = builder.master;
            this.fileFormat = builder.fileFormat;
            this.dataType = builder.dataType;
            this.versionId = builder.versionId;
            this.datasetSource = builder.datasetSource;
            this.processInstance = builder.processInstance;
            this.taskName = builder.taskName;
            this.versionCreated = builder.versionCreated;
            this.versionModified = builder.versionModified;
            this.versionMetadata = builder.versionMetadata;
            this.size = builder.size;
            this.resource = builder.resource;
            this.site = builder.site;
            this.eventCount = builder.eventCount;
            this.runMin = builder.runMin;
            this.runMax = builder.runMax;
            this.checksum = builder.checksum;
            this.locationCreated = builder.locationCreated;
            this.locationModified = builder.locationModified;
            this.locationScanned = builder.locationScanned;
            this.scanStatus = builder.scanStatus;
        }
        
        public Builder(Dataset ds){
            super(ds);
            this.fileFormat = ds.fileFormat;
            this.dataType = ds.dataType;
            this.created = ds.dateCreated;
        }
        
        public Builder view(DatasetViewInfo view){
            if(view.versionOpt().isPresent()){
                version(view.getVersion());
            }
            if(view.singularLocationOpt().isPresent()){
                location(view.singularLocationOpt().get());
            } else if (view.locationsOpt().isPresent()){
                locations(view.getLocations());
            }
            return this;
        }

        @JsonSetter
        public Builder version(DatasetVersionModel val){
            this.version = val;
            dsType |= VERSION;
            return this;
        }

        @JsonSetter
        public Builder location(DatasetLocationModel val){
            this.location = val;
            dsType |= LOCATION;
            return this;
        }

        @JsonSetter
        public Builder created(Timestamp val){
            this.created = checkTime(val);
            dsType |= BASE;
            return this;
        }

        @JsonSetter
        public Builder fileFormat(String val){
            this.fileFormat = val;
            dsType |= BASE;
            return this;
        }

        @JsonSetter
        public Builder dataType(String val){
            this.dataType = val;
            dsType |= BASE;
            return this;
        }

        @JsonSetter
        public Builder versionPk(Long val){
            this.versionPk = val;
            dsType |= BASE;
            return this;
        }

        @JsonSetter
        public Builder locationPk(Long val){
            this.locationPk = val;
            dsType |= VERSION;
            return this;
        }

        @JsonIgnore
        public Builder versionId(Integer val){
            this.versionId = val;
            dsType |= VERSION;
            return this;
        }

        @JsonSetter
        public Builder versionId(DatasetView.VersionId val){
            return versionId( val.getId() );
        }

        @JsonSetter
        public Builder datasetSource(String val){
            this.datasetSource = val;
            dsType |= VERSION;
            return this;
        }

        @JsonSetter
        public Builder processInstance(Long val){
            this.processInstance = val;
            dsType |= VERSION;
            return this;
        }

        @JsonSetter
        public Builder taskName(String val){
            this.taskName = val;
            dsType |= VERSION;
            return this;
        }

        @JsonSetter
        public Builder latest(Boolean val){
            this.latest = val;
            dsType |= VERSION;
            return this;
        }

        @JsonSetter
        public Builder versionCreated(Timestamp val){
            this.versionCreated = checkTime(val);
            dsType |= VERSION;
            return this;
        }

        @JsonSetter
        public Builder versionModified(Timestamp val){
            this.versionModified = checkTime(val);
            dsType |= VERSION;
            return this;
        }

        @JsonSetter
        public Builder locations(Collection<DatasetLocationModel> val){
            this.locations = val;
            dsType |= LOCATIONS;
            return this;
        }

        @JsonSetter
        public Builder versionMetadata(List<MetadataEntry> val){
            this.versionMetadata = new HashMap<>();
            if(val != null){
                for(MetadataEntry e: val){
                    if(e.getRawValue() instanceof Number) {
                        versionMetadata.put(e.getKey(), (Number)e.getRawValue());
                    } else {
                        versionMetadata.put(e.getKey(), (String)e.getRawValue());
                    }
                }   
            }
            dsType |= VERSION;
            return this;
        }

        public Builder versionMetadata(Map<String, Object> val){
            this.versionMetadata = new HashMap<>();
            if(val != null){
                this.versionMetadata.putAll( val );
            }
            dsType |= VERSION;
            return this;
        }

        @JsonSetter
        public Builder size(Long val){
            this.size = val;
            dsType |= LOCATION;
            return this;
        }

        @JsonSetter
        public Builder resource(String val){
            this.resource = val;
            dsType |= LOCATION;
            return this;
        }

        @JsonSetter
        public Builder eventCount(Long val){
            this.eventCount = val;
            dsType |= LOCATION;
            return this;
        }

        @JsonSetter
        public Builder site(String val){
            this.site = val;
            dsType |= LOCATION;
            return this;
        }

        @JsonSetter
        public Builder runMin(Long val){
            this.runMin = val;
            dsType |= LOCATION;
            return this;
        }

        @JsonSetter
        public Builder runMax(Long val){
            this.runMax = val;
            dsType |= LOCATION;
            return this;
        }

        @JsonSetter
        public Builder checksum(Long val){
            this.checksum = val;
            dsType |= LOCATION;
            return this;
        }

        @JsonSetter
        public Builder master(Boolean val){
            this.master = val;
            dsType |= LOCATION;
            return this;
        }

        @JsonSetter
        public Builder locationCreated(Timestamp val){
            this.locationCreated = checkTime(val);
            dsType |= LOCATION;
            return this;
        }

        @JsonSetter
        public Builder locationModified(Timestamp val){
            this.locationModified = checkTime(val);
            dsType |= LOCATION;
            return this;
        }

        @JsonSetter
        public Builder locationScanned(Timestamp val){
            this.locationScanned = checkTime(val);
            dsType |= LOCATION;
            return this;
        }

        @JsonSetter
        public Builder scanStatus(String val){
            this.scanStatus = val;
            dsType |= LOCATION;
            return this;
        }

        @Override
        public Dataset build(){
            if(isType( FLAT )){
                return new FlatDataset.Builder( this ).build();
            }
            if(isType( FULL )){
                return new FullDataset.Builder( this ).build();
            }
            if(isType( BASE | VERSION )){
                return new FlatDataset.Builder( this ).build();
            }
            return new Dataset( this );
        }

        public boolean isType(int type){
            return (dsType & type) == type;
        }
        
        public boolean checkType(int type){
            return (dsType & type) != 0;
        }
    }
}
