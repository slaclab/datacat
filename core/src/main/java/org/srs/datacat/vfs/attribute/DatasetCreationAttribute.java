
package org.srs.datacat.vfs.attribute;

import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.HashSet;
import org.srs.datacat.shared.Dataset;

/**
 *
 * @author bvan
 */
public class DatasetCreationAttribute implements FileAttribute<Dataset>{
    private final Dataset ds;
    private final HashSet<DatasetOption> options = new HashSet<>();
    
    public DatasetCreationAttribute(Dataset ds, DatasetOption... opts){
        this.ds = ds;
        this.options.addAll( Arrays.asList( opts ) );
    }

    @Override
    public String name(){
        return "createds";
    }

    @Override
    public Dataset value(){
        return ds;
    }
    
    public HashSet<DatasetOption> getOptions(){
        return options;
    }

}
