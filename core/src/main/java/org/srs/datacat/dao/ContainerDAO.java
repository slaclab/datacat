
package org.srs.datacat.dao;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.container.BasicStat;
import org.srs.datacat.shared.container.DatasetStat;

/**
 *
 * @author bvan
 */
public interface ContainerDAO extends BaseDAO {

    DatacatObject createContainer(DatacatRecord parent, String targetPath, DatacatObject request) throws IOException;

    void deleteContainer(DatacatRecord container) throws IOException;

    BasicStat getBasicStat(DatacatRecord container) throws IOException;

    DirectoryStream<DatacatObject> getChildrenStream(DatacatRecord parent, DatasetView viewPrefetch) throws IOException;

    DatasetStat getDatasetStat(DatacatRecord container) throws IOException;

    DirectoryStream<DatacatObject> getSubdirectoryStream(DatacatRecord parent) throws IOException;
    
}
