
package org.srs.datacat.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.DatasetGroup;
import org.srs.datacat.shared.LogicalFolder;
import org.srs.datacat.shared.BasicStat;

/**
 * An interface denoting either a folder or a group.
 * 
 * @author bvan
 */
public interface DatasetContainer extends DatacatNode, HasMetadata {
    
    BasicStat getStat();
    String getDescription();    

    /**
     * Builder for DatasetContainers.
     * 
     * @author bvan
     */
    public static class Builder extends DatacatObject.Builder<Builder> {
        public BasicStat stat = null;
        public String description = null;

        public Builder(){
            super();
        }
        
        public Builder(DatacatNode object){
            super(object);
            if(object instanceof DatasetContainer){
                this.description = ((DatasetContainer) object).getDescription();
                this.stat = ((DatasetContainer) object).getStat();
            }
        }
        
        public Builder(DatacatObject object){
            super(object);
            if(object instanceof DatasetContainer){
                this.description = ((DatasetContainer) object).getDescription();
                this.stat = ((DatasetContainer) object).getStat();
            }
        }

        public Builder(DatasetContainer.Builder builder){
            super(builder);
            this.stat = builder.stat;
            this.description = builder.description;
        }
        
        public static Builder create(DatacatNode container){
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
        public DatacatObject build(){
            if(type == DatacatObject.Type.GROUP){
                return new DatasetGroup.Builder(this).build();
            }
            return new LogicalFolder.Builder(this).build();    
        }

        @JsonSetter
        public Builder stat(BasicStat val){
            this.stat = val;
            return this;
        }

        @JsonSetter
        public Builder description(String val){
            this.description = val;
            return this;
        }
    }
    
}
