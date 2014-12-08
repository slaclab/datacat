
package org.srs.datacat.vfs.attribute;

import java.nio.file.attribute.FileAttribute;
import org.srs.datacat.model.DatacatNode;


/**
 * 
 * @author bvan
 */
public class ContainerCreationAttribute implements FileAttribute<DatacatNode>{
    DatacatNode request;
    
    public ContainerCreationAttribute(DatacatNode request){
        this.request = request;
    }

    @Override
    public String name(){
        return "createContainer";
    }

    @Override
    public DatacatNode value(){
        return request;
    }

}
