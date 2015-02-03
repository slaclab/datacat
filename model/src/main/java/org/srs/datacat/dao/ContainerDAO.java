
package org.srs.datacat.dao;

import com.google.common.base.Optional;
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
            Optional<DatasetView> viewPrefetch) throws IOException;
    
    <V extends ContainerStat> V getStat(DatacatRecord container, Class<V> statType) throws IOException;

}
