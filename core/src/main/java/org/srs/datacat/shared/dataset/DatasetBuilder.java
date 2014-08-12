
package org.srs.datacat.shared.dataset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlTransient;
import org.srs.datacat.model.DatasetView.VersionId;
import org.srs.datacat.shared.DatacatObjectBuilder;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.rest.shared.metadata.MetadataEntry;

/**
 *
 * @author bvan
 */
@XmlTransient
public class DatasetBuilder<T extends Dataset> extends DatacatObjectBuilder<T, DatasetBuilder> {
    
    /**
     * This only exists because we need to use another method for creation
     */
    @XmlTransient
    @JsonPOJOBuilder(buildMethodName = "buildVersionWithLocations")
    public static class VersionWithLocationsProxy extends DatasetBuilder { }
    
    public final static int NONE = 0;
    public final static int BASE = 1<<1;

    public final static int VERSION = 1<<2;
    public final static int VERSIONS = 1<<3;

    public final static int LOCATION = 1<<4;
    public final static int LOCATIONS = 1<<5;

    public final static int FLAT = BASE | VERSION | LOCATION;
    public final static int FULL = BASE | VERSION | LOCATIONS;
    public final static int MULTI = BASE | VERSIONS | LOCATIONS;

    public int dsType = BASE;

    public DatasetVersion version;
    public DatasetLocation location;
    
    public List<MetadataEntry> versionMetadata;
    public Map<String,Number> versionNumberMetadata;
    public Map<String,String> versionStringMetadata;
    
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

    protected DatasetBuilder(){}

