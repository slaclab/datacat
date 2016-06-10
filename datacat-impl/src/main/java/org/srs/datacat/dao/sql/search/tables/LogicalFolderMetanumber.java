
package org.srs.datacat.dao.sql.search.tables;

import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "LogicalFolderMetaNumber")
public class LogicalFolderMetanumber extends Metatable<Number> {

    public LogicalFolderMetanumber(){
        super("LogicalFolder" );
    }

}
