
package org.srs.datacat.vfs;

import java.io.FileNotFoundException;
import org.srs.datacat.vfs.security.DcPermissions;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;

import java.nio.file.attribute.AttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.sql.Connection;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.sql.ContainerDAO;
import org.srs.datacat.sql.Utils;
import org.srs.vfs.AbstractFsProvider;
import org.srs.vfs.AbstractPath;
import org.srs.vfs.ChildrenView;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.sql.BaseDAO;
import org.srs.datacat.sql.DatasetDAO;
import org.srs.datacat.vfs.attribute.ContainerCreationAttribute;
import org.srs.datacat.vfs.attribute.DatasetCreationAttribute;
import org.srs.datacat.vfs.attribute.DatasetOption;
import org.srs.datacat.vfs.security.DcAclFileAttributeView;
import org.srs.datacat.vfs.security.DcGroup;

/**
 *
 * @author bvan
 */
public class DcFileSystemProvider extends AbstractFsProvider<DcPath, DcFile> {
    
    private final DcFileSystem fileSystem = new DcFileSystem(this);
    
    public DcFileSystemProvider() throws IOException{
        super();
    }
    
    public DcFileSystemProvider(boolean warmCache) throws IOException{
        super();
        if(warmCache){
            refreshCache();
        }
    }
    
    private void refreshCache() throws IOException{
        doRefreshCache();
    }
    
    protected void doRefreshCache() throws IOException{
        long t0 = System.currentTimeMillis();
        getCache().clear();
        DcPath root = fileSystem.getPathProvider().getRoot();
        try(ContainerDAO dao = new ContainerDAO(Utils.getConnection())){
            DcAclFileAttributeView parentAcl;
            DcPath childPath;
            DcFile childFile;
            for(DatacatObject o: dao.getAllContainers( root, 0L )){
                DcPath parentPath = root.resolve(o.getPath());
                DcFile pFile = resolveFile(parentPath);
                parentAcl = pFile.getAttributeView(DcAclFileAttributeView.class);
                childPath = (DcPath) parentPath.resolve(o.getName()).toAbsolutePath();
                childFile = new DcFile(childPath, o);
                childFile.addAttributeViews(parentAcl);
                getCache().putFile(childFile);
            }
        } catch(SQLException ex) {
            throw new IOException("Unable to refresh cache", ex);
        }
        System.out.println("Cache loading took" + (System.currentTimeMillis() - t0));
    }

    @Override
    public String getScheme(){
        return "dc";
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir,
            final DirectoryStream.Filter<? super Path> filter) throws IOException{
        final DcPath dcPath = checkPath(dir);
        DcFile dirFile = resolveFile(dcPath);
        checkPermission( dirFile, AclEntryPermission.READ_DATA );
        if(!dirFile.isDirectory()){
            throw new NotDirectoryException(dirFile.toString());
        }
        ChildrenView<DcPath> view = dirFile.getAttributeView(ChildrenView.class);
        DirectoryStream<? extends Path> stream;
        if(view != null){
            if(!view.hasCache()){
                view.refreshCache();
            }
            stream = cachedDirectoryStream(dir, filter);
        } else {
            stream = unCachedDirectoryStream( dir, filter );
        }
        return (DirectoryStream<Path>) stream;
    }
    
    @Override
    public DirectoryStream<DcPath> unCachedDirectoryStream(Path dir,
            final DirectoryStream.Filter<? super Path> filter) throws IOException{
        final DcPath dcPath = checkPath( dir );
        DcFile dirFile = resolveFile( dcPath );
        checkPermission(dirFile, AclEntryPermission.READ_DATA );
        final DcAclFileAttributeView aclView = dirFile.
                getAttributeView( DcAclFileAttributeView.class );
        if(!dirFile.isDirectory()){
            throw new NotDirectoryException( dirFile.toString() );
        }
        Long fileKey = dirFile.fileKey();
        try {
            ContainerDAO dao = new ContainerDAO( Utils.getConnection() );
            DirectoryStream<DatacatObject> stream = dao.getChildrenStream( fileKey, dcPath.toString() );
            final Iterator<DatacatObject> iter = stream.iterator();
            
            DirectoryStreamWrapper<DcPath> wrapper = new DirectoryStreamWrapper<DcPath>( stream,
                    new DirectoryStreamWrapper.IteratorAcceptor() {
                @Override
                public boolean acceptNext() throws IOException{
                    while(iter.hasNext()){
                        DatacatObject child = iter.next();
                        DcPath maybeNext = dcPath.resolve( child.getName() );
                        DcFile file = new DcFile( maybeNext, child );
                        file.addAttributeViews( aclView );
                        getCache().putFile(file);
                        if(filter.accept( maybeNext )){
                            setNext( maybeNext );
                            return true;
                        }
                    }
                    throw new NoSuchElementException();
                }
            });
            return wrapper;
        } catch(SQLException ex) {
            throw new IOException( "Unable to list children", ex );
        }
    }

