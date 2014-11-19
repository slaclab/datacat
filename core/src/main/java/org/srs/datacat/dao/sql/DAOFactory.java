
package org.srs.datacat.dao.sql;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;
import org.srs.datacat.vfs.DcPath;

/**
 *
 * @author bvan
 */
public class DAOFactory implements org.srs.datacat.dao.DAOFactory {
    private final DataSource dataSource;
    private final Locker locker = new Locker();
    
    public static class Locker {

        private final HashMap<DcPath, ReentrantLease> locks = new HashMap<>();
        private final HashMap<ReentrantLease, DcPath> reverseLocks = new HashMap<>();
        private final ReentrantLock mapLock = new ReentrantLock();

        class ReentrantLease extends ReentrantLock {
            protected final long initiallyAcquired;
            public ReentrantLease(){
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
         * Propose a lease
         * @param lockPath
         * @return 
         */
        public ReentrantLock prepareLease(DcPath lockPath){
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
         * Remove a lease
         * @param pathLock 
         */
        protected void cleanupLease(Lock pathLock){
            ReentrantLease lease = (ReentrantLease) pathLock;
            mapLock.lock();
            try {
                if(lease.getQueueLength() > 0){
                    return;
                }
                DcPath lockPath = reverseLocks.remove(lease);
                if(lockPath != null){
                     locks.remove(lockPath);
                }
            } finally {
                mapLock.unlock();
            }
        }

    }

          
    public DAOFactory(DataSource ds){
        this.dataSource = ds;
    }
    
    @Override
    public BaseDAO newBaseDAO() throws IOException{
        try {
            return new BaseDAO(dataSource.getConnection());
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }
    
    @Override
    public ContainerDAO newContainerDAO(DcPath lockPath) throws IOException{
        try {
            ReentrantLock lock = locker.prepareLease(lockPath);
            lock.lock();
            return new ContainerDAO(dataSource.getConnection(), lock);
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }
    
    @Override
    public ContainerDAO newContainerDAO() throws IOException{
        try {
            return new ContainerDAO(dataSource.getConnection());
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }
    
    @Override
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
    @Override
    public DatasetDAO newDatasetDAO(DcPath lockPath) throws IOException{
        try {
            ReentrantLock lock = locker.prepareLease(lockPath);
            lock.lock();
            return new DatasetDAO(dataSource.getConnection(), lock);
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }

}
