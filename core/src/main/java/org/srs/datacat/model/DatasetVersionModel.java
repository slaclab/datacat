
package org.srs.datacat.model;

import java.sql.Timestamp;

/**
 *
 * @author bvan
 */
public interface DatasetVersionModel extends DatacatRecord, HasMetadata {

    String getDatasetSource();
    Boolean isLatest();
    Integer getVersionId();
    Timestamp getDateCreated();
    Timestamp getDateModified();

}
