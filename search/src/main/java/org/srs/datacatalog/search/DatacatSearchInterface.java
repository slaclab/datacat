
package org.srs.datacatalog.search;

import java.util.List;
import java.util.Map;
import org.srs.rest.datacat.shared.Dataset;
import org.srs.rest.datacat.shared.DatasetGroup;
import org.srs.rest.datacat.shared.LogicalFolder;

/**
 *
 * @author bvan
 */
public interface DatacatSearchInterface {

    Object getDataCatObject(String dcPath);

    Dataset getDataset(String datasetPath);

    Dataset getDataset(String datasetPath, int versionID);

    Map<String, String> getDatasetDataTypes();

    Map<String, String> getDatasetFileTypes();

    LogicalFolder getDatasetFolder(String folderPath);

    LogicalFolder getDatasetFolder(long folderPK);

    DatasetGroup getDatasetGroup(String datasetGroupPath);

    int getDatasetLatestVersion(String datasetName, String logicalFolderPath, String groupName);
    
    List<DatasetGroup> getDatasetGroups(String logicalFolderPath, String searchCriteria);

    List<Dataset> getDatasets(String logicalFolderPath, String searchCriteria);

    List<Dataset> getDatasets(String logicalFolderPath, String searchCriteria, String[] sites);

    // Old way of doing it with a folderPath String:
    List<Dataset> getDatasets(String searchPath, boolean recurseFolders, boolean searchFolders,
            boolean searchGroups, String searchCriteria, String[] sites, String[] metaFieldsToRetrieve, 
            String[] sortFields);
    
}
