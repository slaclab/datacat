
package org.srs.datacat.vfs;

import com.google.common.base.Optional;
import org.srs.datacat.security.DcPermissions;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

import org.srs.datacat.dao.sql.ContainerDAO;
import org.srs.vfs.AbstractFsProvider;
import org.srs.vfs.AbstractPath;
import org.srs.vfs.ChildrenView;
import org.srs.datacat.dao.BaseDAO;
import org.srs.datacat.dao.DatasetDAO;
import org.srs.datacat.dao.DAOFactory;
import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.shared.dataset.DatasetViewInfo;
import org.srs.datacat.shared.dataset.DatasetWithView;
import org.srs.datacat.vfs.attribute.ContainerCreationAttribute;
import org.srs.datacat.vfs.attribute.ContainerViewProvider;

import org.srs.datacat.vfs.attribute.DatasetOption;
import org.srs.datacat.vfs.attribute.DcAclFileAttributeView;
import org.srs.datacat.security.DcGroup;
import org.srs.datacat.security.DcUserLookupService;
import org.srs.datacat.security.DcUser;
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
        
    public DcFileSystemProvider(DataSource dataSource, DcUserLookupService userLookupService) throws IOException{
        super();
        this.daoFactory = new org.srs.datacat.dao.sql.DAOFactory(dataSource);
        fileSystem = new DcFileSystem(this, userLookupService);
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
        checkPermission(dcPath.getUserName(), dirFile, DcPermissions.READ);
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
        checkPermission(dcPath.getUserName(), dirFile, DcPermissions.READ);
        final DcAclFileAttributeView aclView = dirFile.
                getAttributeView( DcAclFileAttributeView.class );
        if(!dirFile.isDirectory()){
            throw new NotDirectoryException( dirFile.toString() );
        }

        // !IMPORTANT!: This object is closed when the stream is closed
        final ContainerDAO dao = daoFactory.newContainerDAO(); 
        DirectoryStream<DatacatObject> stream;
        if(viewPrefetch != null){
            stream = dao.getChildrenStream(dirFile.asRecord(), viewPrefetch);
        } else {
            stream = dao.getSubdirectoryStream(dirFile.asRecord());
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
                    DcFile file = DcFileSystemProvider.this.buildChild(dirFile, maybeNext, child);

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
        checkPermission(dcPath.getUserName(), dirFile, DcPermissions.READ);
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
            checkPermission(dcPath.getUserName(), f, DcPermissions.READ);
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
        checkPermission(dcPath.getUserName(), f, DcPermissions.READ);
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
        if(f!= null){
            checkPermission(dcPath.getUserName(), f, DcPermissions.READ);
            return f;
        }
        AfsException.NO_SUCH_FILE.throwError( dcPath,"Unable to resolve file");
        return null; // Keep compiler happy
    }
    
    /**
     * Add metadata to a DatacatRecord at a given path
     * @param path
     * @param record
     * @param metadata
     * @return
     * @throws IOException 
     */
    public DcFile addMetadata(Path path, DatacatRecord record, Map metadata) throws IOException{
        DcPath dcPath = checkPath(path);
        DcFile f = getFile(dcPath);
        checkPermission(dcPath.getUserName(), f, DcPermissions.WRITE);
        try (BaseDAO dao = daoFactory.newBaseDAO()){
            dao.addMetadata(record, metadata);
            dao.commit();
        }
        getCache().removeFile(dcPath);
        return getFile(dcPath);
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
                perm = DcPermissions.WRITE;
            }
        }
        checkPermission(dcPath.getUserName(), file, perm);
    }
    
    public boolean exists(Path path) {
        try {
            readAttributes(path, BasicFileAttributes.class);
            // file exists
            return true;
        } catch (IOException x) {
            // does not exist or unable to determine if file exists
            return false;
        }
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
            DatacatRecord parentRecord = parent != null ? parent.asRecord() : null;
            return buildChild(parent, path, dao.getObjectInParent(parentRecord, path.getFileName().toString()));
        }
    }
    
    private DcFile buildChild(DcFile parent, DcPath childPath, DatacatObject child) throws IOException{
        DcAclFileAttributeView aclView;
        if(parent == null){
            aclView = new DcAclFileAttributeView(child.getAclAttributes());
        } else {
            if(child.getAclAttributes() != null){
                /* 
                  TODO: Come up with better strategy for global ADMIN privileges 
                    Always inherit parent ADMIN permissions for now.
                */
                List<AclEntry> acl = new ArrayList<>(child.getAclAttributes().getAcl());
                for(AclEntry e: parent.getAttributeView(DcAclFileAttributeView.class).getAcl()){
                    if(e.permissions().contains(DcPermissions.ADMIN)){
                        acl.add(e);
                    }
                }
                aclView = new DcAclFileAttributeView(child.getAclAttributes().getOwner(), acl);
            } else {
                // Inherit parent's attributes
                aclView = parent.getAttributeView(DcAclFileAttributeView.class);
            }
        }
        return new DcFile(childPath, child, aclView);
    }
    
    /**
     * This will fail if there already exists a Dataset record.
     * @param path Path of this new dataset
     * @param dsReq
     * @param options
     * @return Dataset, FlatDataset, or FullDataset
     * @throws IOException 
     */
    public Dataset createDataset(Path path, Dataset dsReq, Set<DatasetOption> options) throws IOException{
        if(dsReq == null){
            throw new IOException( "Not enough information to create create a Dataset node or view" );
        }
        DcPath dsPath = checkPath(path);

        try (DatasetDAO dao = daoFactory.newDatasetDAO(dsPath)){
            DcFile dsParent = resolveFile(dsPath.getParent());
            
            String pathString = dsPath.toString();
            Dataset ds = null;
            Set<DatasetOption> dsOptions = new HashSet<>(options); // make a copy
            boolean createNode = dsOptions.remove(DatasetOption.CREATE_NODE);
            
            if(createNode){
                checkPermission(dsPath.getUserName(), dsParent, DcPermissions.INSERT);
                // Fail fast
                if(!options.contains(DatasetOption.SKIP_NODE_CHECK) && exists(dsPath)){ 
                    DcFsException.DATASET_EXISTS.throwError(dsPath.toString(), "A dataset node already exists at this location");
                }
                ds = dao.createNode(dsParent.asRecord(), dsPath.getFileName().toString(), dsReq);
                dsOptions.add(DatasetOption.SKIP_VERSION_CHECK); // If we added a node, skip version check
            }
            
            if(ds == null){   // No DatasetOption.CREATE_NODE, find current dataset instead
                DatacatObject o = dao.getObjectInParent(dsParent.asRecord(), dsPath.getFileName().toString());
                if(!(o instanceof Dataset)){
                    AfsException.NO_SUCH_FILE.throwError(pathString, "Target is not a dataset");
                }
                ds = (Dataset) o;
            }
            
            Dataset.Builder builder = new Dataset.Builder(ds);
            // One of these conditions must be present to continue on and create a view
            HashSet<DatasetOption> viewWork = new HashSet<>( Arrays.asList( 
                                                DatasetOption.CREATE_VERSION, 
                                                DatasetOption.MERGE_VERSION, 
                                                DatasetOption.CREATE_LOCATIONS));
            viewWork.retainAll(dsOptions);
            if(!viewWork.isEmpty()){
                checkPermission(dsPath.getUserName(), dsParent, DcPermissions.WRITE);
                // We had a flag that denoting we should create a view, so we continue on
                if(dsReq instanceof DatasetWithView){
                    DatasetViewInfo info = ((DatasetWithView) dsReq).getViewInfo();
                    DatasetViewInfo retView = createDatasetViewInternal(dao, ds, info, options);
                    builder.version(retView.getVersion());
                    if(retView.locationsOpt().isPresent()){
                        if(retView.singularLocationOpt().isPresent()){
                            builder.location(retView.singularLocationOpt().get());
                        } else if (!retView.getLocations().isEmpty()){
                            builder.locations(retView.getLocations());
                        }
                    }   
                } else {
                    throw new IOException("Unable to create dataset, not enough information");
                }
            }
            dao.commit();
            dao.close();
            dsParent.childAdded(dsPath, FileType.FILE);
            return builder.build();
        }
    }
    
    public DatasetViewInfo getDatasetViewInfo(DcFile file, DatasetView view) throws IOException, NoSuchFileException {
        try(DatasetDAO dsdao = daoFactory.newDatasetDAO()) {
            DatasetViewInfo ret = dsdao.getDatasetViewInfo(file.asRecord(), view);
            if(ret == null){
                throw new NoSuchFileException(String.format("Invalid View. Version %d not found", view.getVersionId()));
            }
            return ret;
        }
    }
    
    /*public void createDatasetView(Path path, DatasetVersion verRequest, DatasetLocation locRequest, Set<DatasetOption> options) throws IOException{
        createDatasetView( path, verRequest, Arrays.asList(locRequest), options );
    }
    
    public void createDatasetView(Path path, DatasetVersion verRequest, Collection<DatasetLocation> locRequest, Set<DatasetOption> options) throws IOException{
        DcPath dsPath = checkPath( path );
        DcFile dsFile = resolveFile(dsPath);
        if(options.contains(DatasetOption.CREATE_NODE)){
            throw new UnsupportedOperationException("This method cannot create a node");
        }
        if(!dsFile.isRegularFile() ){
            AfsException.NO_SUCH_FILE.throwError(path, "Path is not a dataset");
        }
        Dataset ds = (Dataset) dsFile.getObject();
        
        //checkPermission(dsFile, DcPermissions.MODIFY);
        try (DatasetDAO dao = daoFactory.newDatasetDAO()){
            createDatasetViewInternal(dao, ds, verRequest, locRequest, null, options);
            dao.commit();
            dao.close();
        }
        dsFile.getAttributeView(DatasetViewProvider.class).clear();
        resolveFile(dsPath.getParent()).childModified(dsPath);
    }*/
    
    /**
     * Create a Dataset View.
     * 
     * If options Contains CREATE_VERSION or MERGE_VERSION, we will attempt to create or merge the
     * requestVersion. If SKIP_VERSION_CHECK was present, we will not attempt to check the
     * data store for the current version for performance reasons.
     * 
     * If options Contains CREATE_LOCATION, we will attempt to create a location.
     * If SKIP_VERSION_CHECK was present, we add the location to the version represented by 
     * requestVersion, otherwise, we fetch and use the current version.
     * 
     * @param ds
     * @param requestVersion
     * @param requestLocation
     * @param returnLocations Location Builder to update
     * @param options Set of flags to aid in the view build
     * @throws IOException 
     */
    private DatasetViewInfo createDatasetViewInternal(DatasetDAO dao, Dataset ds, DatasetViewInfo reqView, Set<DatasetOption> options) throws IOException {        
        Set<DatasetOption> dsOptions = new HashSet<>(options); // make a copy
        boolean mergeVersion = dsOptions.remove(DatasetOption.MERGE_VERSION);
        boolean createVersion = dsOptions.remove(DatasetOption.CREATE_VERSION);
        boolean createLocations = dsOptions.remove(DatasetOption.CREATE_LOCATIONS);
        boolean skipVersionCheck = dsOptions.remove(DatasetOption.SKIP_VERSION_CHECK);
        boolean skipLocationCheck = dsOptions.remove(DatasetOption.SKIP_LOCATION_CHECK);
        List<DatasetLocation> retLocations = new ArrayList<>();
        DatacatRecord dsRecord = ds;
        DatasetVersion curVersion = null;
        
        if(createVersion || mergeVersion) {
            if(!reqView.versionOpt().isPresent()){
                throw new IllegalArgumentException("Missing version from request");
            }
            Optional<DatasetVersion> versionOpt = Optional.fromNullable(skipVersionCheck ? null : dao.getCurrentVersion(dsRecord));
            curVersion = dao.createOrMergeDatasetVersion(dsRecord, reqView.getVersion(), versionOpt, mergeVersion);
            skipLocationCheck = true;
        } else {
            /* We didn't create or merge a version, so we're only creating a location.
               If skipVersionCheck, use reqVersion otherwise get current version */
            curVersion = skipVersionCheck ? reqView.getVersion() : dao.getCurrentVersion(dsRecord);
        }

        if(createLocations){
            if(curVersion == null){
                DcFsException.NO_SUCH_VERSION.throwError(dsRecord.getPath(), "No version exists which we can add a location to");
            }
            if(!reqView.locationsOpt().isPresent() || reqView.getLocations().isEmpty()){
                throw new IllegalArgumentException("Unable to create the view specified without locations");
            }
            for(DatasetLocation reqLocation: reqView.getLocations()){
                DatasetLocation l = dao.createDatasetLocation(curVersion, reqLocation, skipLocationCheck);
                retLocations.add(l);
            }
        }
        return new DatasetViewInfo(curVersion, retLocations);
    }
    
    @Override
    public void createDirectory(Path dir,
            FileAttribute<?>... attrs) throws IOException {
        DcPath targetDir = checkPath(dir);
        try {
            resolveFile(targetDir);
            AfsException.FILE_EXISTS.throwError(targetDir, "A group or folder already exists at this location");
        } catch (NoSuchFileException ex){
            // Do nothing.
        }
        DcFile parent = resolveFile(targetDir.getParent());
        if(parent.getType() != FileType.DIRECTORY){ // Use the constant instead of instanceof
            AfsException.NOT_DIRECTORY.throwError( parent, "The parent file is not a folder");
        }
        checkPermission(targetDir.getUserName(), parent, DcPermissions.INSERT);
        
        if(attrs.length != 1){
            throw new IOException("Only one attribute allowed for dataset creation");
        }
        
        if( !(attrs[0] instanceof ContainerCreationAttribute) ){
                throw new IOException("Creation attribute not valid for creating a dataset");
        }
        ContainerCreationAttribute dsAttr = (ContainerCreationAttribute) attrs[0];
        DatacatObject request = dsAttr.value();
        try (ContainerDAO dao = daoFactory.newContainerDAO(targetDir)){
            DatacatObject ret = dao.createNode(parent.asRecord(), targetDir.getFileName().toString(), request);
            dao.commit();
            parent.childAdded(targetDir, FileType.DIRECTORY);
            DcFile f = buildChild(parent, targetDir, ret);
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
        try(BaseDAO dao = daoFactory.newBaseDAO()){
            DcFile file = resolveFile(dcPath);
            checkPermission(dcPath.getUserName(), file, DcPermissions.DELETE);
            dao.delete(file.asRecord());
            dao.commit();
        }
        DcFile parentFile = resolveFile(dcPath.getParent());
        getCache().removeFile(dcPath);
        parentFile.childRemoved(dcPath);
    }
    
    public void checkPermission(String userName, DcFile file, AclEntryPermission permission) throws IOException {
        DcFileSystem fs = file.getPath().getFileSystem();
        DcUser user = fs.getUserPrincipalLookupService().lookupPrincipalByName(userName);
        Set<DcGroup> usersGroups = fs.getUserPrincipalLookupService().lookupGroupsForUser(user);
        DcAclFileAttributeView aclView = file.getAttributeView(DcAclFileAttributeView.class);

        if(!permissionsCheck(usersGroups, aclView.getAcl(), permission)){
            String err = String.format("No Access Control Entries Found: User %s", user.getName());
            AfsException.ACCESS_DENIED.throwError(file.getPath(), err );
        }
    }
    
    /**
     * usersGroups are all the groups the user is in. acls is t
     * @param usersGroups all the groups the user is in
     * @param acl the acl for the file
     * @param permission The permission requested
     * @return 
     */
    private boolean permissionsCheck(Set<DcGroup> usersGroups, List<AclEntry> acl, AclEntryPermission permission){
        for(AclEntry entry: acl){
            if(usersGroups.contains( (DcGroup)entry.principal() )){
                if(entry.permissions().contains(permission)){
                    return true;
                }
            }
        }
        return false;
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
        DATASET_EXISTS,
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
