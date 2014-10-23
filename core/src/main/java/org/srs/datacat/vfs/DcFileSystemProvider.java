
package org.srs.datacat.vfs;

import java.io.FileNotFoundException;
import org.srs.datacat.vfs.security.DcPermissions;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryNotEmptyException;
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

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.DatasetView;

import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.container.BasicStat;
import org.srs.datacat.shared.dataset.VersionWithLocations;

import org.srs.datacat.dao.sql.ContainerDAO;
import org.srs.vfs.AbstractFsProvider;
import org.srs.vfs.AbstractPath;
import org.srs.vfs.ChildrenView;
import org.srs.datacat.dao.sql.BaseDAO;
import org.srs.datacat.dao.sql.DAOFactory;
import org.srs.datacat.dao.sql.DatasetDAO;
import org.srs.datacat.vfs.attribute.ContainerCreationAttribute;
import org.srs.datacat.vfs.attribute.ContainerViewProvider;

import org.srs.datacat.vfs.attribute.DatasetOption;
import org.srs.datacat.vfs.attribute.DatasetViewProvider;
import org.srs.datacat.vfs.security.DcAclFileAttributeView;
import org.srs.datacat.vfs.security.DcGroup;
import org.srs.vfs.FileType;

/**
 *
 * @author bvan
 */
public class DcFileSystemProvider extends AbstractFsProvider<DcPath, DcFile> {
    
    private static final long MAX_CHILD_CACHE = 500;
    private static final int MAX_METADATA_STRING_BYTE_SIZE = 5000;
    private static final long MAX_DATASET_CACHE_SIZE = 1<<29; // Don't blow more than about 512MB
    private static final int NO_MAX = -1;
    private static final long MAX_CACHE_TIME = 60000L; // 60 seconds TODO: Get rid of this
    
    private final DcFileSystem fileSystem;
    private final DAOFactory daoFactory;
        
    public DcFileSystemProvider(DataSource dataSource) throws IOException{
        super();
        this.daoFactory = new DAOFactory(dataSource);
        fileSystem = new DcFileSystem(this);
    }
        
    @Override
    public String getScheme(){
        return "dc";
    }
    
    public DAOFactory getDaoFactory(){
        return daoFactory;
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir,
            final DirectoryStream.Filter<? super Path> filter) throws IOException{
        return newOptimizedDirectoryStream( dir, filter, NO_MAX, DatasetView.EMPTY );
    }

    public DirectoryStream<Path> newOptimizedDirectoryStream(Path dir,
            final DirectoryStream.Filter<? super Path> filter, int max, DatasetView viewPrefetch) throws IOException{
        final DcPath dcPath = checkPath(dir);
        DcFile dirFile = resolveFile(dcPath);
        checkPermission( dirFile, AclEntryPermission.READ_DATA );
        if(!dirFile.isDirectory()){
            throw new NotDirectoryException(dirFile.toString());
        }
        ChildrenView<DcPath> view = dirFile.getAttributeView(ChildrenView.class);
        DirectoryStream<? extends Path> stream;
        boolean useCache = maybeUseCache(dirFile, viewPrefetch);
        if(view != null && useCache){
            if(!view.hasCache()){
                view.refreshCache();
            }
            stream = cachedDirectoryStream(dir, filter);
        } else {
            boolean fillCache = canFitDatasetsInCache(dirFile, max, viewPrefetch);
            stream = unCachedDirectoryStream(dir, filter, viewPrefetch, fillCache);
        }
        return (DirectoryStream<Path>) stream;
    }
    
    @Override
    public DirectoryStream<DcPath> unCachedDirectoryStream(Path dir,
            final DirectoryStream.Filter<? super Path> filter) throws IOException{
        return unCachedDirectoryStream(dir, filter, DatasetView.EMPTY, true);
    }
        
    public DirectoryStream<DcPath> directSubdirectoryStream(Path dir,
            final DirectoryStream.Filter<? super Path> filter) throws IOException{
        return unCachedDirectoryStream(dir, filter, null, false);
    }
    
