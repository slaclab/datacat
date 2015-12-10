
package org.srs.datacat.model.dataset;

import org.srs.datacat.model.DatasetModel;


/**
 *
 * @author bvan
 */
public interface DatasetWithViewModel extends DatasetModel, DatasetVersionModel {

    DatasetViewInfoModel getViewInfo();
}
