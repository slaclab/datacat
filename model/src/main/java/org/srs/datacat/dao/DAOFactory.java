
package org.srs.datacat.dao;

import java.io.IOException;
import java.nio.file.Path;

/**
 *
 * @author bvan
 */
public interface DAOFactory {

    BaseDAO newBaseDAO() throws IOException;

    ContainerDAO newContainerDAO(Path lockPath) throws IOException;

    ContainerDAO newContainerDAO() throws IOException;

    DatasetDAO newDatasetDAO() throws IOException;

    /**
     * Get a new DatasetDAO, acquire lock for the given lockPath.
     * 
     * @param lockPath Request a lock for the given path.
     * @return DAO object.
     */
    DatasetDAO newDatasetDAO(Path lockPath) throws IOException;
    
    SearchDAO newSearchDAO(Object... plugins) throws IOException;
    
}
