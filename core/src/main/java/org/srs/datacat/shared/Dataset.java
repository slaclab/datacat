
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.srs.datacat.model.DatasetModel;
import java.sql.Timestamp;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlTransient;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.Dataset.Builder;
import org.srs.datacat.shared.dataset.FlatDataset;
import org.srs.datacat.shared.dataset.FullDataset;
import org.srs.datacat.shared.dataset.VersionWithLocations;
import org.srs.rest.shared.RestDateAdapter;
import org.srs.rest.shared.metadata.MetadataEntry;

/**
 * Represents an entire dataset. May include information on all datasetversions, 
 * @author bvan
 */
@XmlRootElement
@XmlType(name="dataset")
@JsonTypeName(value="dataset")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="$type", defaultImpl=Dataset.class)
@JsonDeserialize(builder=Builder.class)
public class Dataset extends DatacatObject implements DatasetModel {
    
    private String datasetFileFormat;
    private String datasetDataType;
    private Timestamp dateCreated;
    
    public Dataset(){ super(); }
    
    public Dataset(DatacatObject o){ super(o); }
    
    /**
     * Copy constructor
     * @param dataset 
     */
    public Dataset(Dataset dataset){
        super(dataset);
        this.datasetDataType = dataset.datasetDataType;
        this.datasetFileFormat = dataset.datasetFileFormat;
        this.dateCreated = dataset.dateCreated;
    }
    
    public Dataset(DatacatObject.Builder builder){
        super(builder);
    }
    
    public Dataset(Builder builder){
        super(builder);
        this.datasetDataType = builder.datasetDataType;
        this.datasetFileFormat = builder.fileFormat;
        this.dateCreated = builder.created;
    }
    
    @Override
    @XmlElement(required=false)
    public String getDataType() { return this.datasetDataType;}
        
    @Override
    @XmlElement(required=false)
    public String getFileFormat() { return this.datasetFileFormat;}
    
    @Override
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    @XmlElement(name="registered", required=false)
    public Timestamp getDateCreated(){ return this.dateCreated; }
    
    @Override
    @XmlTransient
    public Timestamp getDateModified(){
        return null;
    }
    
    @Override
    public String toString() {
        return super.toString() + "\tType: " + datasetDataType + "\tRegistered: " + dateCreated;
    }

    @Override
    public boolean weakEquals(Object obj){
       if(obj == null || getClass().isAssignableFrom(obj.getClass())){
            return false;
        }
        if(!super.weakEquals( obj )){
            return false;
        }
        Dataset other = (Dataset) obj;
        if(!weakEquivalence( this.datasetFileFormat, other.datasetFileFormat )){
            return false;
        }
        if(!weakEquivalence( this.datasetDataType, other.datasetDataType )){
            return false;
        }
        if(!weakEquivalence( this.dateCreated, other.dateCreated )){
            return false;
        }
        return true;
    }
    
    @XmlTransient
    public List<DatasetView> getDatasetViews(){
        return Collections.singletonList(DatasetView.EMPTY);
    }

    /**
     *
     * @author bvan
     */
    @XmlTransient
    public static class Builder extends DatacatObject.Builder<Builder> {
        /**
         * This only exists because we need to use another method for creation
         */
        @XmlTransient
        @JsonPOJOBuilder(buildMethodName = "buildVersionWithLocations")
        public static class VersionWithLocationsProxy extends Builder {
            public VersionWithLocationsProxy(){
                super();
            }
        }
        public static final int NONE = 0;
        public static final int BASE = 1 << 1;
        public static final int VERSION = 1 << 2;
        public static final int VERSIONS = 1 << 3;
        public static final int LOCATION = 1 << 4;
        public static final int LOCATIONS = 1 << 5;
        public static final int FLAT = BASE | VERSION | LOCATION;
        public static final int FULL = BASE | VERSION | LOCATIONS;
        public static final int MULTI = BASE | VERSIONS | LOCATIONS;
        public int dsType = BASE;
        public DatasetVersion version;
        public DatasetLocation location;
        public List<MetadataEntry> versionMetadata;
        public Map<String, Number> versionNumberMetadata;
        public Map<String, String> versionStringMetadata;
        public List<DatasetVersion> versions;
        public List<DatasetLocation> locations;
        // mixins
        public Timestamp created;
        public Long versionPk;
        public Long locationPk;
        public Boolean latest;
        public Boolean master;
        public String fileFormat;
        public String datasetDataType;
        public Integer versionId;
        public String datasetSource;
        public Long processInstance;
        public String taskName;
        public Timestamp versionCreated;
        public Timestamp versionModified;
        public Long fileSize;
        public String fileSystemPath;
        public String site;
        public Long eventCount;
        public Long runMin;
        public Long runMax;
        public Long checkSum;
        public Timestamp locationCreated;
        public Timestamp locationModified;
        public Timestamp locationScanned;
        public String scanStatus;