    private DirectoryStream<DcPath> unCachedDirectoryStream(Path dir,
            final DirectoryStream.Filter<? super Path> filter, final DatasetView viewPrefetch, final boolean cacheDatasets) throws IOException{
        final DcPath dcPath = checkPath( dir );
        final DcFile dirFile = resolveFile( dcPath );
        checkPermission(dirFile, AclEntryPermission.READ_DATA );
        final DcAclFileAttributeView aclView = dirFile.
                getAttributeView( DcAclFileAttributeView.class );
        if(!dirFile.isDirectory()){
            throw new NotDirectoryException( dirFile.toString() );
        }
        Long fileKey = dirFile.fileKey();
        // !IMPORTANT!: This object is closed when the stream is closed
        final ContainerDAO dao = daoFactory.newContainerDAO(); 
        DirectoryStream<DatacatObject> stream;
        if(viewPrefetch != null){
            stream = dao.getChildrenStream(fileKey, dcPath.toString(), viewPrefetch);
        } else {
            stream = dao.getSubdirectoryStream(fileKey, dcPath.toString());
        }

        final Iterator<DatacatObject> iter = stream.iterator();
        final AtomicInteger dsCount = new AtomicInteger();
        DirectoryStreamWrapper.IteratorAcceptor acceptor = 
                new DirectoryStreamWrapper.IteratorAcceptor() {

            @Override
            public boolean acceptNext() throws IOException{
                while(iter.hasNext()){
                    DatacatObject child = iter.next();
                    DcPath maybeNext = dcPath.resolve( child.getName() );
                    DcFile file = new DcFile( maybeNext, child, aclView);

                    if(file.isDirectory()){
                        getCache().putFileIfAbsent(file);
                    }
                    if(!file.isDirectory() && cacheDatasets){
                         getCache().putFileIfAbsent(file);
                         dsCount.incrementAndGet();
                    }
                    if(filter.accept( maybeNext )){
                        setNext( maybeNext );
                        return true;
                    }
                }
                throw new NoSuchElementException();
            }
        };

        DirectoryStreamWrapper<DcPath> wrapper = 
                new DirectoryStreamWrapper<DcPath>(stream, acceptor){

            @Override
            public void close() throws IOException{
                if(dsCount.get() > 0){
                    dirFile.getAttributeView( ContainerViewProvider.class)
                            .setViewStats( viewPrefetch, dsCount.get());
                }
                super.close();
                dao.close();  // Make sure to close dao (and underlying connection)
            }

        };
        return wrapper;
    }

