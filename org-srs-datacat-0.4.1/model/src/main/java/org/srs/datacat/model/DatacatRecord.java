
package org.srs.datacat.model;

import java.io.Serializable;

/**
 *
 * @author bvan
 */
public interface DatacatRecord extends Serializable {
    
    Long getPk();
    RecordType getType();
    String getPath();

    /**
     * Builder interface.
     * @author bvan
     */
    public interface DatacatRecordBuilder<U extends DatacatRecordBuilder> {
        DatacatRecord build();

        U path(String val);

        U pk(Long val);

        U type(RecordType dType);
    }

}
