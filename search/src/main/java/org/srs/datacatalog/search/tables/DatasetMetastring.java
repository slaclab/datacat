
package org.srs.datacatalog.search.tables;

import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "VerDatasetMetastring")
public class DatasetMetastring extends Metatable<String> {

    public DatasetMetastring(){
        super("DatasetVersion" );
    }

}