    /**
     * Copy constructor
     * @param builder 
     */
    public DatasetBuilder(DatasetBuilder builder){
        super(builder);
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

    public static DatasetBuilder create(){
        return new DatasetBuilder();
    }
    
    public static DatasetBuilder create(Class<? extends DatasetBuilder> clazz){
        try {
            return clazz.newInstance();
        } catch(InstantiationException | IllegalAccessException ex) { }
        return new DatasetBuilder();
    }

    public static DatasetBuilder create(Dataset ds){
        DatasetBuilder db = new DatasetBuilder()
                .datasetFileFormat(ds.getFileFormat() )
                .datasetDataType( ds.getDataType() )
                .created( ds.getDateCreated());
        db.pk( ds.getPk())
            .parentPk( ds.getParentPk())
            .name( ds.getName() );
        return db;
                
    }
    
    @Override
    @JsonSetter
    public DatasetBuilder metadata(List<MetadataEntry> val){
        throw new RuntimeException("metadata not implemented on Dataset objects. Use versionMetadata.");
    }

    @JsonSetter
    public DatasetBuilder version(DatasetVersion val){
        this.version = val;
        dsType |= VERSION;
        if(val instanceof VersionWithLocations){
            dsType |= LOCATIONS;
        }
        return this;
    }
    
    @JsonSetter
    public DatasetBuilder location(DatasetLocation val){ this.location = val; dsType |= LOCATION; return this; }

    @JsonSetter public DatasetBuilder created(Timestamp val) { this.created = val; dsType |= BASE; return this; }
    @JsonSetter public DatasetBuilder datasetFileFormat(String val){ this.fileFormat = val; dsType |= BASE;  return this; }
    @JsonSetter public DatasetBuilder datasetDataType(String val){ this.datasetDataType = val; dsType |= BASE; return this; }
    @JsonSetter public DatasetBuilder versionPk(Long val){ this.versionPk = val; dsType |= BASE; return this; }

    @JsonSetter public DatasetBuilder locationPk(Long val){ this.locationPk = val; dsType |= VERSION; return this; }
    @JsonIgnore public DatasetBuilder versionId(Integer val){ this.versionId = val; dsType |= VERSION; return this; }
    @JsonSetter public DatasetBuilder versionId(VersionId val){ return versionId(val.getId()); }
    @JsonSetter public DatasetBuilder datasetSource(String val){ this.datasetSource = val; dsType |= VERSION;  return this; }
    @JsonSetter public DatasetBuilder processInstance(Long val){ this.processInstance = val; dsType |= VERSION; return this; }
    @JsonSetter public DatasetBuilder taskName(String val){ this.taskName = val; dsType |= VERSION; return this; }
    @JsonSetter public DatasetBuilder latest(Boolean val){ this.latest = val; dsType |= VERSION; return this; }
    @JsonSetter public DatasetBuilder versionCreated(Timestamp val) { this.versionCreated = val; dsType |= VERSION; return this; }
    @JsonSetter public DatasetBuilder versionModified(Timestamp val) { this.versionModified = val; dsType |= VERSION; return this; }

    @JsonSetter public DatasetBuilder versions(List<DatasetVersion> val) { this.versions = val; dsType |= VERSIONS; return this; }
    @JsonSetter public DatasetBuilder locations(List<DatasetLocation> val) { this.locations = val; dsType |= LOCATIONS; return this; }
    
    @JsonSetter
    public DatasetBuilder versionMetadata(List<MetadataEntry> val){ this.versionMetadata = val; return  this; }
    
    public DatasetBuilder versionMetadata(Map<String,Object> val){
        this.versionMetadata = new ArrayList<>();
        dsType |= VERSION;
        for(Map.Entry<String,Object> e: val.entrySet()){
            if(e.getValue() instanceof Number){
                versionMetadata.add( new MetadataEntry(e.getKey(), (Number) e.getValue()));
            } else {
                versionMetadata.add( new MetadataEntry(e.getKey(), e.getValue().toString()));
            }
        }
        return this;
    }
    
    public DatasetBuilder versionNumberMetadata(Map<String, Number> val){ this.numberMetadata = val;  dsType |= VERSION; return this; }
    public DatasetBuilder versionStringMetadata(Map<String, String> val){ this.stringMetadata = val; dsType |= VERSION; return this; }

    @JsonSetter public DatasetBuilder fileSize(Long val){ this.fileSize = val; dsType |= LOCATION; return this; }
    @JsonSetter public DatasetBuilder fileSystemPath(String val){ this.fileSystemPath = val; dsType |= LOCATION; return this; }
    @JsonSetter public DatasetBuilder eventCount(Long val){ this.eventCount = val; dsType |= LOCATION; return this; }
    @JsonSetter public DatasetBuilder site(String val){ this.site = val; dsType |= LOCATION;  return this; }
    @JsonSetter public DatasetBuilder runMin(Long val){ this.runMin = val; dsType |= LOCATION; return this; }
    @JsonSetter public DatasetBuilder runMax(Long val){ this.runMax = val; dsType |= LOCATION; return this; }
    @JsonSetter public DatasetBuilder checkSum(Long val){ this.checkSum = val; dsType |= LOCATION; return this; }
    @JsonSetter public DatasetBuilder master(Boolean val){ this.master = val; dsType |= LOCATION; return this; }
    @JsonSetter public DatasetBuilder locationCreated(Timestamp val) { this.locationCreated = val; dsType |= LOCATION; return this; }
    @JsonSetter public DatasetBuilder locationModified(Timestamp val) { this.locationModified = val; dsType |= LOCATION; return this; }
    @JsonSetter public DatasetBuilder locationScanned(Timestamp val) { this.locationScanned = val; dsType |= LOCATION; return this; }
    @JsonSetter public DatasetBuilder scanStatus(String val){ this.scanStatus = val; dsType |= LOCATION;  return this; }

    @Override
    public T build(){
        return (T) buildDataset();
    }
    
    public boolean isType(int type){
        return (dsType & type) == type;
    }
    
    public Dataset buildDataset(){
        if (isType(FLAT)){
            return new FlatDataset.Builder(this).build();
        }
        if (isType(FULL)){
            return new FullDataset.Builder(this).build();
        }
        if (isType(BASE | VERSION)){
            return new FlatDataset.Builder(this).build();
        }
        return new Dataset(this);
    }

}