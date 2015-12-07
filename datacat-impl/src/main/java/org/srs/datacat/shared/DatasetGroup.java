
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetContainer.Group;
import org.srs.datacat.shared.DatasetGroup.Builder;

/**
 * A DatasetGroup. A DatasetGroup is  a special container which contains only Datasets.
 * 
 * @author bvan
 */
@JsonTypeName(value="group")
@JsonDeserialize(builder=Builder.class)
public class DatasetGroup extends DatasetContainer implements Group {

    public DatasetGroup(){
        super();
    }
    
    /**
     * Copy constructor.
     * 
     * @param group 
     */
    public DatasetGroup(DatasetGroup group){
        super(group);
    }
    
    public DatasetGroup(DatacatObject.Builder builder){
        super(builder);
    }
    
    public DatasetGroup(DatasetContainerBuilder builder){
        super(builder);
    }
    
    /**
     * Builder.
     */
    public static class Builder extends DatasetContainerBuilder {
        
        public Builder(){ super(); }
        public Builder(DatacatObject object){ super(object); }
        public Builder(DatacatNode object){ super(object); }
        public Builder(DatasetContainerBuilder builder){ super(builder); }
        
        @Override
        public DatasetGroup build(){
            return new DatasetGroup(this);
        }
    }
}
