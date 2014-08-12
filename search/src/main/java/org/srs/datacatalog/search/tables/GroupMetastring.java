
package org.srs.datacatalog.search.tables;

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
