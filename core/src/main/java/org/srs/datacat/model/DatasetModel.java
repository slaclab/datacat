
package org.srs.datacat.model;

import java.sql.Timestamp;

/**
 *
 * @author bvan
 */
public interface DatasetModel extends DatacatNode {

    String getDataType();
    String getFileFormat();
    Timestamp getDateCreated();
    Timestamp getDateModified();
    
}
