
package org.srs.datacat.dao;

import com.google.common.base.Optional;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.Set;
import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.dataset.DatasetViewInfo;

/**
 *
 * @author bvan
 */
public interface DatasetDAO extends BaseDAO {
    
    Dataset createDataset(DatacatRecord parent, String dsName, Optional<Dataset> dsReq, Optional<DatasetViewInfo> viewInfo, Set options) throws IOException;
    
    DatasetVersion createOrMergeDatasetVersion(DatacatRecord dsRecord, DatasetVersion request,
            Optional<DatasetVersion> curVersionOpt, boolean mergeVersion) throws IOException, FileSystemException;
    
    DatasetLocation createDatasetLocation(DatacatRecord versionRecord, DatasetLocation newLoc,
            boolean skipCheck) throws IOException, FileSystemException;

    DatasetVersion getCurrentVersion(DatacatRecord dsRecord) throws IOException;

    DatasetViewInfo getDatasetViewInfo(DatacatRecord dsRecord, DatasetView view) throws IOException;
    
}
