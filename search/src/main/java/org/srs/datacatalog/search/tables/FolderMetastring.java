
package org.srs.datacatalog.search.tables;

import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "LogicalFolderMetastring")
public class FolderMetastring extends Metatable<String> {

    public FolderMetastring(){
        super("LogicalFolder" );
    }

}