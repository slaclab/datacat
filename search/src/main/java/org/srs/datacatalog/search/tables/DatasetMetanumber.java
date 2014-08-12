
package org.srs.datacatalog.search.tables;

import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "VerDatasetMetanumber")
public class DatasetMetanumber extends Metatable<Number> {

    public DatasetMetanumber(){
        super("DatasetVersion" );
    }

}
