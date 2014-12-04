
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.srs.datacat.model.DatasetContainer;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.shared.DatasetGroup.Builder;

/**
 * A DatasetGroup. A DatasetGroup is  a special container which contains only Datasets.
 * 
 * @author bvan
 */
@JsonTypeName(value="group")
@JsonDeserialize(builder=Builder.class)
public class DatasetGroup extends DatacatObject implements DatasetContainer {
    private BasicStat stat;
    private String description;

    public DatasetGroup(){
        super();
    }
    
    public DatasetGroup(DatacatObject object){
        super(object);
    }
    
    /**
     * Copy constructor.
     * 
     * @param group 
     */
    public DatasetGroup(DatasetGroup group){
        super(group);
        this.stat = group.stat;
        this.description = group.description;
    }
    
    public DatasetGroup(DatacatObject.Builder builder){
        super(builder);
    }
    
    public DatasetGroup(DatasetContainer.Builder builder){
        super(builder);
        this.stat = builder.stat;
        this.description = builder.description;
    }
    
   
    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDescription() {
        return description;
    }
    
    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public BasicStat getStat(){
        return stat;
    }
    
    /**
     * Builder.
     */
    public static class Builder extends DatasetContainer.Builder {
        
        public Builder(){ super(); }
        public Builder(DatacatObject object){ super(object); }
        public Builder(DatacatNode object){ super(object); }
        public Builder(DatasetContainer.Builder builder){ super(builder); }
        
        @Override
        public DatasetGroup build(){
            return new DatasetGroup(this);
        }
    }
}
