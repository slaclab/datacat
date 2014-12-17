
package org.srs.datacat.model;

import java.util.Map;

/**
 * A Node object - either a Dataset or Container.
 * @author bvan
 */
public interface DatacatNode extends DatacatRecord {
    
    Long getParentPk();
    String getName();
    String getAcl();

    /**
     * Builder interface.
     * @author bvan
     */
    public interface DatacatNodeBuilder<U extends DatacatNodeBuilder> extends DatacatRecordBuilder<U>, Builder {
        U create(DatacatNode val);

        U acl(String val);

        U metadata(Map<String, Object> val);

        U name(String val);

        U parentPk(Long val);
    }
    
}