        public Builder(){
            super();
        }

        /**
         * Copy constructor
         * @param builder
         */
        public Builder(Builder builder){
            super( builder );
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
            this.datasetDataType = builder.datasetDataType;
            this.versionId = builder.versionId;
            this.datasetSource = builder.datasetSource;
            this.processInstance = builder.processInstance;
            this.taskName = builder.taskName;
            this.versionCreated = builder.versionCreated;
            this.versionModified = builder.versionModified;
            this.versionMetadata = builder.versionMetadata;
            this.fileSize = builder.fileSize;
            this.fileSystemPath = builder.fileSystemPath;
            this.site = builder.site;
            this.eventCount = builder.eventCount;
            this.runMin = builder.runMin;
            this.runMax = builder.runMax;
            this.checkSum = builder.checkSum;
            this.locationCreated = builder.locationCreated;
            this.locationModified = builder.locationModified;
            this.locationScanned = builder.locationScanned;
            this.scanStatus = builder.scanStatus;
        }
        
        public Builder(Dataset ds){
            super(ds);
            this.fileFormat = ds.datasetFileFormat;
            this.datasetDataType = ds.datasetDataType;
            this.created = ds.dateCreated;
        }

        @Override
        @JsonSetter
        public Builder metadata(List<MetadataEntry> val){
            throw new RuntimeException( "metadata not implemented on Dataset objects. Use versionMetadata." );
        }

        @JsonSetter
        public Builder version(DatasetVersion val){
            this.version = val;
            dsType |= VERSION;
            if(val instanceof VersionWithLocations){
                dsType |= LOCATIONS;
            }
            return this;
        }

        @Override
        public Builder metadata(Map<String, Object> val){
            throw new UnsupportedOperationException();
        }

        @JsonSetter
        public Builder location(DatasetLocation val){
            this.location = val;
            dsType |= LOCATION;
            return this;
        }

        @JsonSetter
        public Builder created(Timestamp val){
            this.created = val;
            dsType |= BASE;
            return this;
        }

        @JsonSetter
        public Builder datasetFileFormat(String val){
            this.fileFormat = val;
            dsType |= BASE;
            return this;
        }

        @JsonSetter
        public Builder datasetDataType(String val){
            this.datasetDataType = val;
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
            this.versionCreated = val;
            dsType |= VERSION;
            return this;
        }

        @JsonSetter
        public Builder versionModified(Timestamp val){
            this.versionModified = val;
            dsType |= VERSION;
            return this;
        }

        @JsonSetter
        public Builder versions(List<DatasetVersion> val){
            this.versions = val;
            dsType |= VERSIONS;
            return this;
        }

        @JsonSetter
        public Builder locations(List<DatasetLocation> val){
            this.locations = val;
            dsType |= LOCATIONS;
            return this;
        }

        @JsonSetter
        public Builder versionMetadata(List<MetadataEntry> val){
            this.versionMetadata = val;
            return this;
        }

        public Builder versionMetadata(Map<String, Object> val){
            this.versionMetadata = new ArrayList<>();
            dsType |= VERSION;
            for(Map.Entry<String, Object> e: val.entrySet()){
                if(e.getValue() instanceof Number){
                    versionMetadata.add( new MetadataEntry( e.getKey(), (Number) e.getValue() ) );
                } else {
                    versionMetadata.add( new MetadataEntry( e.getKey(), e.getValue().toString() ) );
                }
            }
            return this;
        }

        public Builder versionNumberMetadata(Map<String, Number> val){
            this.numberMetadata = val;
            dsType |= VERSION;
            return this;
        }

        public Builder versionStringMetadata(Map<String, String> val){
            this.stringMetadata = val;
            dsType |= VERSION;
            return this;
        }

        @JsonSetter
        public Builder fileSize(Long val){
            this.fileSize = val;
            dsType |= LOCATION;
            return this;
        }

        @JsonSetter
        public Builder fileSystemPath(String val){
            this.fileSystemPath = val;
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
        public Builder checkSum(Long val){
            this.checkSum = val;
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
            this.locationCreated = val;
            dsType |= LOCATION;
            return this;
        }

        @JsonSetter
        public Builder locationModified(Timestamp val){
            this.locationModified = val;
            dsType |= LOCATION;
            return this;
        }

        @JsonSetter
        public Builder locationScanned(Timestamp val){
            this.locationScanned = val;
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
            return buildDataset();
        }

        public boolean isType(int type){
            return (dsType & type) == type;
        }

        public Dataset buildDataset(){
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
    }
}
