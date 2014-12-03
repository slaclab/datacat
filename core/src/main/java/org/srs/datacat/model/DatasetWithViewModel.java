
package org.srs.datacat.model;

import org.srs.datacat.shared.dataset.DatasetViewInfo;

/**
 *
 * @author bvan
 */
public interface DatasetWithViewModel extends DatasetModel, DatasetVersionModel {

    DatasetViewInfo getViewInfo();
}