    @Override
    public DirectoryStream<DcPath> cachedDirectoryStream(Path dir,
            final DirectoryStream.Filter<? super Path> filter) throws IOException{
        final DcPath dcPath = checkPath(dir);
        final DcFile dirFile = resolveFile(dcPath);
        checkPermission( dirFile, AclEntryPermission.READ_DATA );
        final ChildrenView<DcPath> view = dirFile.getAttributeView( ChildrenView.class);
        if(!view.hasCache()){
            throw new IOException("Error attempting to use cached child entries");
        }

        final Iterator<DcPath> iter = view.getChildrenPaths().iterator();
        DirectoryStreamWrapper<DcPath> wrapper = new DirectoryStreamWrapper<>(iter, new DirectoryStreamWrapper.IteratorAcceptor() {

            @Override
            public boolean acceptNext() throws IOException{
                while(iter.hasNext()){
                    DcPath maybeNext = iter.next();
                    if(filter.accept( maybeNext )){
                        setNext( maybeNext );
                        return true;
                    }
                }
                throw new NoSuchElementException();
            }
        });
        return wrapper;
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type,
            LinkOption... options){
        DcPath dcPath = checkPath( path );
        try {
            DcFile f = resolveFile(dcPath);
            checkPermission( f, AclEntryPermission.READ_DATA );
            return f.getAttributeView( type );
        } catch(IOException ex) { 
            // Do nothing, just return null;].
        }
        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type,
            LinkOption... options) throws IOException{
        DcPath dcPath = checkPath( path );
        DcFile f = resolveFile(dcPath);
        checkPermission( f, AclEntryPermission.READ_DATA );
        if(f!=null){
            if(type == BasicFileAttributes.class || type == DcFile.class){
                return (A) f;
            }
        }
        AfsException.NO_SUCH_FILE.throwError( dcPath,"Unable to resolve file");
        return null; // Keep compiler happy
    }

