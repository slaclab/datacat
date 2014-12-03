
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.shared.container.BasicStat;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.srs.datacat.shared.LogicalFolder.Builder;

/**
 * A LogicalFolder is the fundamental container which is used to create a hierarchy.
 * 
 * @author bvan
 */
@JsonTypeName(value="folder")
@JsonDeserialize(builder=Builder.class)
public class LogicalFolder extends DatacatObject implements DatasetContainer {

    private BasicStat stat;
    
    public LogicalFolder(){
        super();
    }
    
    public LogicalFolder(DatacatObject object){
        super(object);
    }
    
    public LogicalFolder(LogicalFolder folder){
        super(folder);
        this.stat = folder.stat;
        this.description = folder.description;
    }
    
    public LogicalFolder(DatacatObject.Builder builder){
        super(builder);
    }
    
    public LogicalFolder(DatasetContainer.Builder builder){
        super(builder);
        this.stat = builder.stat;
        this.description = builder.description;
    }
    
    private String description = null;

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDescription() { return description; }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public BasicStat getStat(){
        return this.stat;
    }
    
    public void setDescription(String desc){ this.description = desc; }
    
    public void setStat(BasicStat stat){
        this.stat = stat;
    }
    
    /**
     * Builder.
     */
    public static class Builder extends DatasetContainer.Builder {
        public Builder(){ super(); }
        public Builder(DatacatObject o){ super(o); }
        public Builder(DatasetContainer.Builder o){ super(o); }
        
        @Override
        public LogicalFolder build(){
            return new LogicalFolder(this);
        }
    }
}
