
package org.srs.datacat.vfs.attribute;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.container.BasicStat;
import org.srs.datacat.shared.container.BasicStat.StatType;
import org.srs.datacat.dao.sql.ContainerDAO;
import org.srs.datacat.dao.sql.DAOFactory;
import org.srs.datacat.vfs.DcFile;

/**
 *
 * @author bvan
 */
public class ContainerViewProvider implements DcViewProvider<StatType> {
    
    private final DcFile file;
    private final DAOFactory daoFactory;
    private final HashMap<String,BasicStat> stats = new HashMap<>(3);
    private final HashMap<DatasetView, AtomicInteger> viewCaches = new HashMap<>(3);
    private final Lock lock = new ReentrantLock();
    
    public ContainerViewProvider(DcFile file){
        this.file = file;
        this.daoFactory = file.getPath().getFileSystem().provider().getDaoFactory();
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
            viewCaches.put( view, new AtomicInteger(cacheCount));
        } finally {
            lock.unlock();
        }
    }
    
    public int decrementViewCount(DatasetView view){
        lock.lock();
        try {
            if(viewCaches.containsKey( view )){
                AtomicInteger i = viewCaches.get( view );
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
        if(viewCaches.containsKey( view )){
            return viewCaches.get( view ).get();
        }
        return 0;
    }
    
    @Override
    public DatacatObject withView(StatType statType) throws FileNotFoundException, IOException {
        if(statType == StatType.NONE){
            return file.getObject();
        }
        String wantName = statType.toString();
        String basicName = StatType.BASIC.toString();
        
        BasicStat basicStat = null;
        BasicStat retStat = null;
        lock.lock();
        try {
            if(!stats.containsKey( wantName )){
                try(ContainerDAO dao = daoFactory.newContainerDAO()) {
                    if(!stats.containsKey( basicName )){
                        stats.put( basicName, dao.getBasicStat( file.getObject() ) );
                    }
                    basicStat = stats.get( basicName );
                    if(statType == StatType.DATASET){
                        BasicStat s = dao.getDatasetStat( file.getObject(), basicStat );
                        stats.put( statType.toString(), s );
                    }
                }
            }
            retStat = stats.get(wantName);
        } finally {
            lock.unlock();
        }
        DatasetContainer.Builder b = DatasetContainer.Builder.create(file.getObject());
        b.stat( retStat );
        return b.build();
    }
    
}
