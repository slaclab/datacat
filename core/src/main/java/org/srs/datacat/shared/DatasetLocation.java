
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.srs.datacat.model.DatasetLocationModel;
import java.sql.Timestamp;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Objects;
import org.srs.datacat.shared.DatasetLocation.Builder;
import org.srs.rest.shared.RestDateAdapter;


/**
 * A DatasetLocation represents a physical file for a given DatasetVersion.
 * 
 * @author bvan
 */
@JsonTypeName(value="location")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="_type", defaultImpl=DatasetLocation.class)
@JsonDeserialize(builder = Builder.class)
@JsonPropertyOrder({"_type", "name",  "path", "pk", "parentPk",
    "metadata", "site", "master", "resource", "size", "checksum", "scanStatus",
    "registered", "modified", "scanned", "runMin", "runMax", "eventCount"}
)
public class DatasetLocation extends DatacatObject implements DatasetLocationModel {
   
    private String resource;
    private Long size;
    private String site;
    private Long runMin;
    private Long runMax;
    private Long eventCount;
    private Long checksum;
    private Timestamp dateModified;
    private Timestamp dateScanned;
    private Timestamp dateCreated;
    private String scanStatus;
    private Boolean master;
    
    public DatasetLocation(){ super(); }
    
    /**
     * Copy constructor.
     * 
     * @param location 
     */
    public DatasetLocation(DatasetLocation location){
        super(location);
        this.resource = location.resource;
        this.size = location.size;
        this.site = location.site;
        this.runMin = location.runMin;
        this.runMax = location.runMax;
        this.checksum = location.checksum;
        this.dateModified = location.dateModified;
        this.dateCreated = location.dateCreated;
        this.dateScanned = location.dateScanned;
        this.scanStatus = location.scanStatus;
        this.eventCount = location.eventCount;
        this.master = location.master;
    }

    public DatasetLocation(Builder builder){
        super(builder.pk, builder.parentPk, builder.site);
        this.resource = builder.resource;
        this.size = builder.size;
        this.site = builder.site;
        this.runMin = builder.runMin;
        this.runMax = builder.runMax;
        this.checksum = builder.checksum;
        this.dateModified = builder.dateModified;
        this.dateCreated = builder.dateCreated;
        this.dateScanned = builder.dateScanned;
        this.scanStatus = builder.scanStatus;
        this.eventCount = builder.eventCount;
        this.master = builder.master;
    }
    
    public DatasetLocation(Dataset.Builder builder){
        super(builder.locationPk, builder.versionPk, builder.site);
        this.resource = builder.resource;
        this.size = builder.size;
        this.site = builder.site;
        this.runMin = builder.runMin;
        this.runMax = builder.runMax;
        this.checksum = builder.checksum;
        this.dateModified = builder.locationModified;
        this.dateCreated = builder.locationCreated;
        this.dateScanned = builder.locationScanned;
        this.scanStatus = builder.scanStatus;
        this.eventCount = builder.eventCount;
        this.master = builder.master;
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL) @Override public String getResource() { return this.resource; }
    
    @Patchable(column="FileSizeBytes")
    @JsonInclude(JsonInclude.Include.NON_NULL) @Override public Long getSize() { return this.size; }
    @JsonInclude(JsonInclude.Include.NON_NULL) @Override public String getSite() { return this.site; }
    
    @Patchable(column="RunMin")
    @JsonInclude(JsonInclude.Include.NON_NULL) public Long getRunMin() { return this.runMin; }
    
    @Patchable(column="RunMax")
    @JsonInclude(JsonInclude.Include.NON_NULL) public Long getRunMax() { return this.runMax; }
    
    @Patchable(column="NumberEvents")
    @JsonInclude(JsonInclude.Include.NON_NULL) public Long getEventCount() { return this.eventCount; }
    
    @Patchable(column="Checksum")
    @JsonInclude(JsonInclude.Include.NON_NULL) @Override public Long getChecksum() { return this.checksum; }
   
