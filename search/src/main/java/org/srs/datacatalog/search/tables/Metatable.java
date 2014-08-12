
package org.srs.datacatalog.search.tables;

import org.zerorm.core.Column;
import org.zerorm.core.Table;
import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
public class Metatable<T> extends Table {
    @Schema public Column<Long> datacatKey;
    @Schema public Column<String> metaName;
    @Schema public Column<T> metaValue;

    public Metatable(){
        super();
    }
    
    public Metatable(String datacatKeyName){
        this();
        datacatKey.setName( datacatKeyName );
    }
}
