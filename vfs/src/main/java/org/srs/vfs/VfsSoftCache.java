
package org.srs.vfs;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Stripped down, java.nio.file.Path compatible, 
 * file system re-implementation of  org.apache.commons.vfs2.cache.SoftRefFilesCache
 * @author bvan
 */
public class VfsSoftCache<V extends VirtualFile> implements VfsCache<V> {

    private static final int TIMEOUT = 1000;

    private final Map<Path, Reference<V>> virtualFileSystemCache = new ConcurrentHashMap<>();
    private final Map<Reference<V>, Path> refReverseMap = new HashMap<>(100);
    private final ReferenceQueue<V> refQueue = new ReferenceQueue<>();
    private final AtomicReference<SoftRefReleaseThread> softRefReleaseThread = new AtomicReference<>();

    private final Lock lock = new ReentrantLock();

    public VfsSoftCache(){ }

    {
        Thread thread;
        SoftRefReleaseThread newThread;
        do {
            newThread = null;
            thread = softRefReleaseThread.get();
            if (thread != null) {
                break;
            }
            newThread = new SoftRefReleaseThread();
        } while (softRefReleaseThread.compareAndSet(null, newThread));
        if (newThread != null) {
            newThread.start();
        }
    }

    private void endThread() {
        final SoftRefReleaseThread thread = softRefReleaseThread.getAndSet(null);
        if (thread != null) {
            thread.requestEnd = true;
            thread.interrupt();
        }
    }

    @Override
    public void putFile(final V fileObject) {
        final Reference<V> ref = createReference(fileObject, refQueue);
        final Path key = fileObject.getPath();

        lock.lock();
        try {
            final Reference<V> old = virtualFileSystemCache.put(key, ref);
            if (old != null) {
                refReverseMap.remove(old);
            }
            refReverseMap.put(ref, key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean putFileIfAbsent(final V fileObject) {

        final Reference<V> ref = createReference(fileObject, refQueue);
        final Path key = fileObject.getPath();

        lock.lock();
        try {
            if (virtualFileSystemCache.containsKey(key) && virtualFileSystemCache.get(key).get() != null){
                return false;
            }
            final Reference<V> old = virtualFileSystemCache.put(key, ref);
            if (old != null) {
                refReverseMap.remove(old);
            }
            refReverseMap.put(ref, key);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public V getFile(final Path fileName) {
        lock.lock();
        try {
            final Reference<V> ref = virtualFileSystemCache.get(fileName);
            if (ref == null){
                return null;
            }

            final V fo = ref.get();
            if (fo == null) {
                removeFile(fileName);
            }
            return fo;
        } finally {
            lock.unlock();
        }
    }
    
    protected Reference<V> createReference(final V file, 
            final ReferenceQueue<V> refqueue) {
        return new SoftReference<>(file, refqueue);
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            final Iterator<Path> iterKeys = refReverseMap.values().iterator();
            while (iterKeys.hasNext()) {
                final Path key = iterKeys.next();
                iterKeys.remove();
                virtualFileSystemCache.remove(key);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        endThread();
        lock.lock();
        try {
            virtualFileSystemCache.clear();
            refReverseMap.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void touchFile(final V fileObject){
    }

    @Override
    public boolean removeFile(final Path key){
        
        lock.lock();
        try {
            final Object ref = virtualFileSystemCache.remove(key);
            if (ref != null) {
                refReverseMap.remove(ref);
            }
            return virtualFileSystemCache.size() < 1;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * This thread will listen on the ReferenceQueue and remove the entry in the
     * filescache as soon as the vm removes the reference
     */
    private final class SoftRefReleaseThread extends Thread {
        
        private volatile boolean requestEnd; // used for inter-thread communication

        private SoftRefReleaseThread() {
            setName(SoftRefReleaseThread.class.getName());
            setDaemon(true);
        }

        @Override
        public void run() {
            
            loop: while (!requestEnd && !Thread.currentThread().isInterrupted())
            {
                try {
                    final Reference<?> ref = refQueue.remove(TIMEOUT);
                    if (ref == null) {
                        continue;
                    }

                    lock.lock();
                    try {
                        final Path key = refReverseMap.get(ref);

                        if (key != null && removeFile(key)){
                            close();
                        }
                    } finally {
                        lock.unlock();
                    }
                } catch (final InterruptedException e) {
                    if (!requestEnd){
                        // "vfs.impl/SoftRefReleaseThread-interrupt.info"
                    }
                    break loop;
                }
            }
        }
    }

}