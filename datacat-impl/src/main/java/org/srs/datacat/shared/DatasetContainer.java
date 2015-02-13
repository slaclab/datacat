
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.srs.datacat.model.container.ContainerStat;

/**
 *
 * @author bvan
 */
@JsonDeserialize(builder=DatasetContainerBuilder.class)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="_type", defaultImpl=DatasetContainer.class)
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
    
    @Patchable(column="Description")
    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDescription() { return description; }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ContainerStat getStat(){
        return this.stat;
    }

}
