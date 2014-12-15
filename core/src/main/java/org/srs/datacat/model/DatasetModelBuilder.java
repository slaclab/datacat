/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacat.model;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;
import org.srs.datacat.shared.DatasetViewInfo;

/**
 *
 * @author bvan
 */
public interface DatasetModelBuilder extends Builder {
    
    int LOCATION = 1 << 4;
    int LOCATIONS = 1 << 5;
    int NONE = 0;
    int VERSION = 1 << 2;

    int BASE = 1 << 1;
    int FLAT = BASE | VERSION | LOCATION;
    int FULL = BASE | VERSION | LOCATIONS;
    
    DatasetModel build();

    DatasetModelBuilder checksum(String val);

    DatasetModelBuilder created(Timestamp val);

    DatasetModelBuilder dataType(String val);

    DatasetModelBuilder datasetSource(String val);

    DatasetModelBuilder fileFormat(String val);

    DatasetModelBuilder latest(Boolean val);

    DatasetModelBuilder location(DatasetLocationModel val);

    DatasetModelBuilder locationCreated(Timestamp val);

    DatasetModelBuilder locationModified(Timestamp val);

    DatasetModelBuilder locationPk(Long val);

    DatasetModelBuilder locationScanned(Timestamp val);

    DatasetModelBuilder locations(Collection<DatasetLocationModel> val);

    DatasetModelBuilder master(Boolean val);

    DatasetModelBuilder resource(String val);

    DatasetModelBuilder scanStatus(String val);

    DatasetModelBuilder site(String val);

    DatasetModelBuilder size(Long val);

    DatasetModelBuilder version(DatasetVersionModel val);

    DatasetModelBuilder versionCreated(Timestamp val);

    DatasetModelBuilder versionId(Integer val);

    DatasetModelBuilder versionId(DatasetView.VersionId val);

    DatasetModelBuilder versionMetadata(Map<String, Object> val);

    DatasetModelBuilder versionModified(Timestamp val);

    DatasetModelBuilder versionPk(Long val);

    DatasetModelBuilder view(DatasetViewInfo view);
    
}
