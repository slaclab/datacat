
package org.srs.datacatalog.search.old;

import org.srs.datacatalog.search.tables.Metatable;
import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "DatasetGroupMetastring")
public class GroupMetastring extends Metatable<String> {

    public GroupMetastring(){
        super("DatasetGroup" );
    }

}