    @Override 
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    @JsonProperty("modified")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Timestamp getDateModified() { return this.dateModified; }
   
    @Override 
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    @Patchable(column="LastScanned")
    @JsonProperty("scanned")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Timestamp getDateScanned() { return this.dateScanned; }

    @Override
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    @JsonProperty("registered")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Timestamp getDateCreated() { return this.dateCreated; }

    @Patchable(column="ScanStatus")
    @Override public String getScanStatus() { return this.scanStatus; }
    
    @Override public Boolean isMaster(){ return master; }

    @Override
    public String toString(){
        return "DatasetLocation{" + "resource=" + resource + ", size=" + size +
                ", site=" + site + ", runMin=" + runMin + ", runMax=" + runMax + ", eventCount=" + 
                eventCount + ", checksum=" + checksum + ", modified=" + dateModified + 
                ", scanned=" + dateScanned + ", created=" + dateCreated + ", scanStatus=" + 
                scanStatus + ", master=" + master + '}';
    }
    
    public void validateFields(){
        Objects.requireNonNull( this.resource, "Physical file path required");
        Objects.requireNonNull( this.site, "Location site is required");
    }
    
    @Override
    public int hashCode(){
        return String.format("%s_%s", this.site, this.resource).hashCode();
    }

    /**
     * Builder.
     */
    public static class Builder extends DatacatObject.Builder<Builder>{
        private String resource;
        private Long size;
        private String site;
        private Long runMin;
        private Long runMax;
        private Long eventCount;
        private Long checksum;
        private Timestamp dateModified;
        private Timestamp dateScanned;
        private Timestamp dateCreated;
        private String scanStatus;
        private Boolean master;
        
        public Builder(){}
        
        public Builder(DatasetLocation location){
            super(location);
            this.name = location.site;
            this.resource = location.resource;
            this.size = location.size;
            this.site = location.site;
            this.runMin = location.runMin;
            this.runMax = location.runMax;
            this.checksum = location.checksum;
            this.dateModified = location.dateModified;
            this.dateCreated = location.dateCreated;
            this.dateScanned = location.dateScanned;
            this.scanStatus = location.scanStatus;
            this.eventCount = location.eventCount;
            this.master = location.master;
        }
        
        public Builder(Dataset.Builder builder){
            this.pk = builder.locationPk;
            this.parentPk = builder.versionPk;
            this.name = builder.site;
            this.resource = builder.resource;
            this.size = builder.size;
            this.site = builder.site;
            this.runMin = builder.runMin;
            this.runMax = builder.runMax;
            this.checksum = builder.checksum;
            this.dateModified = builder.locationModified;
            this.dateCreated = builder.locationCreated;
            this.dateScanned = builder.locationScanned;
            this.scanStatus = builder.scanStatus;
            this.eventCount = builder.eventCount;
            this.master = builder.master;
        }
   
        public static Builder create(){
            return new Builder();
        }

        @Override
        public DatasetLocation build(){ return new DatasetLocation(this); }
        
        @JsonSetter public Builder size(Long val){ this.size = val; return this; }
        @JsonSetter public Builder resource(String val){ this.resource = val; return this; }
        @JsonSetter public Builder eventCount(Long val){ this.eventCount = val; return this; }
        @JsonSetter public Builder site(String val){ this.site = val;  return this; }
        @JsonSetter public Builder runMin(Long val){ this.runMin = val; return this; }
        @JsonSetter public Builder runMax(Long val){ this.runMax = val; return this; }
        @JsonSetter public Builder checksum(Long val){ this.checksum = val; return this; }
        @JsonSetter public Builder master(Boolean val){ this.master = val; return this; }
        @JsonSetter public Builder created(Timestamp val) { this.dateCreated = val; return this; }
        @JsonSetter public Builder modified(Timestamp val) { this.dateModified = val; return this; }
        @JsonSetter public Builder scanned(Timestamp val) { this.dateScanned = val; return this; }
        @JsonSetter public Builder scanStatus(String val){ this.scanStatus = val; return this; }
    }
   
}
