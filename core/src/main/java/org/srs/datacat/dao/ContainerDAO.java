
package org.srs.datacat.dao;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import org.srs.datacat.model.container.ContainerStat;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.model.DatasetView;

/**
 *
 * @author bvan
 */
public interface ContainerDAO extends BaseDAO {

    DirectoryStream<DatacatNode> getChildrenStream(DatacatRecord parent, 
            DatasetView viewPrefetch) throws IOException;
    
    DirectoryStream<DatacatNode> getSubdirectoryStream(DatacatRecord parent) throws IOException;
    
    <V extends ContainerStat> V getStat(DatacatRecord container, Class<V> statType) throws IOException;

}
