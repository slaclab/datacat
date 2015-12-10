
package org.srs.datacat.dao.sql.search.tables;

import org.zerorm.core.Column;
import org.zerorm.core.Table;
import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "VerDatasetLocation")
public class Location extends Table {
    @Schema public Column datasetLocation;
    @Schema public Column datasetVersion;
    @Schema(alias = "site") public Column<String> datasetSite;
    @Schema public Column path;
    @Schema public Column runMin;
    @Schema public Column runMax;
    @Schema(alias = "eventCount") public Column numberEvents;
    @Schema public Column fileSizeBytes;
    @Schema public Column checksum;
    @Schema public Column scanStatus;

    public Location(){
        super();
    }

}
