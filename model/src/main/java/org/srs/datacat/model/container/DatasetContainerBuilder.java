
package org.srs.datacat.model.container;

import java.util.Map;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetContainer;

/**
 *
 * @author bvan
 */
public interface DatasetContainerBuilder<U extends DatasetContainerBuilder> extends DatacatNode.DatacatNodeBuilder<U> {

    DatasetContainer build();
    U create(DatacatNode val);
    U stat(ContainerStat stat);
    U metadata(Map<String, Object> val);
    U description(String val);

}
