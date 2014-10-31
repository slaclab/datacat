
package org.srs.datacat.shared.dataset;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.srs.datacat.model.DatasetLocationModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.dataset.FlatDataset.Builder;
import org.srs.rest.shared.RestDateAdapter;

/**
 * Dataset with a version. The location may not exist.
 * @author bvan
 */
@XmlRootElement
@XmlType(name="flatDataset")
@JsonTypeName(value="dataset#flat")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="$type", defaultImpl=DatasetVersion.class)
@JsonDeserialize(builder = Builder.class)
public class FlatDataset extends DatasetWithView implements DatasetLocationModel {
    
    private FlatDataset(){}
    
    /**
     * Copy constructor. Can convert a FullDataset to a FlatDataset.
     * @param dataset 
     */
    public FlatDataset(Dataset dataset){
        super(dataset, true);
    }
    
    protected FlatDataset(Dataset.Builder builder){
        super(builder, true);
    }
        
    @XmlElement(required=false)
    public Long getLocationPk(){ 
        
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getPk();
        }
        return null; 
    }
    
    @XmlTransient
    public DatasetVersion getVersion(){ 
        return getViewInfo().getVersion();
    }
    
    @XmlTransient
    public DatasetLocation getLocation(){ 
        return getViewInfo().singularLocationOpt().orNull();
    }
    
    // Location info
    @Override 
    @XmlElement(required=false)
    public String getResource(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getResource();
        }
        return null; 
    }
    
    @Override 
    @XmlElement(required=false)
    public Long getSize(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getSize();
        }
        return null; 
    }
    
    @Override 
    @XmlElement(required=false)
    public Long getChecksum(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getChecksum();
        }
        return null; 
    }
    
    @XmlElement(name="locationModified", required=false)
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    public Timestamp getDateLocationModified(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getDateModified();
        }
        return null; 
    }
    
    @XmlElement(name="locationRegistered", required=false)
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    public Timestamp getDateLocationCreated(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getDateCreated();
        }
        return null; 
    }
    
    @Override 
    @XmlElement(name="locationScanned", required=false)
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    public Timestamp getDateScanned(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getDateScanned();
        }
        return null; 
    }
    
    @Override 
    @XmlElement(required=false)
    public String getSite(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getSite();
        }
        return null; 
    }
    
    @Override 
    @XmlElement(required=false)
    public String getScanStatus(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getScanStatus();
        }
        return null; 
    }
    
    @Override 
    @XmlElement(required=false)
    public Long getEventCount(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getEventCount();
        }
        return null; 
    }
    
    @Override 
    @XmlElement(required=false)
    public Long getRunMin(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getRunMin();
        }
        return null; 
    }
    
    @Override 
    @XmlElement(required=false)
    public Long getRunMax(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getRunMax();
        }
        return null; 
    }
    
    @Override
    @XmlElement(required=false)
    public Boolean isMaster(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().isMaster();
        }
        return null; 
    }

    @Override
    public String toString(){
        String verInfo = getViewInfo().versionOpt().isPresent() ? 
                " Version:" + getViewInfo().getVersion().toString() : "";
        String locInfo = getViewInfo().singularLocationOpt().isPresent() ? 
                " Location:" + getViewInfo().singularLocationOpt().get().toString() : "";
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
