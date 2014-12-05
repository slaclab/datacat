
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.srs.datacat.model.DatasetLocationModel;
import org.srs.datacat.model.DatasetVersionModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.FlatDataset.Builder;
import org.srs.rest.shared.RestDateAdapter;

/**
 * Dataset with a version. The location may not exist.
 * @author bvan
 */
@JacksonXmlRootElement(localName="flatDataset")
@JsonTypeName(value="dataset#flat")
@JsonDeserialize(builder = Builder.class)
public class FlatDataset extends DatasetWithView implements DatasetLocationModel {
    
    private FlatDataset(){}
    
    /**
     * Copy constructor. Can convert a FullDataset to a FlatDataset.
     * 
     * @param dataset 
     */
    public FlatDataset(Dataset dataset){
        super(dataset, true);
    }
    
    protected FlatDataset(Dataset.Builder builder){
        super(builder, true);
    }
        
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getLocationPk(){ 
        
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getPk();
        }
        return null; 
    }
    
    @JsonIgnore
    public DatasetVersionModel getVersion(){ 
        return getViewInfo().getVersion();
    }
    
    @JsonIgnore
    public DatasetLocationModel getLocation(){ 
        return getViewInfo().singularLocationOpt().orNull();
    }
    
    // Location info
    @Override 
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getResource(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getResource();
        }
        return null; 
    }
    
    @Override 
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getSize(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getSize();
        }
        return null; 
    }
    
    @Override 
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getChecksum(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getChecksum();
        }
        return null; 
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("locationModified")
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    public Timestamp getDateLocationModified(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getDateModified();
        }
        return null; 
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("locationRegistered")
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    public Timestamp getDateLocationCreated(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getDateCreated();
        }
        return null; 
    }
    
    @Override 
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("locationScanned")
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    public Timestamp getDateScanned(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getDateScanned();
        }
        return null; 
    }
    
    @Override 
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getSite(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getSite();
        }
        return null; 
    }
    
    @Override 
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getScanStatus(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return getViewInfo().singularLocationOpt().get().getScanStatus();
        }
        return null; 
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getEventCount(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return ((DatasetLocation) getViewInfo().singularLocationOpt().get()).getEventCount();
        }
        return null; 
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getRunMin(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return ((DatasetLocation) getViewInfo().singularLocationOpt().get()).getRunMin();
        }
        return null; 
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getRunMax(){ 
        if(getViewInfo().singularLocationOpt().isPresent()){
            return ((DatasetLocation) getViewInfo().singularLocationOpt().get()).getRunMax();
        }
        return null; 
    }
    
    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
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
    
    /**
     * Builder.
     */
    public static class Builder extends Dataset.Builder{
        
        public Builder(){}
        
        /**
         * Copy constructor.
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
    @JsonIgnore
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
