
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.srs.datacat.model.DatasetLocationModel;
import java.sql.Timestamp;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Objects;
import javax.xml.bind.annotation.XmlTransient;
import org.srs.datacat.shared.DatasetLocation.Builder;
import org.srs.rest.shared.RestDateAdapter;


/**
 * Represents a complete location
 * @author bvan
 */
@XmlRootElement
@XmlType(name="location")
@JsonTypeName(value="location")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="$type", defaultImpl=DatasetLocation.class)
@JsonDeserialize(builder = Builder.class)
public class DatasetLocation extends DatacatObject implements DatasetLocationModel {
   
    private String fileSystemPath;
    private Long fileSize;
    private String site;
    private Long runMin;
    private Long runMax;
    private Long eventCount;
    private Long checkSum;
    private Timestamp dateModified;
    private Timestamp dateScanned;
    private Timestamp dateCreated;
    private String scanStatus;
    private Boolean master;
    
    public DatasetLocation(){ super(); }
    
    /**
     * Copy constructor
     * @param location 
     */
    public DatasetLocation(DatasetLocation location){
        super(location);
        this.fileSystemPath = location.fileSystemPath;
        this.fileSize = location.fileSize;
        this.site = location.site;
        this.runMin = location.runMin;
        this.runMax = location.runMax;
        this.checkSum = location.checkSum;
        this.dateModified = location.dateModified;
        this.dateCreated = location.dateCreated;
        this.dateScanned = location.dateScanned;
        this.scanStatus = location.scanStatus;
        this.eventCount = location.eventCount;
        this.master = location.master;
    }

    public DatasetLocation(Builder builder){
        super(builder.pk, builder.parentPk, builder.fileSystemPath);
        this.fileSystemPath = builder.fileSystemPath;
        this.fileSize = builder.fileSize;
        this.site = builder.site;
        this.runMin = builder.runMin;
        this.runMax = builder.runMax;
        this.checkSum = builder.checkSum;
        this.dateModified = builder.dateModified;
        this.dateCreated = builder.dateCreated;
        this.dateScanned = builder.dateScanned;
        this.scanStatus = builder.scanStatus;
        this.eventCount = builder.eventCount;
        this.master = builder.master;
    }
    
    public DatasetLocation(Dataset.Builder builder){
        super(builder.locationPk, builder.versionPk, builder.fileSystemPath);
        this.fileSystemPath = builder.fileSystemPath;
        this.fileSize = builder.fileSize;
        this.site = builder.site;
        this.runMin = builder.runMin;
        this.runMax = builder.runMax;
        this.checkSum = builder.checkSum;
        this.dateModified = builder.locationModified;
        this.dateCreated = builder.locationCreated;
        this.dateScanned = builder.locationScanned;
        this.scanStatus = builder.scanStatus;
        this.eventCount = builder.eventCount;
        this.master = builder.master;
    }
   
    @Override public String getFileSystemPath() { return this.fileSystemPath; }
    @Override public Long getFileSize() { return this.fileSize; }
    @Override public String getSite() { return this.site; }
    @Override public Long getRunMin() { return this.runMin; }
    @Override public Long getRunMax() { return this.runMax; }
    @Override public Long getEventCount() { return this.eventCount; }
    @Override public Long getCheckSum() { return this.checkSum; }
   
    @Override 
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    @XmlElement(name="modified", required=false)
    public Timestamp getDateModified() { return this.dateModified;}
   
    @Override 
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    @XmlElement(name="scanned", required=false)
    public Timestamp getDateScanned() { return this.dateScanned;}

    @Override
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    @XmlElement(name="locationRegistered", required=false)
    public Timestamp getDateCreated() { return this.dateCreated;}

    @Override public String getScanStatus() { return this.scanStatus; }
    
    @Override public Boolean isMaster(){ return master; }

    @Override
    public String toString(){
        return "DatasetLocation{" + "fileSystemPath=" + fileSystemPath + ", fileSize=" + fileSize +
                ", site=" + site + ", runMin=" + runMin + ", runMax=" + runMax + ", eventCount=" + 
                eventCount + ", checkSum=" + checkSum + ", dateModified=" + dateModified + 
                ", dateScanned=" + dateScanned + ", dateCreated=" + dateCreated + ", scanStatus=" + 
                scanStatus + ", master=" + master + '}';
    }
    
    public void validateFields(){
        Objects.requireNonNull( this.fileSystemPath,"Physical file path required");
        Objects.requireNonNull( this.site, "Location site is required");
    }

    @XmlTransient
    public static class Builder extends DatacatObject.Builder<Builder>{
        private String fileSystemPath;
        private Long fileSize;
        private String site;
        private Long runMin;
        private Long runMax;
        private Long eventCount;
        private Long checkSum;
        private Timestamp dateModified;
        private Timestamp dateScanned;
        private Timestamp dateCreated;
        private String scanStatus;
        private Boolean master;
        
        public Builder(){}
        
        public Builder(DatasetLocation location){
            super(location);
            this.name = location.site;
            this.fileSystemPath = location.fileSystemPath;
            this.fileSize = location.fileSize;
            this.site = location.site;
            this.runMin = location.runMin;
            this.runMax = location.runMax;
            this.checkSum = location.checkSum;
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
            this.fileSystemPath = builder.fileSystemPath;
            this.fileSize = builder.fileSize;
            this.site = builder.site;
            this.runMin = builder.runMin;
            this.runMax = builder.runMax;
            this.checkSum = builder.checkSum;
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
        
        @JsonSetter public Builder fileSize(Long val){ this.fileSize = val; return this; }
        @JsonSetter public Builder fileSystemPath(String val){ this.fileSystemPath = val; return this; }
        @JsonSetter public Builder eventCount(Long val){ this.eventCount = val; return this; }
        @JsonSetter public Builder site(String val){ this.site = val;  return this; }
        @JsonSetter public Builder runMin(Long val){ this.runMin = val; return this; }
        @JsonSetter public Builder runMax(Long val){ this.runMax = val; return this; }
        @JsonSetter public Builder checkSum(Long val){ this.checkSum = val; return this; }
        @JsonSetter public Builder master(Boolean val){ this.master = val; return this; }
        @JsonSetter public Builder created(Timestamp val) { this.dateCreated = val; return this; }
        @JsonSetter public Builder modified(Timestamp val) { this.dateModified = val; return this; }
        @JsonSetter public Builder scanned(Timestamp val) { this.dateScanned = val; return this; }
        @JsonSetter public Builder scanStatus(String val){ this.scanStatus = val; return this; }
    }
   
}
