
package org.srs.datacat.model;

import java.sql.Timestamp;

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
