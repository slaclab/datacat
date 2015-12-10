
package org.srs.datacat.dao.sql.search.tables;

import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "VerDatasetMetaString")
public class DatasetMetastring extends Metatable<String> {

    public DatasetMetastring(){
        super("DatasetVersion" );
    }

}
