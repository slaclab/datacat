
package org.srs.vfs;

import java.io.IOException;
import java.net.URI;

import java.nio.file.AccessDeniedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;

import java.nio.file.spi.FileSystemProvider;


/**
 *
 * @author bvan
 */
public abstract class AbstractFsProvider<P extends AbstractPath, V extends VirtualFile> extends FileSystemProvider {
    
    /**
     *  should minimally implement these:
        checkAccess(path, modes)
        delete(path)
        deleteIfExissts(path);
        createDirectory(dir, attrs);
        newDirectoryStream(dir, filter);
        newByteChannel(path, options, attrs)
        readAttributes(path, type, options)
        readAttributes(path, attributes, options)
        setAttribute(path, attribute, value, options
    */
    private VfsCache<V> cache = new VfsSoftCache();
    
    public static final DirectoryStream.Filter<Path> AcceptAllFilter
            = new DirectoryStream.Filter<Path>() {
                @Override
                public boolean accept(Path entry) throws IOException{
                    return true;
                }
            };
    
    public VfsCache<V> getCache(){
        return cache;
    }
        
    @Override
    public abstract P getPath(URI uri);
    
    public abstract V retrieveFileAttributes(P path, V parent) throws NoSuchFileException, IOException;
    
    public V resolveFile(P path) throws NoSuchFileException, IOException {
        // Find this file in the cache. If it's not in the cache, resolve it's parents
        // (thereby putting them in the cache), and eventually this file.
        V file = getCache().getFile(path);
        if(file == null){
            V parent = null;
            if(!path.equals( path.getRoot())){
                parent = resolveFile( (P) path.getParent());
            }
            file = retrieveFileAttributes(path, parent);
            getCache().putFile(file);
            return file;
        }
        return file;
    }
            
    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException{
        throw new UnsupportedOperationException( "Unimplemented feature" ); 
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException{
        throw new UnsupportedOperationException( "Unimplemented feature" ); 
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException{
        throw new UnsupportedOperationException( "Unimplemented feature" ); 
    }

    @Override
    public boolean isHidden(Path path) throws IOException{
        throw new UnsupportedOperationException( "Unimplemented feature" ); 
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException{
        throw new UnsupportedOperationException( "Unimplemented feature" ); 
    }
    
    public static enum AfsException {
        ACCESS_DENIED(AccessDeniedException.class),
        NO_SUCH_FILE(NoSuchFileException.class),
        NOT_DIRECTORY(NotDirectoryException.class),
        DIRECTORY_NOT_EMPTY(DirectoryNotEmptyException.class),
        FILE_EXISTS(FileAlreadyExistsException.class),
        GENERIC(FileSystemException.class);
        
        private final Class<? extends FileSystemException> exceptionClass;
        
        private AfsException(Class<? extends FileSystemException> exceptionClass){
            this.exceptionClass = exceptionClass;
        }
        
        /*
          TODO: Untested and unused
        public static AfsException whichError(Class<? extends FileSystemException> exception){
            for(AfsException e: AfsException.values()){
                if(e.exceptionClass.isAssignableFrom(exception)){
                    return e;
                }
            }
            return GENERIC;
        }*/
        
        private static class NotContainerException extends NotDirectoryException {
            private String reason;
            private String msg;
            
            NotContainerException(String path, String msg, String reason){
                super(path);
                this.msg = msg;
                this.reason = reason;
            }
            
            @Override
            public String getReason(){
                return reason;
            }

            @Override
            public String getMessage(){
                return msg;
            }

        }
        
        private static class ContainerNotEmptyException extends DirectoryNotEmptyException {
            private String reason;
            private String msg;
            
            ContainerNotEmptyException(String path, String msg, String reason){
                super(path);
                this.msg = msg;
                this.reason = reason;
            }
            
            @Override
            public String getReason(){
                return reason;
            }

            @Override
            public String getMessage(){
                return msg;
            }
        }
        
        public void throwError(Object target, final String msg) throws FileSystemException{
            String path = target.toString();
            final String reason = toString();
            switch(this){
                case ACCESS_DENIED:
                    throw new AccessDeniedException(path, msg, reason);
                case NO_SUCH_FILE:
                    throw new NoSuchFileException(path, msg, reason);
                case NOT_DIRECTORY:
                    throw new NotContainerException(path, msg, reason);
                case DIRECTORY_NOT_EMPTY:
                    throw new ContainerNotEmptyException(path, msg, reason);
                case FILE_EXISTS:
                    throw new FileAlreadyExistsException(path, msg, reason);
                default:
                    throw new FileSystemException(path, msg, reason);
            }
        }
    }
}
