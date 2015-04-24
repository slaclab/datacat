
package org.srs.datacat.vfs;

import com.google.common.base.Optional;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttributeView;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import org.srs.datacat.model.DatasetView;

/**
 * Internal view for caching of children.
 * @author bvan
 */
public class ChildrenView implements FileAttributeView {

    private boolean hasCache = false;
    protected TreeMap<String, DcPath> children;
    private final Path path;
    private final ReentrantLock lock = new ReentrantLock();
    private final DcFileSystemProvider provider;
    
    public ChildrenView(Path path, DcFileSystemProvider provider){
        this.path = path;
        this.provider = provider;
    }
    
    @Override
    public String name(){
        return "children";
    }
    
    public void clear(){
        lock.lock();
        try {
            children.clear();
            children = null;
            hasCache = false;
        } finally {
            lock.unlock();
        }
    }
    
    public boolean hasCache(){
        return hasCache;
    }
    
    /**
     * Return true if we were able to link the file to the current list of children.
     * @param path
     * @return 
     */
    public boolean link(DcPath child){
        lock.lock();
        try {  
            if(children == null){
                return false;
            }
            children.put(child.getFileName().toString(), child);
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Return true if we were able to unlink the file to the current list of children.
     * @param filename
     * @return 
     */
    public boolean unlink(String filename){
        lock.lock();
        try {  
            if(children == null){
                return false;
            }
            if(children.remove(filename) != null){
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    public void refreshCache() throws IOException{
        lock.lock();
        try {  
            if(children == null){
                children = new TreeMap<>();
            }
            children.clear();
            doRefreshCache();
            hasCache = true;
        } finally {
            lock.unlock();
        }
    }
    
    protected void doRefreshCache() throws IOException{
        try(DirectoryStream<DcPath> stream = provider.unCachedDirectoryStream(path,
                DcFileSystemProvider.ACCEPT_ALL_FILTER, Optional.of(DatasetView.EMPTY), true)){
            for(DcPath child: stream){
                children.put(child.getFileName().toString(), child);
            }
        }
    }

    public Path getPath(){
        return path;
    }
    
    public Map<String, DcPath> getEntries() throws IOException{
        lock.lock();
        try {
            if(children == null){
                refreshCache();
            }
        } finally {
            lock.unlock();
        }
        return Collections.unmodifiableMap(children);
    }
    
    public Set<String> getChildren() throws IOException {
        return getEntries().keySet();
    }
    
    public Collection<DcPath> getChildrenPaths() throws IOException {
        return getEntries().values();
    }

}
