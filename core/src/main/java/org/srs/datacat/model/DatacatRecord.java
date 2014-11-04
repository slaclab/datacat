
package org.srs.datacat.model;

import java.io.Serializable;
import org.srs.datacat.shared.DatacatObject;

/**
 *
 * @author bvan
 */
public interface DatacatRecord extends Serializable {
    
    public Long getPk();
    public DatacatObject.Type getType();
    public String getPath();
}
