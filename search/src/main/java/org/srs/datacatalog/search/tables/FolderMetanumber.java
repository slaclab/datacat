
package org.srs.datacatalog.search.tables;

import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "LogicalFolderMetanumber")
public class FolderMetanumber extends Metatable<Number> {

    public FolderMetanumber(){
        super("LogicalFolder" );
    }

}
