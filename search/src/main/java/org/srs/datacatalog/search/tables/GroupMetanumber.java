
package org.srs.datacatalog.search.tables;

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
