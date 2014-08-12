
package org.srs.datacatalog.search.tables;

import java.sql.Timestamp;
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
