
package org.srs.datacat.shared.dataset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.srs.datacat.model.DatasetLocationModel;
import org.srs.datacat.model.DatasetVersionModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.dataset.FlatDataset.Builder;
import org.srs.rest.shared.RestDateAdapter;
import org.srs.rest.shared.metadata.MetadataEntry;

/**
 * Dataset with a version. The location may not exist.
 * @author bvan
 */
@XmlRootElement
@XmlType(name="flatDataset")
@JsonTypeName(value="dataset#flat")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="$type", defaultImpl=DatasetVersion.class)
@JsonDeserialize(builder = Builder.class)
public class FlatDataset extends Dataset implements DatasetVersionModel, DatasetLocationModel {
    private DatasetVersion dsVersion = DatasetVersion.Builder.create().build();
    private DatasetLocation dsLocation = new DatasetLocation();
    
    private FlatDataset(){}
    
    /**
     * Copy constructor. Can convert a FullDataset to a FlatDataset.
     * @param dataset 
     */
    public FlatDataset(Dataset dataset){
        super(dataset);
        if(dataset instanceof FlatDataset){
            dsVersion = ((FlatDataset) dataset).dsVersion;
            dsLocation = ((FlatDataset) dataset).dsLocation;
        } 
        else if(dataset instanceof FullDataset){
            FullDataset ds = (FullDataset) dataset;
            dsVersion = new DatasetVersion(ds.getVersion());
            if(ds.getVersion() instanceof VersionWithLocations){
                VersionWithLocations loc = (VersionWithLocations) ds.getVersion();
                dsLocation = new DatasetLocation(loc.getPriorityLocation());
            } else {
                dsLocation = new DatasetLocation();
            }
        }
    }
    
    protected FlatDataset(Dataset.Builder builder){
        super(builder);
        dsVersion = builder.version;
        dsLocation = builder.location;
    }
    
    @XmlElement(required=false)
    public Long getDatasetVersionPk(){ return dsVersion.getPk(); }
    
    @XmlElement(required=false)
    public Long getDatasetLocationPk(){ return dsLocation.getPk(); }
    
    @XmlTransient
    public DatasetVersion getVersion(){ return dsVersion; }
    
    @XmlTransient
    public DatasetLocation getLocation(){ return dsLocation; }
    
    @XmlElementWrapper(name="versionMetadata")
    @XmlElement(required=false, name="entry")
    @JsonProperty("versionMetadata")
    public List<MetadataEntry> getVersionMetadata(){ return dsVersion.getMetadata(); }
    
    @Override
    @XmlElement(required=false)
    public String getDatasetSource(){ return dsVersion.getDatasetSource(); }

    @Override
    @XmlElement(required=false)
    public Boolean isLatest(){ return dsVersion.isLatest(); }

    @Override
    @XmlElement(required=false)
    public Long getProcessInstance(){ return dsVersion.getProcessInstance(); }

    @Override
    @XmlElement(required=false)
    public String getTaskName(){ return dsVersion.getTaskName(); }

    @Override
    @XmlElement(required=false)
    public Integer getVersionId(){ return dsVersion.getVersionId(); }

    @XmlElement(required=false)
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    public Timestamp getDateVersionCreated(){ return dsVersion.getDateCreated(); }

    // Location info
    @Override 
    @XmlElement(required=false)
    public String getFileSystemPath(){ return dsLocation.getFileSystemPath(); }
    
    @Override 
    @XmlElement(required=false)
    public Long getFileSize(){ return dsLocation.getFileSize(); }
    
    @Override 
    @XmlElement(required=false)
    public Long getCheckSum(){ return dsLocation.getCheckSum(); }
    
    @XmlElement(required=false)
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    public Timestamp getDateLocationModified(){ return dsLocation.getDateModified(); }
    
    @XmlElement(required=false)
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    public Timestamp getDateLocationCreated(){ return dsLocation.getDateCreated(); }
    
    @Override 
    @XmlElement(required=false)
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    public Timestamp getDateScanned(){ return dsLocation.getDateScanned(); }
    
    @Override 
    @XmlElement(required=false)
    public String getSite(){ return dsLocation.getSite(); }
    
    @Override 
    @XmlElement(required=false)
    public String getScanStatus(){ return dsLocation.getScanStatus(); }
    
    @Override 
    @XmlElement(required=false)
    public Long getEventCount(){ return dsLocation.getEventCount(); }
    
    @Override 
    @XmlElement(required=false)
    public Long getRunMin(){ return dsLocation.getRunMin(); }
    
    @Override 
    @XmlElement(required=false)

    public Long getRunMax(){ return dsLocation.getRunMax(); }
    
    @Override
    @XmlElement(required=false)
    public Boolean isMaster(){ return dsLocation.isMaster(); }

    @Override
    public String toString(){
        String verInfo = dsVersion != null ? " Version:" + dsVersion.toString() : "";
        String locInfo = dsLocation != null ? " Location:" + dsLocation.toString() : "";
        return "FlatDataset{" + super.toString() + verInfo + locInfo + '}';
    }
    
    @XmlTransient
    public static class Builder extends Dataset.Builder{
        
        public Builder(){}
        /**
         * Copy constructor
         * @param builder 
         */
        public Builder(Dataset.Builder builder){
            super(builder);
        }
        
        @Override
        public FlatDataset build(){
            if(location == null){
                location(new DatasetLocation( this ));
            }
            if(version == null){
                version(new DatasetVersion.Builder(this).build());
            }
            return new FlatDataset(this);
        }
    }

    
    @Override
    @XmlTransient
    public List<DatasetView> getDatasetViews(){
        ArrayList<DatasetView> viewList = new ArrayList<>();
        viewList.add( new DatasetView(getVersionId(), getSite()));
        if(isMaster()){
            viewList.add( new DatasetView(getVersionId(), DatasetView.CANONICAL_SITE));
        }
        if(isLatest()){
            viewList.add( new DatasetView(DatasetView.CURRENT_VER, getSite()));
        }
        if(isLatest() && isMaster()){
            viewList.add(DatasetView.MASTER);
        }
        return viewList;
    }
}
