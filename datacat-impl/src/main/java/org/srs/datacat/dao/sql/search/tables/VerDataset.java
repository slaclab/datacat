
package org.srs.datacat.dao.sql.search.tables;

import java.sql.Timestamp;
import org.zerorm.core.Column;
import org.zerorm.core.Table;
import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "VerDataset")
public class VerDataset extends Table {
    @Schema public Column<Long> dataset;
    @Schema(alias = "name") public Column<String> datasetName;
    @Schema(alias = "fileFormat") public Column<String> datasetFileFormat;
    @Schema(alias = "dataType") public Column<String> datasetDataType;
    @Schema public Column<Long> datasetlogicalfolder;
    @Schema public Column<Long> datasetGroup;
    @Schema public Column<Long> latestVersion;
    @Schema(alias = "created") public Column<Timestamp> registered;

    public VerDataset(){
        super();
    }

}
