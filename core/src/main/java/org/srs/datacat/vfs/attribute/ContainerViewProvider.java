package org.srs.datacat.vfs.attribute;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.dao.ContainerDAO;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.container.ContainerStat;
import org.srs.datacat.model.container.DatasetContainerBuilder;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcFileSystemProvider;

/**
 * A view for Containers. Helps with the stats.
 *
 * @author bvan
 */
public class ContainerViewProvider implements DcViewProvider<Class<? extends ContainerStat>> {

    private final DcFile file;
    private final DcFileSystemProvider provider;
    private final HashMap<Class<? extends ContainerStat>, ContainerStat> stats = new HashMap<>(3);
    private final HashMap<DatasetView, AtomicInteger> viewCaches = new HashMap<>(3);
    private final Lock lock = new ReentrantLock();

    public ContainerViewProvider(DcFile file){
        this.file = file;
        this.provider = file.getPath().getFileSystem().provider();
    }

    @Override
    public String name(){
        return "cstat";
    }

    public void clearStats(){
        lock.lock();
        try {
            stats.clear();
            viewCaches.clear();
        } finally {
            lock.unlock();
        }
    }

    public void setViewStats(DatasetView view, int cacheCount){
        lock.lock();
        try {
            viewCaches.put(view, new AtomicInteger(cacheCount));
        } finally {
            lock.unlock();
        }
    }

    public int decrementViewCount(DatasetView view){
        lock.lock();
        try {
            if(viewCaches.containsKey(view)){
                AtomicInteger i = viewCaches.get(view);
                int ret = i.decrementAndGet();
                if(ret <= 0){
                    viewCaches.remove(view);
                }
                return ret;
            }
        } finally {
            lock.unlock();
        }
        return 0;
    }

    public int getViewStats(DatasetView view){
        if(viewCaches.containsKey(view)){
            return viewCaches.get(view).get();
        }
        return 0;
    }

    @Override
    public DatasetContainer withView(Class<? extends ContainerStat> statType) throws NoSuchFileException, IOException{
        if(statType == null){
            return (DatasetContainer) file.getObject();
        }
        
        ContainerStat retStat = null;
        lock.lock();
        try {
            if(!stats.containsKey(statType)){
                try(ContainerDAO dao = provider.getDaoFactory().newContainerDAO()) {
                    stats.put(statType, dao.getStat(file.getObject(), statType));
                }
            }
            retStat = stats.get(statType);
        } finally {
            lock.unlock();
        }
        DatasetContainerBuilder b = provider.getModelProvider()
                .getContainerBuilder().create(file.getObject());
        b.stat(retStat);
        return b.build();
    }

}
