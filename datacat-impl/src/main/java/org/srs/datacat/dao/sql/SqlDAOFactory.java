
package org.srs.datacat.dao.sql;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;
import org.srs.datacat.dao.SearchDAO;

/**
 *
 * @author bvan
 */
public class SqlDAOFactory implements org.srs.datacat.dao.DAOFactory {
    protected final DataSource dataSource;
    private final Locker locker = new Locker();
    
    /**
     * Helper class for locking some records.
     */
    public static class Locker {

        private final HashMap<Path, ReentrantLease> locks = new HashMap<>();
        private final HashMap<ReentrantLease, Path> reverseLocks = new HashMap<>();
        private final ReentrantLock mapLock = new ReentrantLock();

        class ReentrantLease extends ReentrantLock {
            protected final long initiallyAcquired;
            ReentrantLease(){
                super();
                this.initiallyAcquired = System.currentTimeMillis();
            }

            @Override
            public void unlock(){
                super.unlock();
                Locker.this.cleanupLease(this);
            }
        }

        /**
         * Propose a lease.
         * 
         * @param lockPath
         * @return 
         */
        public ReentrantLock prepareLease(Path lockPath){
            mapLock.lock();
            try {
                ReentrantLease newLock = locks.get(lockPath);
                if(newLock != null){
                    return newLock;
                }
                newLock = new ReentrantLease();
                locks.put(lockPath, newLock);
                reverseLocks.put(newLock, lockPath);
                return newLock;
            } finally {
                mapLock.unlock();
            }
        }

        /**
         * Remove a lease.
         * 
         * @param pathLock 
         */
        protected void cleanupLease(Lock pathLock){
            ReentrantLease lease = (ReentrantLease) pathLock;
            mapLock.lock();
            try {
                if(lease.getQueueLength() > 0){
                    return;
                }
                Path lockPath = reverseLocks.remove(lease);
                if(lockPath != null){
                    locks.remove(lockPath);
                }
            } finally {
                mapLock.unlock();
            }
        }

    }
          
    public SqlDAOFactory(DataSource ds){
        this.dataSource = ds;
    }
    
    @Override
    public SqlBaseDAO newBaseDAO() throws IOException{
        try {
            return new SqlBaseDAO(dataSource.getConnection(), locker);
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }
        
    @Override
    public SqlContainerDAO newContainerDAO() throws IOException{
        try {
            return new SqlContainerDAO(dataSource.getConnection(), locker);
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }
    
    @Override
    public SqlDatasetDAO newDatasetDAO() throws IOException{
        try {
            return new SqlDatasetDAO(dataSource.getConnection(), locker);
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }
    

    @Override
    public SearchDAO newSearchDAO(Object... plugins) throws IOException{
        try {
            return new SqlSearchDAO(dataSource.getConnection(), locker, plugins);
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }


}
