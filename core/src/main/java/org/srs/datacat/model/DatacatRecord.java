
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

}
