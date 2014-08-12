
package org.srs.datacatalog.search;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.srs.rest.datacat.shared.Dataset;
import org.srs.rest.datacat.shared.DatasetGroup;
import org.srs.rest.datacat.shared.LogicalFolder;

/**
 *
 * @author bvan
 */
public interface DatacatUpdateInterface {
    
    Map<String, Object> addLogicalFolderMetaData(LogicalFolder folder, List<String> metaData);

    Map<String, Object> addLogicalFolderMetaData(LogicalFolder folder, Map<String, Object> metaData);

    Map<String, Object> addDatasetGroupMetaData(DatasetGroup datasetGroup, List<String> metaData);

    Map<String, Object> addDatasetGroupMetaData(DatasetGroup datasetGroup, Map<String, Object> metaData);

    void addDatasetLocation(Dataset dataset, String site, String filePath);

    Map<String, Object> addDatasetMetaData(Dataset dataset, List<String> metaData);

    Map<String, Object> addDatasetMetaData(Dataset dataset, Map<String, Object> metaData);

    Dataset newDataset(String name, String fileFormat, String dataType, String datacatPath, String site, String location);

    Dataset registerDataset(Dataset ds, Map<String, Object> metaData);

    Dataset registerDataset(Dataset ds, Map<String, Object> metaData, boolean replaceExisting);

    Dataset registerDataset(Dataset ds, List<String> metaData);

    Dataset registerDataset(Dataset ds, List<String> metaData, boolean replaceExisting);

    Dataset registerDataset(String dataType, String logicalPath, String filePath);

    Dataset registerDataset(String dataType, String logicalPath, String filePath, String attributes);
    
    DatasetGroup addGroup(String fullPath, String dataType);
    DatasetGroup addGroup(String path, String name, String dataType);
    
    LogicalFolder addFolder(String fullPath);
    LogicalFolder addFolder(String path, String name);
    
}

