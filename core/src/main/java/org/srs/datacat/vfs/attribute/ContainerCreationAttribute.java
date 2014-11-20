
package org.srs.datacat.vfs.attribute;

import java.nio.file.attribute.FileAttribute;
import org.srs.datacat.shared.DatacatObject;

/**
 * 
 * @author bvan
 */
public class ContainerCreationAttribute implements FileAttribute<DatacatObject>{
    DatacatObject request;
    
    public ContainerCreationAttribute(DatacatObject request){
        this.request = request;
    }

    @Override
    public String name(){
        return "createContainer";
    }

    @Override
    public DatacatObject value(){
        return request;
    }

}
