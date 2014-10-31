
package org.srs.datacat.shared.dataset;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.dataset.FullDataset.Builder;

/**
 * Dataset with a Version, which also must contain locations
 * @author bvan
 */
@XmlRootElement
@XmlType(name="fullDataset")
@JsonTypeName(value="dataset#full")
@JsonDeserialize(builder = Builder.class)
public class FullDataset extends DatasetWithView {
    
    private FullDataset(){}
    
    /**
     * Copy constructor. Can convert other datasets, if needed.
     * @param dataset 
     */
    public FullDataset(Dataset dataset){
        super(dataset, false);
    }

    public FullDataset(Dataset.Builder builder){
        super(builder, false);
    }
    
    @XmlTransient
    public DatasetVersion getVersion(){
        return getViewInfo().getVersion();
    }
            
    @XmlElement(required=false)
    public Set<DatasetLocation> getLocations(){
        return getViewInfo().getLocations();
    }

    @XmlTransient
    public static class Builder extends Dataset.Builder{
        
        public Builder(){}
        
        public Builder(Dataset.Builder builder){
            super(builder);
        }
        
        @Override
        public FullDataset build(){
            return new FullDataset(this);
        }
    }

}
