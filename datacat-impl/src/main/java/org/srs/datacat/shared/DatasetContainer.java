
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.srs.datacat.model.container.ContainerStat;

/**
 *
 * @author bvan
 */
public abstract class DatasetContainer extends DatacatObject 
    implements org.srs.datacat.model.DatasetContainer {
    
    protected ContainerStat stat;
    private String description = null;
    
    public DatasetContainer(){ }
    
    public DatasetContainer(DatacatObject object){
        super(object);
    }
    
    public DatasetContainer(DatasetContainer container){
        super(container);
        this.stat = container.stat;
        this.description = container.description;
    }
    
    public DatasetContainer(DatacatObject.Builder builder){
        super(builder);
    }
    
    public DatasetContainer(DatasetContainerBuilder builder){
        super(builder);
        this.stat = builder.stat;
        this.description = builder.description;
    }
    
    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDescription() { return description; }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ContainerStat getStat(){
        return this.stat;
    }

}
