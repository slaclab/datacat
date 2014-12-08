
package org.srs.datacat.model;


/**
 *
 * @author bvan
 */
public interface DatasetWithViewModel extends DatasetModel, DatasetVersionModel {

    DatasetViewInfoModel getViewInfo();
}
