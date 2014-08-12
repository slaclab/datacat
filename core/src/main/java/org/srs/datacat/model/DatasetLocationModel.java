
package org.srs.datacat.model;

import java.sql.Timestamp;

/**
 *
 * @author bvan
 */
public interface DatasetLocationModel {

    String getFileSystemPath();
    Long getFileSize();
    Long getCheckSum();
    Timestamp getDateModified();
    Timestamp getDateCreated();
    Timestamp getDateScanned();
    String getSite();
    String getScanStatus();
    Long getEventCount();
    Long getRunMax();
    Long getRunMin();
    Boolean isMaster();
}
