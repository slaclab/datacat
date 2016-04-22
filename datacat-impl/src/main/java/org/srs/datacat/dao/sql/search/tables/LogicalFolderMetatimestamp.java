
package org.srs.datacat.dao.sql.search.tables;

import java.sql.Timestamp;
import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "LogicalFolderMetaTimestamp")
public class LogicalFolderMetatimestamp extends Metatable<Timestamp> {

    public LogicalFolderMetatimestamp(){
        super("LogicalFolder");
    }

}
