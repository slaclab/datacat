
package org.srs.datacat.dao;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.BasicStat;
import org.srs.datacat.shared.DatasetStat;

/**
 *
 * @author bvan
 */
public interface ContainerDAO extends BaseDAO {

    DirectoryStream<DatacatNode> getChildrenStream(DatacatRecord parent, 
            DatasetView viewPrefetch) throws IOException;
    
    DirectoryStream<DatacatNode> getSubdirectoryStream(DatacatRecord parent) throws IOException;
    
    BasicStat getBasicStat(DatacatRecord container) throws IOException;

    DatasetStat getDatasetStat(DatacatRecord container) throws IOException;

}
