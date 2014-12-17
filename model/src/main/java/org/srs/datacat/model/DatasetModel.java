
package org.srs.datacat.model;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;
import org.srs.datacat.model.dataset.DatasetLocationModel;
import org.srs.datacat.model.dataset.DatasetVersionModel;
import org.srs.datacat.model.dataset.DatasetViewInfoModel;

/**
 *
 * @author bvan
 */
public interface DatasetModel extends DatacatNode {

    String getDataType();
    String getFileFormat();
    Timestamp getDateCreated();
    Timestamp getDateModified();

    /**
     * Standard interface for building a DatasetModel object.
     * @author bvan
     */
    public interface Builder extends org.srs.datacat.model.Builder {
        int LOCATION = 1 << 4;
        int LOCATIONS = 1 << 5;
        int NONE = 0;
        int VERSION = 1 << 2;
        int BASE = 1 << 1;
        int FLAT = BASE | VERSION | LOCATION;
        int FULL = BASE | VERSION | LOCATIONS;

        @Override
        DatasetModel build();

        Builder create(DatacatNode val);

        Builder checksum(String val);

        Builder created(Timestamp val);

        Builder dataType(String val);

        Builder datasetSource(String val);

        Builder fileFormat(String val);

        Builder latest(Boolean val);

        Builder location(DatasetLocationModel val);

        Builder locationCreated(Timestamp val);

        Builder locationModified(Timestamp val);

        Builder locationPk(Long val);

        Builder locationScanned(Timestamp val);

        Builder locations(Collection<DatasetLocationModel> val);

        Builder master(Boolean val);

        Builder resource(String val);

        Builder scanStatus(String val);

        Builder site(String val);

        Builder size(Long val);

        Builder version(DatasetVersionModel val);

        Builder versionCreated(Timestamp val);

        Builder versionId(Integer val);

        Builder versionId(DatasetView.VersionId val);

        Builder versionMetadata(Map<String, Object> val);

        Builder versionModified(Timestamp val);

        Builder versionPk(Long val);

        Builder view(DatasetViewInfoModel view);
    }
    
}
