
package org.srs.datacat.dao;

import java.io.IOException;
import org.srs.datacat.dao.sql.BaseDAO;
import org.srs.datacat.dao.sql.ContainerDAO;
import org.srs.datacat.dao.sql.DatasetDAO;
import org.srs.datacat.vfs.DcPath;

/**
 *
 * @author bvan
 */
public interface DAOFactory {

    BaseDAO newBaseDAO() throws IOException;

    ContainerDAO newContainerDAO(DcPath lockPath) throws IOException;

    ContainerDAO newContainerDAO() throws IOException;

    DatasetDAO newDatasetDAO() throws IOException;

    /**
     * Get a new DatasetDAO, acquire lock for the given lockPath
     * @param lockPath
     * @return
     * @throws IOException
     */
    DatasetDAO newDatasetDAO(DcPath lockPath) throws IOException;
    
}
