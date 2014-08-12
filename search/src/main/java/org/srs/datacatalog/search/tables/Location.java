
package org.srs.datacatalog.search.tables;

import org.zerorm.core.Column;
import org.zerorm.core.Table;
import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "VerdatasetLocation")
public class Location extends Table {
    @Schema public Column datasetLocation;
    @Schema public Column datasetVersion;
    @Schema public Column datasetSite;
    @Schema public Column path;
    @Schema public Column runMin;
    @Schema public Column runMax;
    @Schema public Column numberEvents;
    @Schema public Column fileSizeBytes;
    @Schema public Column checkSum;
    @Schema public Column scanstatus;

    public Location(){
        super();
    }

}
