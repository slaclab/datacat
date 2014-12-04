
package org.srs.datacat.model;

import org.srs.datacat.shared.DatasetViewInfo;

/**
 *
 * @author bvan
 */
public interface DatasetWithViewModel extends DatasetModel, DatasetVersionModel {

    DatasetViewInfo getViewInfo();
}
