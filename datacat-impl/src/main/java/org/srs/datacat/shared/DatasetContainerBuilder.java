
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonSetter;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.RecordType;
import org.srs.datacat.model.container.ContainerStat;

/**
 * Builder for DatasetContainers.
 *
 * @author bvan
 */
public class DatasetContainerBuilder extends DatacatObject.Builder<DatasetContainerBuilder> 
    implements org.srs.datacat.model.container.DatasetContainerBuilder<DatasetContainerBuilder>  {
    
    public ContainerStat stat = null;
    public String description = null;

    public DatasetContainerBuilder(){
        super();
    }

    public DatasetContainerBuilder(DatacatNode object){
        super(object);
        if(object instanceof org.srs.datacat.model.DatasetContainer){
            this.description = ((org.srs.datacat.model.DatasetContainer) object).getDescription();
            this.stat = ((org.srs.datacat.model.DatasetContainer) object).getStat();
        }
    }

    public DatasetContainerBuilder(DatasetContainerBuilder builder){
        super(builder);
        this.stat = builder.stat;
        this.description = builder.description;
    }

    @Override
    public DatasetContainerBuilder create(DatacatNode container){
        if(container.getType() == null){
            return new LogicalFolder.Builder(container);
        }
        switch(container.getType()){
            case FOLDER:
                return new LogicalFolder.Builder(container);
            case GROUP:
                return new DatasetGroup.Builder(container);
            default:
                return null;
        }
    }

    @Override
    public DatasetContainer build(){
        if(type == RecordType.GROUP){
            return new DatasetGroup.Builder(this).build();
        }
        return new LogicalFolder.Builder(this).build();
    }

    @Override
    @JsonSetter
    public DatasetContainerBuilder stat(ContainerStat val){
        this.stat = val;
        return this;
    }

    @Override
    @JsonSetter
    public DatasetContainerBuilder description(String val){
        this.description = val;
        return this;
    }

}
