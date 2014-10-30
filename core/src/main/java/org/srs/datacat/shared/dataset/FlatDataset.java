
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
import org.srs.datacat.model.HasDatasetViewInfo;
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
public class FlatDataset extends Dataset implements DatasetVersionModel, DatasetLocationModel, HasDatasetViewInfo {
    
    private DatasetViewInfo viewInfo;    
    private FlatDataset(){}
    
    /**
     * Copy constructor. Can convert a FullDataset to a FlatDataset.
     * @param dataset 
     */
    public FlatDataset(Dataset dataset){
        super(dataset);
        DatasetVersion dsVersion = null;
        DatasetLocation dsLocation = null;
        if(dataset instanceof HasDatasetViewInfo){
            DatasetViewInfo info = ((HasDatasetViewInfo) dataset).getDatasetViewInfo();
            dsVersion = info.getVersion();
            if(dataset instanceof FullDataset){
                dsLocation = info.canonicalLocationOpt().orNull();
            } else {
                dsLocation = info.singularLocationOpt().orNull();
            }
        }
        this.viewInfo = new DatasetViewInfo(dsVersion, dsLocation);
    }
    
    protected FlatDataset(Dataset.Builder builder){
        super(builder);
        this.viewInfo = new DatasetViewInfo(builder.version, builder.location);
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
    
    @XmlElement(required=false)
    public Long getLocationPk(){ 
        
        if(viewInfo.singularLocationOpt().isPresent()){
            return viewInfo.singularLocationOpt().get().getPk();
        }
        return null; 
    }
    
    @XmlTransient
    public DatasetVersion getVersion(){ 
        return viewInfo.getVersion();
    }
    
    @XmlTransient
    public DatasetLocation getLocation(){ 
        return viewInfo.singularLocationOpt().orNull();
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
        return null; }

    @Override
    @XmlElement(required=false)
    public Integer getVersionId(){ 
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getVersionId();
        }
        return null; }

    @XmlElement(name="versionCreated", required=false)
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    public Timestamp getDateVersionCreated(){ 
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getDateCreated();
        }
        return null; }

    // Location info
    @Override 
    @XmlElement(required=false)
    public String getResource(){ 
        if(viewInfo.singularLocationOpt().isPresent()){
            return viewInfo.singularLocationOpt().get().getResource();
        }
        return null; }
    
    @Override 
    @XmlElement(required=false)
    public Long getSize(){ 
        if(viewInfo.singularLocationOpt().isPresent()){
            return viewInfo.singularLocationOpt().get().getSize();
        }
        return null; }
    
    @Override 
    @XmlElement(required=false)
    public Long getChecksum(){ 
        if(viewInfo.singularLocationOpt().isPresent()){
            return viewInfo.singularLocationOpt().get().getChecksum();
        }
        return null; }
    
    @XmlElement(name="locationModified", required=false)
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    public Timestamp getDateLocationModified(){ 
        if(viewInfo.singularLocationOpt().isPresent()){
            return viewInfo.singularLocationOpt().get().getDateModified();
        }
        return null; }
    
    @XmlElement(name="locationRegistered", required=false)
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    public Timestamp getDateLocationCreated(){ 
        if(viewInfo.singularLocationOpt().isPresent()){
            return viewInfo.singularLocationOpt().get().getDateCreated();
        }
        return null; }
    
    @Override 
    @XmlElement(name="locationScanned", required=false)
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    public Timestamp getDateScanned(){ 
        if(viewInfo.singularLocationOpt().isPresent()){
            return viewInfo.singularLocationOpt().get().getDateScanned();
        }
        return null; }
    
    @Override 
    @XmlElement(required=false)
    public String getSite(){ 
        if(viewInfo.singularLocationOpt().isPresent()){
            return viewInfo.singularLocationOpt().get().getSite();
        }
        return null; }
    
    @Override 
    @XmlElement(required=false)
    public String getScanStatus(){ 
        if(viewInfo.singularLocationOpt().isPresent()){
            return viewInfo.singularLocationOpt().get().getScanStatus();
        }
        return null; }
    
    @Override 
    @XmlElement(required=false)
    public Long getEventCount(){ 
        if(viewInfo.singularLocationOpt().isPresent()){
            return viewInfo.singularLocationOpt().get().getEventCount();
        }
        return null; }
    
    @Override 
    @XmlElement(required=false)
    public Long getRunMin(){ 
        if(viewInfo.singularLocationOpt().isPresent()){
            return viewInfo.singularLocationOpt().get().getRunMin();
        }
        return null; }
    
    @Override 
    @XmlElement(required=false)
    public Long getRunMax(){ 
        if(viewInfo.singularLocationOpt().isPresent()){
            return viewInfo.singularLocationOpt().get().getRunMax();
        }
        return null; 
    }
    
    @Override
    @XmlElement(required=false)
    public Boolean isMaster(){ 
        if(viewInfo.singularLocationOpt().isPresent()){
            return viewInfo.singularLocationOpt().get().isMaster();
        }
        return null; 
    }

    @Override
    public String toString(){
        String verInfo = viewInfo.versionOpt().isPresent() ? 
                " Version:" + viewInfo.getVersion().toString() : "";
        String locInfo = viewInfo.singularLocationOpt().isPresent() ? 
                " Location:" + viewInfo.singularLocationOpt().get().toString() : "";
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
