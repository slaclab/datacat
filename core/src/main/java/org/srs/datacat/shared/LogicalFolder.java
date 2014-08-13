
package org.srs.datacat.shared;

import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.shared.container.BasicStat;
import java.util.ArrayList;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.xml.bind.annotation.XmlTransient;
import org.srs.datacat.shared.LogicalFolder.Builder;

/**
 *
 * @author bvan
 */
@XmlRootElement(name="folder")
@XmlType(name="folder")
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
    
    public LogicalFolder(DatacatObject object, ArrayList<DatacatObject> children){
        this(object);
        this.children = children;
    }
    
    private ArrayList<DatacatObject> children = null;
    private String description = null;

    @XmlElement(required=false)
    public String getDescription() { return description; }
    
    @XmlElement(required=false)
    public BasicStat getStat(){
        return this.stat;
    }
    
    public void setDescription(String desc){ this.description = desc; }

    @XmlAnyElement
    @XmlElementWrapper
    public ArrayList<DatacatObject> getChildren() {
        return children;
    }
    
    public ArrayList<DatacatObject> listChildren() {
        return children;
    }
    
    public void addChild(DatacatObject o){
        if(children == null){
            children = new ArrayList<DatacatObject>();
        }
        children.add( o );
    }
    
    public void setStat(BasicStat stat){
        this.stat = stat;
    }
    
    @XmlTransient
    public static class Builder extends DatasetContainer.Builder {
        public Builder(){super();}
        public Builder(DatacatObject o){super(o);}
        public Builder(DatasetContainer.Builder o){super(o);}
        
        @Override
        public LogicalFolder build(){
            return new LogicalFolder(this);
        }
    }
}
