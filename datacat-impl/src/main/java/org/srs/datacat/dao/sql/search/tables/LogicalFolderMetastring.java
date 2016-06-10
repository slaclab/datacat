
package org.srs.datacat.dao.sql.search.tables;

import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "LogicalFolderMetaString")
public class LogicalFolderMetastring extends Metatable<String> {

    public LogicalFolderMetastring(){
        super("LogicalFolder");
    }

}
