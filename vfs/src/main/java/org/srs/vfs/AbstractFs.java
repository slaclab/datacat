
package org.srs.vfs;

import java.io.IOException;

import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;

import java.util.Arrays;
import java.util.Set;

/**
 *
 * @author bvan
 */
public abstract class AbstractFs<P extends AbstractPath> extends FileSystem {
    
    private final AbstractFsProvider provider;
    
    public AbstractFs(AbstractFsProvider provider){
        this.provider = provider;
    }
    
    public abstract PathProvider<P> getPathProvider();

    @Override
    public AbstractFsProvider provider(){
        return provider;
    }
    
    @Override
    public void close() throws IOException{
        return;
    }

    @Override
    public boolean isOpen(){
        return true;
    }

    @Override
    public boolean isReadOnly(){
        return false;
    }

    @Override
    public String getSeparator(){
        return "/";
    }

    @Override
    public Iterable<Path> getRootDirectories(){
        return (Iterable<Path>) Arrays.asList(getPathProvider().getRoot()).iterator();
    }

    @Override
    public Iterable<FileStore> getFileStores(){
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> supportedFileAttributeViews(){
        throw new UnsupportedOperationException();
    }

    @Override
    public P getPath(String first, String... more) {
        String path;
        if (more.length == 0) {
            path = first;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(first);
            for (String segment: more) {
                if (segment.length() > 0) {
                    if (sb.length() > 0)
                        sb.append('/');
                    sb.append(segment);
                }
            }
            path = sb.toString();
        }
        return getPathProvider().getPath( null, path, more);
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern){
        return PathMatchers.getPathMatcher(syntaxAndPattern, getSeparator());
    }

    public abstract UserPrincipalLookupService getUserPrincipalLookupService();

    @Override
    public WatchService newWatchService() throws IOException{
        throw new UnsupportedOperationException();
    }

}
