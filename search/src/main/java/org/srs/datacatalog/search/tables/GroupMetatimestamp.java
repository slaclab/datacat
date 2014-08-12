
package org.srs.datacatalog.search.tables;

import java.sql.Timestamp;
import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "DatasetGroupMetaTimestamp")
public class GroupMetatimestamp extends Metatable<Timestamp> {

    public GroupMetatimestamp(){
        super("DatasetGroup" );
    }

}
