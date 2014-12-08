
package org.srs.datacat.model;

import java.util.Map;

/**
 *
 * @author bvan
 */
public interface DatacatNodeBuilder<U extends DatacatNodeBuilder> extends DatacatRecordBuilder<U>, 
        Builder {

    U acl(String val);

    U metadata(Map<String, Object> val);

    U name(String val);

    U parentPk(Long val);

}
