
package org.srs.datacat.model.dataset;

import java.sql.Timestamp;
import java.util.Map;
import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.model.HasMetadata;

/**
 * DatasetVersionModel is the model for versions.
 * @author bvan
 */
public interface DatasetVersionModel extends DatacatRecord, HasMetadata {

    String getDatasetSource();
    Boolean isLatest();
    Integer getVersionId();
    Timestamp getDateCreated();
    Timestamp getDateModified();
    
    /**
     * Version Builder interface.
     * @param <U> Implementation class.
     */
    public interface Builder<U extends Builder> extends DatacatRecordBuilder<U> {
        
        DatasetVersionModel build();
        U create(DatasetVersionModel val);
        U metadata(Map<String, Object> val);
        
    }

}
