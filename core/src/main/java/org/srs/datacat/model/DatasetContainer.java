
package org.srs.datacat.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import javax.xml.bind.annotation.XmlTransient;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.DatacatObjectBuilder;
import org.srs.datacat.shared.container.BasicStat;

/**
 * An interface denoting either a folder or a group
 * @author bvan
 */
public interface DatasetContainer {
    
    public BasicStat getStat();
    public String getDescription();

    /**
     * Builder for DatasetContainers
     * @author bvan
     */
    @XmlTransient
    public static class Builder<T extends DatacatObject> extends DatacatObjectBuilder<T, Builder> {
        public BasicStat stat = null;
        public String description = null;

        public Builder(){
            super();
        }

        public Builder(DatacatObject object){
            super( DatacatObjectBuilder.create( object ) );
            if(object instanceof DatasetContainer){
                this.description = ((DatasetContainer) object).getDescription();
            }
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
