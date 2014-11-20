
package org.srs.datacat.model;

import java.io.Serializable;
import org.srs.datacat.shared.DatacatObject;

/**
 *
 * @author bvan
 */
public interface DatacatRecord extends Serializable {
    
    Long getPk();
    DatacatObject.Type getType();
    String getPath();
}
