
package org.srs.datacat.dao.sql.search.tables;

import org.zerorm.core.Column;
import org.zerorm.core.Table;
import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "DatasetVersion")
public class Version extends Table {
    @Schema
    Column datasetVersion;
    @Schema
    Column dataset;
    @Schema
    Column<Integer> versionId;
    @Schema
    Column masterLocation;

    public Version(){
        super();
    }

}