    @Override
    public DcPath getPath(URI uri){
        return fileSystem.getPathProvider().getPath(uri);
    }
    
    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        DcPath dcPath = checkPath( path );
        DcFile file = resolveFile(dcPath);
        AclEntryPermission perm = DcPermissions.READ;
        if(modes.length > 0){
            if(modes[0] == AccessMode.WRITE){
                perm = DcPermissions.MODIFY;
            }
        }
        checkPermission(file, perm);
    }
        
    private DcPath checkPath(Path path){
        if(path instanceof DcPath){
            return (DcPath) path;
        }
        if(path instanceof AbstractPath){
            return fileSystem.getPathProvider().getPath(((AbstractPath) path).getUserName(), path.toString());
        }
        return fileSystem.getPathProvider().getPath(null, path.toString());
    }

    @Override
    public DcFile retrieveFileAttributes(DcPath path, DcFile parent) throws IOException {
        // LOG: Checking database
        try (BaseDAO dao = new BaseDAO(Utils.getConnection())){
            DcAclFileAttributeView aclView;
            DatacatObject child;
            if(path.equals( path.getRoot())){
                AclEntry e = AclEntry.newBuilder()
                        .setPrincipal(DcGroup.PUBLIC_GROUP)
                        .setPermissions(DcPermissions.READ)
                        .setFlags(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT)
                        .setType( AclEntryType.ALLOW )
                        .build();
                aclView = new DcAclFileAttributeView(Arrays.asList( e));
                child = dao.getDatacatObject(null, path);
            } else {
                aclView = parent.getAttributeView(DcAclFileAttributeView.class);
                DatacatObject par = parent.getObject();
                child = dao.getDatacatObject( par.getPk(), path);
            }
            DcFile f = new DcFile(path, child);
            f.addAttributeViews(aclView);
            return f;
        } catch(SQLException ex) {
            throw new IOException(ex);
        }
    }
    
    @Override
    public SeekableByteChannel newByteChannel(Path path,
            Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException{
                
        DcPath dsPath = checkPath( path );
        Dataset request = null;
        if(attrs.length != 1){
            throw new IOException("Only one attribute allowed for dataset creation");
        }
        if( !(attrs[0] instanceof DatasetCreationAttribute) ){
                throw new IOException("Creation attribute not valid for creating a dataset");
        }
        DatasetCreationAttribute dsAttr = (DatasetCreationAttribute) attrs[0];
        request = dsAttr.value();
        
        if(Files.exists(dsPath) && dsAttr.getOptions().contains( DatasetOption.CREATE_NODE)){
            AfsException.FILE_EXISTS.throwError( dsPath, "A dataset node alread exists at this location");
        }
        
        DcFile dsParent = resolveFile(dsPath.getParent());
        checkPermission(dsParent, DcPermissions.CREATE_CHILD);
        
        final DatasetDAO dao;
        try {
            dao = new DatasetDAO(Utils.getConnection());
        } catch(SQLException ex){
            throw new IOException("Unable to connect to database", ex);
        }
        
        try {
            dao.createDatasetNodeAndView( dsParent.fileKey(), dsParent.getObject().getType(), dsPath, request, dsAttr.getOptions() );
            
            return new SeekableByteChannel(){

                @Override
                public int read(ByteBuffer dst) throws IOException{
                    throw new UnsupportedOperationException("Illegal operation");
                }

                @Override
                public int write(ByteBuffer src) throws IOException{
                    throw new UnsupportedOperationException("Illegal operation");
                }

                @Override
                public long position() throws IOException{
                    throw new UnsupportedOperationException("Illegal operation");
                }

                @Override
                public SeekableByteChannel position(long newPosition) throws IOException{
                    throw new UnsupportedOperationException("Illegal operation");
                }

                @Override
                public long size() throws IOException{
                    throw new UnsupportedOperationException("Illegal operation");
                }

                @Override
                public SeekableByteChannel truncate(long size) throws IOException{
                    throw new UnsupportedOperationException("Illegal operation");
                }

                @Override
                public boolean isOpen(){
                    throw new UnsupportedOperationException("Illegal operation");
                }

                @Override
                public void close() throws IOException{
                    try {
                        dao.close();
                    } catch(SQLException ex) {
                        try { dao.rollback(); } catch(SQLException ex1) { }
                        throw new IOException("Unable to create dataset", ex);
                    } finally {
                        try { dao.close(); } catch(SQLException ex1) { }
                    }
                }
            };
            
        } catch (SQLException ex){
            try { dao.rollback(); } catch(SQLException ex1) { }
            throw new IOException(ex);
        } finally {
            try { dao.close(); } catch(SQLException ex1) { }
        }
    }
    
    @Override
    public void createDirectory(Path dir,
            FileAttribute<?>... attrs) throws IOException {
        DcPath dcDir = checkPath(dir);
        try {
            resolveFile(dcDir);
            AfsException.FILE_EXISTS.throwError(dcDir, "A group or folder already exists at this location");
        } catch (FileNotFoundException ex){
            // Do nothing.
        }
        DcFile parent = resolveFile(dcDir.getParent());
        if( !(parent.getObject().getType() == DatacatObject.Type.FOLDER)){
            AfsException.NOT_DIRECTORY.throwError( parent, "The parent file is not a folder");
        }
        //checkPermission(parent, DcPermissions.CREATE_CHILD);
        
        if(attrs.length != 1){
            throw new IOException("Only one attribute allowed for dataset creation");
        }
        
        if( !(attrs[0] instanceof ContainerCreationAttribute) ){
                throw new IOException("Creation attribute not valid for creating a dataset");
        }
        ContainerCreationAttribute dsAttr = (ContainerCreationAttribute) attrs[0];
        DatacatObject request = dsAttr.value();
        try (ContainerDAO dao = new ContainerDAO(Utils.getConnection())){
            dao.createContainer( parent.fileKey(), dcDir.getParent(), request);
            dao.commit();
        } catch(SQLException ex) {
            throw new IOException("Unable to create container", ex);
        }
    }

    @Override
    public FileSystem newFileSystem(URI uri,
            Map<String, ?> env) throws IOException{
        return getFileSystem(uri);
    }

    @Override
    public DcFileSystem getFileSystem(URI uri){
        if(uri.getScheme().equalsIgnoreCase( getScheme())){
            return fileSystem;
        }
        throw new UnsupportedOperationException(); 
    }
    
    /*
            NOT IMPLEMENTED
    */
    
    @Override
    public void delete(Path path) throws IOException{
        /*
        DcPath dcDir = checkPath(path);
        DcFile file = resolveFile( dcDir );
        checkPermission( file, DcPermissions.DELETE );
        if(file.isDirectory()){
            try(ContainerDAO dao = new ContainerDAO(Utils.getConnection())){
                
            }
            
        }*/
    }
    
    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException{
        throw new UnsupportedOperationException(); 
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException{
        throw new UnsupportedOperationException(); 
    }
    
    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException{
        throw new UnsupportedOperationException(); 
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException{
        throw new UnsupportedOperationException(); 
    }
    
    /*
            / NOT IMPLEMENTED
    */    
    public static enum DcFsException {
        
        
        NO_SUCH_VERSION,
        NO_SUCH_LOCATION,
        VERSION_EXISTS,
        LOCATION_EXISTS,
        VERSION_CONFLICT,
        NEWER_VERSION_EXISTS;
        
        public boolean throwError(String targetPath, String msg) throws FileSystemException {
            String path = targetPath;
            String reason = toString();
            switch(this){
                case NO_SUCH_VERSION:
                case NO_SUCH_LOCATION:
                    throw new NoSuchFileException(path, msg, reason);
                default:
                    throw new FileAlreadyExistsException(path, msg, reason);
            }
        }
        
    }
}
