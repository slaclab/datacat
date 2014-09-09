
package org.srs.datacatalog.search.old;

import org.srs.datacatalog.search.tables.Metatable;
import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "DatasetGroupMetanumber")
public class GroupMetanumber extends Metatable<Number> {

    public GroupMetanumber(){
        super("DatasetGroup" );
    }

}
