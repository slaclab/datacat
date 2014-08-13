
package org.srs.datacat.shared;

import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.shared.container.BasicStat;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.xml.bind.annotation.XmlTransient;
import org.srs.datacat.shared.DatasetGroup.Builder;

/**
 *
 * @author bvan
 */
@XmlRootElement
@XmlType(name="group")
@JsonTypeName(value="group")
@JsonDeserialize(builder=Builder.class)
public class DatasetGroup extends DatacatObject implements DatasetContainer {
    private BasicStat stat;
    private String description;
    private List<DatacatObject> children = null;

    public DatasetGroup(){
        super();
    }
    
    public DatasetGroup(DatacatObject object){
        super(object);
    }
    
    /**
     * Copy constructor
     * @param group 
     */
    public DatasetGroup(DatasetGroup group){
        super(group);
        this.stat = group.stat;
        this.description = group.description;
    }
    
    public DatasetGroup(DatacatObjectBuilder builder){
        super(builder);
    }
    
    public DatasetGroup(DatasetContainer.Builder builder){
        super(builder);
        this.stat = builder.stat;
        this.description = builder.description;
    }
    
    public DatasetGroup(DatacatObject object, List<DatacatObject> children){
        this(object);
        this.children = children;
    }
    
    @Override
    @XmlElement(required=false)
    public String getDescription() {
        return description;
    }
    
    public List<DatacatObject> listChildren() {
        return children;
    }

    @Override
    @XmlElement(required=false)
    public BasicStat getStat(){
        return stat;
    }
    
    @XmlTransient
    public static class Builder extends DatasetContainer.Builder<DatasetGroup> {
        
        public Builder(){super();}
        public Builder(DatacatObject object){
            super(object);
        }
        
        @Override
        public DatasetGroup build(){
            return new DatasetGroup(this);
        }
    }
}
