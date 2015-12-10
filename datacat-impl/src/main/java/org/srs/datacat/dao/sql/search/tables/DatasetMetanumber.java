
package org.srs.datacat.dao.sql.search.tables;

import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "VerDatasetMetaNumber")
public class DatasetMetanumber extends Metatable<Number> {

    public DatasetMetanumber(){
        super("DatasetVersion" );
    }

}
