
package org.srs.datacat.model.dataset;

import java.sql.Timestamp;
import org.srs.datacat.model.DatacatRecord;

/**
 *
 * @author bvan
 */
public interface DatasetLocationModel extends DatacatRecord {

    String getResource();
    Long getSize();
    String getChecksum();
    Timestamp getDateModified();
    Timestamp getDateCreated();
    Timestamp getDateScanned();
    String getSite();
    String getScanStatus();
    Boolean isMaster();
}
