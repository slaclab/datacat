
package org.srs.datacat.dao;

import com.google.common.base.Optional;
import java.io.IOException;
import java.util.Set;
import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.dataset.DatasetViewInfoModel;

/**
 *
 * @author bvan
 */
public interface DatasetDAO extends BaseDAO {
    
    DatasetModel createDataset(DatacatRecord parent, String dsName, Optional<DatasetModel> dsReq, 
            Optional<DatasetViewInfoModel> viewInfo, Set options) throws IOException;
    
    void patchDataset(DatacatRecord dataset, DatasetView view, Optional<DatasetModel> dsReq, 
            Optional<DatasetViewInfoModel> viewInfo) throws IOException;

    DatasetViewInfoModel getDatasetViewInfo(DatacatRecord dsRecord, DatasetView view) throws IOException;
    
}
