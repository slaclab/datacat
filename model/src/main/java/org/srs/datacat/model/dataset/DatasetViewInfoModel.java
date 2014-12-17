
package org.srs.datacat.model.dataset;

import com.google.common.base.Optional;
import java.util.Set;
import org.srs.datacat.model.DatasetView;


/**
 *
 * @author bvan
 */
public interface DatasetViewInfoModel {

    Optional<DatasetLocationModel> canonicalLocationOpt();

    DatasetViewInfoModel fromView(DatasetView view);

    DatasetLocationModel getLocation(DatasetView view);

    DatasetLocationModel getLocation(String site);

    Set<DatasetLocationModel> getLocations();

    DatasetVersionModel getVersion();

    Optional<Set<DatasetLocationModel>> locationsOpt();

    Optional<DatasetLocationModel> singularLocationOpt();

    Optional<DatasetVersionModel> versionOpt();
    
}