    @Override
    public DirectoryStream<DcPath> cachedDirectoryStream(Path dir,
            final DirectoryStream.Filter<? super Path> filter) throws IOException{
        final DcPath dcPath = checkPath(dir);
        final DcFile dirFile = resolveFile(dcPath);
        checkPermission( dirFile, AclEntryPermission.READ_DATA );
        final ChildrenView<DcPath> view = dirFile.getAttributeView(ChildrenView.class);
        if(!view.hasCache()){
            throw new IOException("Error attempting to use cached child entries");
        }

        final Iterator<DcPath> iter = view.getChildrenPaths().iterator();
        DirectoryStreamWrapper<DcPath> wrapper = new DirectoryStreamWrapper<>(iter, 
                new DirectoryStreamWrapper.IteratorAcceptor() {

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
    
    /**
     * This checks to see if a given view is cached and, if it is, if there is enough items
     * in the cache to be worthwhile to use the cache.
     */
    private boolean maybeUseCache(DcFile dirFile, DatasetView viewPrefetch) throws IOException{
        //TODO: Fix caching of large results
        if(viewPrefetch == DatasetView.EMPTY){
            return true;
        }
        ContainerViewProvider cstat = dirFile.getAttributeView( ContainerViewProvider.class);
        DatasetContainer container = (DatasetContainer) cstat.withView( BasicStat.StatType.BASIC );
        int count = container.getStat().getChildCount();
        int cacheCount = cstat.getViewStats( viewPrefetch );
        if((count - cacheCount) < MAX_CHILD_CACHE){
            return true;
        }
        return false;
    }
    
    /**
     * This could tries to decide if we should even try to fit all the datasets in
     * cache or not.
     */
    private boolean canFitDatasetsInCache(DcFile dirFile, int max, DatasetView viewPrefetch) throws IOException{
        //TODO: Improve logic
        ContainerViewProvider cstat = dirFile.getAttributeView( ContainerViewProvider.class);
        DatasetContainer container = (DatasetContainer) cstat.withView( BasicStat.StatType.BASIC );
        int count = max;
        if(count <= 0){
            count = container.getStat().getChildCount();
        }
        /* 
            The average upper bound of metadata size (in bytes) for a given dataset is about 
            5kB. We want to make sure that we don't cache more than about 
        */
        long estimate = count*MAX_METADATA_STRING_BYTE_SIZE;
        if(estimate < MAX_DATASET_CACHE_SIZE){
            return true;
        }
        // TODO: Check actual return size using the Data Access layer
        return false;
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
    
    /**
     * Gets a file
     * @param path
     * @return
     * @throws IOException 
     */
    public DcFile getFile(Path path) throws IOException{
        DcPath dcPath = checkPath( path );
        /*
        TODO: When we have control over file creation, remove this and replace it with
              some sort of distributed consensus stuff potentially.
        */
        DcFile f = resolveFile(dcPath);
        if((f.lastModifiedTime().toMillis() - System.currentTimeMillis()) > MAX_CACHE_TIME){
            getCache().removeFile(dcPath);
            f = resolveFile(dcPath);
        }
        checkPermission( f, AclEntryPermission.READ_DATA );
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
        try (BaseDAO dao = daoFactory.newBaseDAO()){
            DcAclFileAttributeView aclView;
            DatacatObject child;
            if(path.equals(path.getRoot())){
                AclEntry e = AclEntry.newBuilder()
                        .setPrincipal(DcGroup.PUBLIC_GROUP)
                        .setPermissions(DcPermissions.READ)
                        .setFlags(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT)
                        .setType( AclEntryType.ALLOW )
                        .build();
                aclView = new DcAclFileAttributeView(Arrays.asList( e));
                child = dao.getObjectInParent(null, path.toString());
            } else {
                aclView = parent.getAttributeView(DcAclFileAttributeView.class);
                DatacatObject par = parent.getObject();
                child = dao.getObjectInParent( par.getPk(), path.toString());
            }
            DcFile f = new DcFile(path, child, aclView);
            return f;
        }
    }
    
    /**
     * This will fail if there already exists a Dataset record.
     * @param path Path of this new dataset
     * @param ds
     * @param options
     * @throws IOException 
     */
    public void createDataset(Path path, Dataset ds, Set<DatasetOption> options) throws IOException{
        DcPath dsPath = checkPath( path );
        
        if(!options.contains(DatasetOption.SKIP_NODE_CHECK)     // Fail fast
                && options.contains(DatasetOption.CREATE_NODE) // Fail fast
                && Files.exists(dsPath)){                      // -> Can be Expensive
            AfsException.FILE_EXISTS.throwError( dsPath, "A dataset node already exists at this location");
        }
        
        DcFile dsParent = resolveFile(dsPath.getParent());
        //checkPermission(dsParent, DcPermissions.CREATE_CHILD);
        try (DatasetDAO dao = daoFactory.newDatasetDAO()){
            dao.createDatasetNodeAndView( dsParent.fileKey(), dsParent.getObject().getType(), dsPath.toString(), ds, options );
            dao.commit();
            dao.close();
        }
        dsParent.childAdded(dsPath, FileType.FILE);
    }
    
    public VersionWithLocations getVersionWithLocations(DcFile file, DatasetView view) throws IOException {
        try(DatasetDAO dsdao = daoFactory.newDatasetDAO()) {
            VersionWithLocations ret = dsdao.getVersionWithLocations(file.fileKey(), view);
            if(ret == null){
                throw new FileNotFoundException(String.format("Invalid View. Version %d not found", view.getVersionId()));
            }
            return ret;
        }
    }
    
    public void createDatasetView(Path path, DatasetVersion verRequest, DatasetLocation locRequest, Set<DatasetOption> options) throws IOException{
        DcPath dsPath = checkPath( path );
        DcFile dsFile = resolveFile(dsPath);
        if(!dsFile.isRegularFile() ){
            AfsException.NO_SUCH_FILE.throwError(path, "Path is not a dataset");
        }
        
        Dataset ds = (Dataset) dsFile.getObject();

        if(options.contains(DatasetOption.CREATE_NODE)){
            throw new UnsupportedOperationException("This method cannot create a node");
        }
        
        //checkPermission(dsFile, DcPermissions.MODIFY);
        Dataset.Builder builder = new Dataset.Builder(ds);
        try (DatasetDAO dao = daoFactory.newDatasetDAO()){
            dao.createDatasetView(ds, builder, verRequest, locRequest, options);
            dao.commit();
            dao.close();    
        }
        dsFile.getAttributeView(DatasetViewProvider.class).clear();
        resolveFile(dsPath.getParent()).childModified(dsPath);
    }
        
    @Override
    public void createDirectory(Path dir,
            FileAttribute<?>... attrs) throws IOException {
        DcPath targetDir = checkPath(dir);
        try {
            resolveFile(targetDir);
            AfsException.FILE_EXISTS.throwError(targetDir, "A group or folder already exists at this location");
        } catch (FileNotFoundException ex){
            // Do nothing.
        }
        DcFile parent = resolveFile(targetDir.getParent());
        if(parent.getType() != FileType.DIRECTORY){ // Use the constant instead of instanceof
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
        try (ContainerDAO dao = daoFactory.newContainerDAO()){
            DatacatObject ret = dao.createContainer( parent.fileKey(), targetDir.toString(), request);
            dao.commit();
            parent.childAdded(targetDir, FileType.DIRECTORY);
            DcFile f = new DcFile(targetDir, ret, parent.getAttributeView( DcAclFileAttributeView.class ));
            getCache().putFile(f);
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
    
    @Override
    public void delete(Path path) throws IOException{
        DcPath dcPath = checkPath(path);
        DcFile file = resolveFile( dcPath );
        DcFile parentFile = resolveFile(dcPath.getParent());
        // TODO: Delete permissions
        // checkPermission( file, DcPermissions.DELETE );
        if(file.isDirectory()){
            doDeleteDirectory(dcPath.toString(), file);
        } else if (file.isRegularFile()){
            doDeleteDataset(dcPath.toString(), file );
        }
        getCache().removeFile(dcPath);
        parentFile.childRemoved(dcPath);
    }
    
    protected void doDeleteDirectory(String path, DcFile file) throws DirectoryNotEmptyException, IOException{
        try(ContainerDAO dao = daoFactory.newContainerDAO()) {
            // Verify directory is empty
            try(DirectoryStream ds = dao.getChildrenStream( file.fileKey(), path, DatasetView.EMPTY )) {
                if(ds.iterator().hasNext()){
                    AfsException.DIRECTORY_NOT_EMPTY.throwError( path, "Container not empty" );
                }
            }
            dao.deleteContainer( file.getObject() );
            dao.commit();
        }
    }
    
    protected void doDeleteDataset(String path, DcFile file) throws IOException {
        try(DatasetDAO dao = daoFactory.newDatasetDAO()){
            dao.deleteDataset(file.getObject());
        }
    }
    
    /*
            NOT IMPLEMENTED
    */
    
    @Override
    public SeekableByteChannel newByteChannel(Path path,
            Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException{
        throw new UnsupportedOperationException("Use createDataset method to create a dataset");
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
