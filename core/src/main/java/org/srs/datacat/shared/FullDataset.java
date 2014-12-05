
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Set;
import org.srs.datacat.model.DatasetLocationModel;
import org.srs.datacat.model.DatasetVersionModel;
import org.srs.datacat.shared.FullDataset.Builder;

/**
 * Dataset with a Version, which also must contain locations.
 * @author bvan
 */
@JacksonXmlRootElement(localName="fullDataset")
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
    
    @JsonIgnore
    public DatasetVersionModel getVersion(){
        return getViewInfo().getVersion();
    }
            
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Set<DatasetLocationModel> getLocations(){
        return getViewInfo().getLocations();
    }

    /**
     * Builder.
     */
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
