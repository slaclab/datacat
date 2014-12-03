
package org.srs.datacat.model;

import java.sql.Timestamp;

/**
 *
 * @author bvan
 */
public interface DatasetVersionModel extends DatacatRecord {

    String getDatasetSource();
    Boolean isLatest();
    Long getProcessInstance();
    String getTaskName();
    Integer getVersionId();
    Timestamp getDateCreated();
    Timestamp getDateModified();

}
