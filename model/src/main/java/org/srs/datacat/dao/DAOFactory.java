
package org.srs.datacat.dao;

import java.io.IOException;

/**
 * DAOFactor returns a new DAO object for the proper sub-DAO.
 * @author bvan
 */
public interface DAOFactory {

    BaseDAO newBaseDAO() throws IOException;

    ContainerDAO newContainerDAO() throws IOException;

    DatasetDAO newDatasetDAO() throws IOException;
    
    SearchDAO newSearchDAO(Object... plugins) throws IOException;
    
}
