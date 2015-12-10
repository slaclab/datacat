
package org.srs.datacat.dao.sql.search.tables;

import java.sql.Timestamp;
import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "VerDatasetMetaTimestamp")
public class DatasetMetatimestamp extends Metatable<Timestamp> {

    public DatasetMetatimestamp(){
        super("DatasetVersion" );
    }

}
