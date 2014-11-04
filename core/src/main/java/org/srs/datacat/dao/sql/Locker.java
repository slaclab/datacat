
package org.srs.datacat.dao.sql;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author bvan
 */
public class Locker {
    
    private final HashMap<String, ReentrantLease> locks = new HashMap<>();
    private final HashMap<ReentrantLease, String> reverseLocks = new HashMap<>();
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
            Locker.this.removeLock(this);
        }
    }
    
    public ReentrantLock prepareLock(String lockPath){
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
    
    protected void removeLock(Lock pathLock){
        ReentrantLease lease = (ReentrantLease) pathLock;
        mapLock.lock();
        try {
            if(lease.getQueueLength() > 0){
                return;
            }
            String lockPath = reverseLocks.remove(lease);
            if(lockPath != null){
                 locks.remove(lockPath);
            }
        } finally {
            mapLock.unlock();
        }
    }
    
}
