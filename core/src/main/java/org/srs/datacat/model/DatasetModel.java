
package org.srs.datacat.model;

import java.sql.Timestamp;

/**
 *
 * @author bvan
 */
public interface DatasetModel {

    String getDatasetDataType();
    String getDatasetFileFormat();
    Timestamp getDateCreated();
    Timestamp getDateModified();
    
}
