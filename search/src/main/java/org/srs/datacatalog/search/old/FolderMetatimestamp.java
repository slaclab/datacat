
package org.srs.datacatalog.search.old;

import java.sql.Timestamp;
import org.srs.datacatalog.search.tables.Metatable;
import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "LogicalFolderMetaTimestamp")
public class FolderMetatimestamp extends Metatable<Timestamp> {

    public FolderMetatimestamp(){
        super("LogicalFolder" );
    }

}
