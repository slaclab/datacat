
package org.srs.vfs;

import java.io.IOException;
import java.net.URI;

import java.nio.file.AccessDeniedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.AttributeView;
import java.nio.file.attribute.UserPrincipal;

import java.nio.file.spi.FileSystemProvider;

import java.security.acl.Group;
import org.srs.vfs.AbstractFsProvider.AfsException;

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
    
    public static final DirectoryStream.Filter<AbstractPath> AcceptAllFilter
            = new DirectoryStream.Filter<AbstractPath>() {
                @Override
                public boolean accept(AbstractPath entry) throws IOException{
                    return true;
                }
            };
    
    public VfsCache<V> getCache(){
        return cache;
    }
        
    @Override
    public abstract P getPath(URI uri);
    
    public abstract V retrieveFileAttributes(P path, V parent, Class<? extends AttributeView>... attributes) throws IOException;

    public V resolveFile(P path, Class<? extends AttributeView>... attributes) throws IOException {
        // Find this file in the cache. If it's not in the cache, resolve it's parents
        // (thereby putting them in the cache), and eventually this file.
        V file = getCache().getFile(path);
        if(file == null){
            System.out.println("cache fail");
            V parent = null;
            if(!path.equals( path.getRoot())){
                parent = resolveFile( (P) path.getParent(), attributes);
            }
            file = retrieveFileAttributes(path, parent, attributes);
            getCache().putFile(file);
            return file;
        }
        return file;
    }
        
    public void checkPermission(V file, AclEntryPermission permission) throws IOException {
        AbstractPath path = file.getPath();
        String userName = path.getUserName();
        AbstractFs fs = path.getFileSystem();
        UserPrincipal user = fs.getUserPrincipalLookupService().lookupPrincipalByName(userName);

        AclFileAttributeView aclView = file.getAttributeView(AclFileAttributeView.class);
        
        for(AclEntry entry: aclView.getAcl()){
            UserPrincipal principal = entry.principal();
            if(entry.type() == AclEntryType.ALARM || entry.type() == AclEntryType.AUDIT){
                AfsException.ACCESS_DENIED.throwError(path, "Unsupported Access Control Entry found: " + entry.type());
            }
            boolean allow = entry.type() == AclEntryType.ALLOW;
            if(principal instanceof Group){
                boolean isMember = ((Group) principal).isMember( user );
                boolean hasPermission = entry.permissions().contains(permission);
                if(isMember && hasPermission){
                    if(allow){
                        return;
                    } else {
                        AfsException.ACCESS_DENIED.throwError(path, "User disallowed access to path");
                    }
                }
            }
        }
        AfsException.ACCESS_DENIED.throwError(path, "No Access Control Entries Found");
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
    
    public abstract DirectoryStream<? extends AbstractPath> cachedDirectoryStream(Path dir,
            DirectoryStream.Filter<? super Path> filter) throws IOException;
    
    public abstract DirectoryStream<? extends AbstractPath> unCachedDirectoryStream(Path dir,
            DirectoryStream.Filter<? super Path> filter) throws IOException;
    
    public static enum AfsException {
        ACCESS_DENIED(AccessDeniedException.class),
        NO_SUCH_FILE(NoSuchFileException.class),
        FILE_EXISTS(FileAlreadyExistsException.class),
        GENERIC(FileSystemException.class);
        
        private final Class<? extends FileSystemException> exceptionClass;
        
        private AfsException(Class<? extends FileSystemException> exceptionClass){
            this.exceptionClass = exceptionClass;
        }
        
        public static AfsException whichError(Class<? extends FileSystemException> exception){
            Class err = AfsException.ACCESS_DENIED.exceptionClass;
            if(ACCESS_DENIED.exceptionClass.isInstance( err )){
                return ACCESS_DENIED;
            } else if(NO_SUCH_FILE.exceptionClass.isInstance( err )){
                return NO_SUCH_FILE;
            } else if(FILE_EXISTS.exceptionClass.isInstance( err )){
                return  FILE_EXISTS;
            }
            return GENERIC;
        }
        
        public void throwError(Object target, String msg) throws FileSystemException{
            String path = target.toString();
            String reason = toString();
            switch(this){
                case ACCESS_DENIED:
                    throw new AccessDeniedException(path, msg, reason);
                case NO_SUCH_FILE:
                    throw new NoSuchFileException(path, msg, reason);
                case FILE_EXISTS:
                    throw new FileAlreadyExistsException(path, msg, reason);
                default:
                    throw new FileSystemException(path, msg, reason);
            }
        }
    }
}
