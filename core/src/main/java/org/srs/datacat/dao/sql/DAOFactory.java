
package org.srs.datacat.dao.sql;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;

/**
 *
 * @author bvan
 */
public class DAOFactory {
    private DataSource dataSource;
    private Locker locker = new Locker();
          
    public DAOFactory(DataSource ds){
        this.dataSource = ds;
    }
    
    public BaseDAO newBaseDAO() throws IOException{
        try {
            return new BaseDAO(dataSource.getConnection());
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }
    
    public ContainerDAO newContainerDAO() throws IOException{
        try {
            return new ContainerDAO(dataSource.getConnection());
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }
    
    public DatasetDAO newDatasetDAO() throws IOException{
        try {
            return new DatasetDAO(dataSource.getConnection());
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }
    
    /**
     * Get a new DatasetDAO, acquire lock for the given lockPath
     * @param lockPath
     * @return
     * @throws IOException 
     */
    public DatasetDAO newDatasetDAO(String lockPath) throws IOException{
        try {
            ReentrantLock lock = locker.prepareLock(lockPath);
            lock.lock();
            return new DatasetDAO(dataSource.getConnection(), lock);
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }

}
