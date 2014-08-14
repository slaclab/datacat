
package org.srs.vfs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.attribute.FileAttributeView;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author bvan
 * @param <P> Type of path for this object and it's children
 */
public class ChildrenView<P extends AbstractPath> implements FileAttributeView {

    private boolean hasCache = false;
    private TreeMap<String, P> children;
    private final P path;
    private final ReentrantLock lock = new ReentrantLock();
    
    public ChildrenView(P path){
        this.path = path;
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
     * Return true if we were able to link the file to the current list of children
     * @param filename
     * @return 
     */
    public boolean link(P path){
        lock.lock();
        try {  
            if(children == null){
                return false;
            }
            children.put( path.getFileName().toString(), path);
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Return true if we were able to unlink the file to the current list of children
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
            AbstractFsProvider provider = path.getFileSystem().provider();
            try(DirectoryStream<P> stream = 
                    provider.unCachedDirectoryStream(path, provider.AcceptAllFilter)){
                for(P p: stream){
                    children.put( p.getFileName().toString(), p);
                }
                hasCache = true;
            }
        } finally {
            lock.unlock();
        }
    }
    
    public Map<String, P> getEntries() throws IOException{
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
    
    public Collection<P> getChildrenPaths() throws IOException {
        return getEntries().values();
    }

}
