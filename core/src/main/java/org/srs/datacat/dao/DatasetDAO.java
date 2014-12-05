
package org.srs.datacat.dao;

import com.google.common.base.Optional;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.Set;
import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.model.DatasetLocationModel;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetVersionModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.DatasetViewInfo;

/**
 *
 * @author bvan
 */
public interface DatasetDAO extends BaseDAO {
    
    DatasetModel createDataset(DatacatRecord parent, String dsName, Optional<DatasetModel> dsReq, 
            Optional<DatasetViewInfo> viewInfo, Set options) throws IOException;
    
    void patchDataset(DatacatRecord dataset, DatasetView view, Optional<DatasetModel> dsReq, 
            Optional<DatasetViewInfo> viewInfo) throws IOException;
    
    DatasetVersionModel createOrMergeDatasetVersion(DatacatRecord dsRecord, DatasetVersionModel request,
            Optional<DatasetVersionModel> curVersionOpt, boolean mergeVersion) throws IOException, FileSystemException;
    
    DatasetLocationModel createDatasetLocation(DatacatRecord versionRecord, DatasetLocationModel newLoc,
            boolean skipCheck) throws IOException, FileSystemException;

    DatasetVersionModel getCurrentVersion(DatacatRecord dsRecord) throws IOException;

    DatasetViewInfo getDatasetViewInfo(DatacatRecord dsRecord, DatasetView view) throws IOException;
    
}
