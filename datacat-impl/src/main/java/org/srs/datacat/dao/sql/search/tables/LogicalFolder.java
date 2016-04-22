
package org.srs.datacat.dao.sql.search.tables;

import org.zerorm.core.Column;
import org.zerorm.core.Table;
import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "DatasetLogicalFolder")
public class LogicalFolder extends Table {
    @Schema public Column<Long> datasetLogicalFolder;
    @Schema public Column<String> name;
    @Schema public Column<Long> parent;
    @Schema public Column<String> description;
   
    public LogicalFolder(){
        super();
    }
}

