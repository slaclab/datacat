
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetContainer.Folder;
import org.srs.datacat.shared.LogicalFolder.Builder;

/**
 * A LogicalFolder is the fundamental container which is used to create a hierarchy.
 * 
 * @author bvan
 */
@JsonTypeName(value="folder")
@JsonDeserialize(builder=Builder.class)
public class LogicalFolder extends DatasetContainer implements Folder {
    
    public LogicalFolder(){
        super();
    }
    
    public LogicalFolder(DatacatObject object){
        super(object);
    }
    
    public LogicalFolder(LogicalFolder folder){
        super(folder);
    }
    
    public LogicalFolder(DatacatObject.Builder builder){
        super(builder);
    }
    
    public LogicalFolder(DatasetContainerBuilder builder){
        super(builder);
    }
    
    /**
     * Builder.
     */
    public static class Builder extends DatasetContainerBuilder {
        public Builder(){ super(); }
        public Builder(DatacatNode o){ super(o); }
        public Builder(DatasetContainerBuilder o){ super(o); }
        
        @Override
        public LogicalFolder build(){
            return new LogicalFolder(this);
        }
    }
}
