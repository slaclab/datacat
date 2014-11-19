
package org.srs.datacat.dao;

import com.google.common.base.Optional;
import java.io.IOException;
import java.nio.file.FileSystemException;
import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.dataset.DatasetViewInfo;

/**
 *
 * @author bvan
 */
public interface DatasetDAO extends BaseDAO {

    DatasetLocation createDatasetLocation(Long versionPk, String path, DatasetLocation newLoc,
            boolean skipCheck) throws IOException, FileSystemException;

    DatasetVersion createOrMergeDatasetVersion(DatacatRecord dsRecord, DatasetVersion request,
            Optional<DatasetVersion> curVersionOpt, boolean mergeVersion) throws IOException, FileSystemException;

    DatasetVersion getCurrentVersion(DatacatRecord dsRecord) throws IOException;

    DatasetViewInfo getDatasetViewInfo(DatacatRecord dsRecord, DatasetView view) throws IOException;
    
}
