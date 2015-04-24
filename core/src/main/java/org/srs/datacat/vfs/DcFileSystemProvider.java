package org.srs.datacat.vfs;

import com.google.common.base.Optional;
import org.srs.datacat.model.security.DcPermissions;
import java.io.IOException;
import java.net.URI;    
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;

import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.dataset.DatasetWithViewModel;
import org.srs.datacat.model.dataset.DatasetOption;
import org.srs.datacat.model.container.ContainerStat;

import org.srs.datacat.dao.BaseDAO;
import org.srs.datacat.dao.ContainerDAO;
import org.srs.datacat.dao.DatasetDAO;
import org.srs.datacat.dao.DAOFactory;
import org.srs.datacat.model.ModelProvider;
import org.srs.datacat.model.dataset.DatasetViewInfoModel;

import org.srs.datacat.model.security.DcAclEntry;
import org.srs.datacat.model.security.DcAclEntryScope;
import org.srs.datacat.model.security.DcGroup;
import org.srs.datacat.model.security.DcUser;

import org.srs.datacat.model.security.AclTransformation;
import org.srs.datacat.security.DcUserLookupService;

import org.srs.datacat.vfs.attribute.ChildrenView;
import org.srs.datacat.vfs.attribute.ContainerCreationAttribute;
import org.srs.datacat.vfs.attribute.ContainerViewProvider;

import org.srs.vfs.AbstractPath;
import org.srs.vfs.AbstractFsProvider.AfsException;
import org.srs.vfs.FileType;
import org.srs.vfs.VfsCache;
import org.srs.vfs.VfsSoftCache;

/**
 *
 * @author bvan
 */
public class DcFileSystemProvider {

    private static final long MAX_CHILD_CACHE = 500;
    private static final int MAX_METADATA_STRING_BYTE_SIZE = 5000;
    private static final long MAX_DATASET_CACHE_SIZE = 1 << 29; // Don't blow more than about 512MB
    private static final int NO_MAX = -1;
    private static final long MAX_CACHE_TIME = 60000L; // TODO: Get rid of this - 60 seconds

    private final DcFileSystem fileSystem;
    private final DAOFactory daoFactory;
    private final ModelProvider modelProvider;
    private final DcUserLookupService userLookupService;
    private final VfsCache<DcFile> cache = new VfsSoftCache<>();
    
    public DcFileSystemProvider(DAOFactory daoFactory, 
            ModelProvider modelProvider, DcUserLookupService userLookupService) throws IOException{
        super();
        this.daoFactory = daoFactory;
        this.modelProvider = modelProvider;
        this.userLookupService = userLookupService;
        this.fileSystem = new DcFileSystem();
    }
    
    public DcFileSystem getFileSystem(){
        return fileSystem;
    }

    public DAOFactory getDaoFactory(){
        return daoFactory;
    }
    
    public ModelProvider getModelProvider(){
        return modelProvider;
    }
    
    public static final DirectoryStream.Filter<Path> ACCEPT_ALL_FILTER
        = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException{
                return true;
            }
        };
    
    private VfsCache<DcFile> getCache(){
        return cache;
    }
    
    private DcFile resolveFile(DcPath path) throws NoSuchFileException, IOException {
        // Find this file in the cache. If it's not in the cache, resolve it's parents
        // (thereby putting them in the cache), and eventually this file.
        DcFile file = getCache().getFile(path);
        if(file == null){
            DcFile parent = null;
            if(!path.equals( path.getRoot())){
                parent = resolveFile(path.getParent());
            }
            file = retrieveFileAttributes(path, parent);
            getCache().putFile(file);
            return file;
        }
        return file;
    }

    public DirectoryStream<DcPath> newOptimizedDirectoryStream(Path dir,
            final DirectoryStream.Filter<? super Path> filter, int max, DatasetView viewPrefetch) throws IOException{
        final DcPath dcPath = checkPath(dir);
        DcFile dirFile = resolveFile(dcPath);
        checkPermission(dcPath.getUserName(), dirFile, DcPermissions.READ);
        if(!dirFile.isDirectory()){
            throw new NotDirectoryException(dirFile.toString());
        }
        ChildrenView view = dirFile.getAttributeView(ChildrenView.class);
        DirectoryStream<DcPath> stream;
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
        return stream;
    }

    public DirectoryStream<DcPath> unCachedDirectoryStream(final Path dir,
            final DirectoryStream.Filter<? super Path> filter, final DatasetView viewPrefetch,
            final boolean cacheDatasets) throws IOException{
        final DcPath dcPath = checkPath(dir);
        final DcFile dirFile = resolveFile(dcPath);
        checkPermission(dcPath.getUserName(), dirFile, DcPermissions.READ);
        if(!dirFile.isDirectory()){
            throw new NotDirectoryException(dirFile.toString());
        }

        // !IMPORTANT!: This object is closed when the stream is closed
        final ContainerDAO dao = daoFactory.newContainerDAO();
        DirectoryStream<DatacatNode> stream;
        Optional<DatasetView> view = Optional.fromNullable(viewPrefetch);
        stream = dao.getChildrenStream(dirFile.getObject(), view);

        final Iterator<DatacatNode> iter = stream.iterator();
        final AtomicInteger dsCount = new AtomicInteger();
        DirectoryStreamWrapper.IteratorAcceptor acceptor
            = new DirectoryStreamWrapper.IteratorAcceptor() {

                @Override
                public boolean acceptNext() throws IOException{
                    while(iter.hasNext()){
                        DatacatNode child = iter.next();
                        Path maybeNext = dcPath.resolve(child.getName());
                        DcFile file = DcFileSystemProvider.this.
                            buildChild(dirFile, maybeNext, child);
                        /* TODO: Permissions check here?
                        try{
                            DcFileSystemProvider.this.checkPermission(dcPath.getUserName(), file, DcPermissions.READ);
                        } catch (AccessDeniedException ex){
                            continue;
                        }
                        */
                        if(file.isDirectory()){
                            getCache().putFileIfAbsent(file);
                        }
                        if(!file.isDirectory() && cacheDatasets){
                            getCache().putFileIfAbsent(file);
                            dsCount.incrementAndGet();
                        }
                        if(filter.accept(maybeNext)){
                            setNext(maybeNext);
                            return true;
                        }
                    }
                    throw new NoSuchElementException();
                }
            };

        DirectoryStreamWrapper<DcPath> wrapper
            = new DirectoryStreamWrapper<DcPath>(stream, acceptor) {

                @Override
                public void close() throws IOException{
                    if(dsCount.get() > 0){
                        dirFile.getAttributeView(ContainerViewProvider.class)
                            .setViewStats(viewPrefetch, dsCount.get());
                    }
                    super.close();
                    dao.close();  // Make sure to close dao (and underlying connection)
                }

            };
        return wrapper;
    }

    protected DirectoryStream<DcPath> cachedDirectoryStream(Path dir,
            final DirectoryStream.Filter<? super Path> filter) throws IOException{
        final DcPath dcPath = checkPath(dir);
        final DcFile dirFile = resolveFile(dcPath);
        checkPermission(dcPath.getUserName(), dirFile, DcPermissions.READ);
        final ChildrenView view = dirFile.getAttributeView(ChildrenView.class);
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
                            /* TODO: Permissions check here?
                            DcFile file = DcFileSystemProvider.this.resolveFile(maybeNext);
                            try{
                                DcFileSystemProvider.this.
                                    checkPermission(dcPath.getUserName(), file, DcPermissions.READ);
                            } catch (AccessDeniedException ex){
                                continue;
                            }
                            */
                            if(filter.accept(maybeNext)){
                                setNext(maybeNext);
                                return true;
                            }
                        }
                        throw new NoSuchElementException();
                    }
                });
        return wrapper;
    }

    /**
     * This checks to see if a given view is cached and, if it is, if there is enough items in the
     * cache to be worthwhile to use the cache.
     */
    private boolean maybeUseCache(DcFile dirFile, DatasetView viewPrefetch) throws IOException{
        // TODO: Fix caching of large results
        if(viewPrefetch == DatasetView.EMPTY){
            return true;
        }
        ContainerViewProvider cstat = dirFile.getAttributeView(ContainerViewProvider.class);
        DatasetContainer container = (DatasetContainer) cstat.withView(ContainerStat.class);
        int count = container.getStat().getChildCount();
        int cacheCount = cstat.getViewStats(viewPrefetch);
        return (count - cacheCount) < MAX_CHILD_CACHE;
    }

    /**
     * Tries to decide if we should even try to fit all the datasets in cache or not.
     */
    private boolean canFitDatasetsInCache(DcFile dirFile, int max, DatasetView viewPrefetch) throws IOException{
        // TODO: Improve logic
        ContainerViewProvider cstat = dirFile.getAttributeView(ContainerViewProvider.class);
        DatasetContainer container = (DatasetContainer) cstat.withView(ContainerStat.class);
        int count = max;
        if(count <= 0){
            count = container.getStat().getChildCount();
        }
        /*
         The average upper bound of metadata size (in bytes) for a given dataset is about
         5kB. We want to make sure that we don't cache more than about
         */
        long estimate = count * MAX_METADATA_STRING_BYTE_SIZE;
        // TODO: Check actual return size using the Data Access layer
        return estimate < MAX_DATASET_CACHE_SIZE;
    }

    /**
     * Gets a file.
     *
     * @param path
     * @return
     * @throws IOException
     */
    public DcFile getFile(Path path) throws IOException{
        DcPath dcPath = checkPath(path);
        /* TODO: When we have control over file creation, remove this and replace it with
         some sort of distributed consensus stuff potentially.
         */
        DcFile f = resolveFile(dcPath);
        if((System.currentTimeMillis() - f.lastModifiedTime().toMillis()) > MAX_CACHE_TIME){
            getCache().removeFile(dcPath);
            f = resolveFile(dcPath);
        }
        if(f != null){
            checkPermission(dcPath.getUserName(), f, DcPermissions.READ);
            return f;
        }
        AfsException.NO_SUCH_FILE.throwError(dcPath, "Unable to resolve file");
        return null; // Keep compiler happy
    }
    
    public DcPath getPath(URI uri){
        return fileSystem.getPathProvider().getPath(uri);
    }

    private boolean exists(Path path){
        try {
            resolveFile(checkPath(path));
            // file exists
            return true;
        } catch(IOException x) {
            // does not exist or unable to determine if file exists
            return false;
        }
    }

    private DcPath checkPath(Path path){
        if(path instanceof DcPath){
            return (DcPath) path;
        }
        String user = path instanceof AbstractPath ? ((AbstractPath) path).getUserName() : null;
        return fileSystem.getPathProvider().getPath(user, path.toString());
    }

    private DcFile retrieveFileAttributes(Path path, DcFile parent) throws IOException{
        // LOG: Checking database
        try(BaseDAO dao = daoFactory.newBaseDAO()) {
            DatacatRecord parentRecord = parent != null ? parent.getObject() : null;
            return buildChild(parent, path, dao.getObjectInParent(parentRecord, path.getFileName().
                    toString()));
        }
    }

    private DcFile buildChild(DcFile parent, Path childPath, DatacatNode child) throws IOException{
        // Make copy with no userName
        childPath = getFileSystem().getPathProvider().getPath(null, childPath.toString());
        List<DcAclEntry> acl = AclTransformation.parseAcl(child.getAcl()).orNull();
        if(acl == null){
            acl = new ArrayList<>();
            // Inherit parent's attributes
            for(DcAclEntry e: parent.getAcl()){
                DcAclEntry defaultEntry = DcAclEntry.newBuilder(e)
                        .scope(DcAclEntryScope.DEFAULT)
                        .build();
                acl.add(defaultEntry);
            }
        }
        return new DcFile(childPath, this, child, acl);
    }
    
    /**
     * This will fail if there already exists a Dataset record.
     *
     * @param path Path of this new dataset
     * @param dsReq
     * @param options
     * @return Dataset, FlatDataset, or FullDataset
     * @throws IOException
     */
    public DatasetModel createDataset(Path path, DatasetModel dsReq, Set<DatasetOption> options) throws IOException{
        if(dsReq == null){
            throw new IOException("Not enough information to create create a Dataset node or view");
        }
        DcPath dsPath = checkPath(path);

        DcFile dsParent = resolveFile(dsPath.getParent());
        String dsName = path.getFileName().toString();
        Set<DatasetOption> dsOptions = new HashSet<>(options); // make a copy

        Optional<DatasetModel> requestDataset = Optional.absent();
        Optional<DatasetViewInfoModel> requestView = Optional.absent();

        boolean createNode = dsOptions.remove(DatasetOption.CREATE_NODE);

        if(createNode){
            checkPermission(dsPath.getUserName(), dsParent, DcPermissions.INSERT);
            requestDataset = Optional.of(dsReq);
        }
        HashSet<DatasetOption> viewWork = new HashSet<>(Arrays.asList(
                DatasetOption.CREATE_VERSION,
                DatasetOption.MERGE_VERSION,
                DatasetOption.CREATE_LOCATIONS));
        viewWork.retainAll(dsOptions);
        if(!viewWork.isEmpty()){
            checkPermission(dsPath.getUserName(), dsParent, DcPermissions.WRITE);
            if(dsReq instanceof DatasetWithViewModel){
                requestView = Optional.of(((DatasetWithViewModel) dsReq).getViewInfo());
            } else {
                throw new IllegalArgumentException("Unable to fulfill rquest");
            }
        }
        try(DatasetDAO dao = daoFactory.newDatasetDAO(dsPath)) {
            DatasetModel ret = dao.
                    createDataset(dsParent.getObject(), dsName, requestDataset, requestView, dsOptions);
            dao.commit();
            dsParent.childAdded(dsPath, FileType.FILE);
            return ret;
        }
    }
    
    /**
     * Patch ACLs.
     *
     * @param path
     * @param request
     * @param clear Flag to overwrite access-scoped entries and ignore default-scoped entries
     * @return
     * @throws IOException
     */
    public DcFile mergeContainerAclEntries(Path path, List<DcAclEntry> request, boolean clear) throws IOException{
        DcPath dcPath = checkPath(path);
        DcFile f = getFile(dcPath);
        try {
            checkPermission(dcPath.getUserName(), f, DcPermissions.ADMIN);
        } catch (AccessDeniedException ex){
            // If there is admin entries on the root folder, allow those through as well.
            checkPermission(dcPath.getUserName(), getFile(dcPath.getName(0)), DcPermissions.ADMIN);
        }

        if(f.getType() != FileType.DIRECTORY){ // Use the constant instead of instanceof
            AfsException.NO_SUCH_FILE.throwError(f, "Unable to set ACLs on dataset");
        }
        List<DcAclEntry> existing = clear ? Collections.<DcAclEntry>emptyList() : f.getAcl();
        List<DcAclEntry> newAcl = AclTransformation.mergeAclEntries(existing, request);
        
        try(BaseDAO dao = daoFactory.newBaseDAO()) {    
            dao.setAcl(f.getObject(), AclTransformation.aclToString(newAcl));
            dao.commit();
        }
        getCache().removeFile(dcPath);
        return getFile(dcPath);
    }
    
    /**
     * Patch a container.
     *
     * @param path
     * @param request
     * @return
     * @throws IOException
     */
    public DcFile patchContainer(Path path, DatasetContainer request) throws IOException{
        DcPath dcPath = checkPath(path);
        DcFile f = getFile(dcPath);
        checkPermission(dcPath.getUserName(), f, DcPermissions.WRITE);
        
        if(f.getType() != FileType.DIRECTORY){ // Use the constant instead of instanceof
            AfsException.NO_SUCH_FILE.throwError(f, "The file to be patched is a container");
        }
        
        DatacatNode container = f.getObject();
                
        try(ContainerDAO dao = daoFactory.newContainerDAO(dcPath)) {
            dao.patchContainer(container, request);
            dao.commit();
        }
        getCache().removeFile(dcPath);
        return getFile(dcPath);
    }
    
    /**
     * Patch a dataset.
     *
     * @param path
     * @param view
     * @param request
     * @return
     * @throws IOException
     */
    public DcFile patchDataset(Path path, DatasetView view, DatasetModel request) throws IOException{
        DcPath dsPath = checkPath(path);
        DcFile f = getFile(dsPath);
        checkPermission(dsPath.getUserName(), f, DcPermissions.WRITE);
        DatacatNode ds = f.getObject();
        
        Optional<DatasetModel> requestDataset = Optional.of(request);
        Optional<DatasetViewInfoModel> requestView = Optional.absent();
        
        if(f.getType() != FileType.FILE){ // Use the constant instead of instanceof
            AfsException.NO_SUCH_FILE.throwError(f, "The file to be patched is a container");
        }

        if(request instanceof DatasetWithViewModel){
            requestView = Optional.of(((DatasetWithViewModel) request).getViewInfo());
        }
        
        try(DatasetDAO dao = daoFactory.newDatasetDAO(dsPath)) {
            dao.patchDataset(ds, view, requestDataset, requestView);
            dao.commit();
        }
        getCache().removeFile(dsPath);
        return getFile(dsPath);
    }

    public DatasetViewInfoModel getDatasetViewInfo(DcFile file, 
            DatasetView view) throws IOException, NoSuchFileException{
        try(DatasetDAO dsdao = daoFactory.newDatasetDAO()) {
            DatasetViewInfoModel ret = dsdao.getDatasetViewInfo(file.getObject(), view);
            if(ret == null){
                String msg = String.format("Invalid View. Version %d not found", view.getVersionId());
                throw new NoSuchFileException(msg);
            }
            return ret;
        }
    }

    public void createDirectory(Path dir,
            FileAttribute<?>... attrs) throws IOException{
        DcPath targetDir = checkPath(dir);
        if(exists(targetDir)){
            String msg = "A group or folder already exists at this location";
            AfsException.FILE_EXISTS.throwError(targetDir, msg);
        }
        DcFile parent = resolveFile(targetDir.getParent());
        if(parent.getType() != FileType.DIRECTORY){ // Use the constant instead of instanceof
            AfsException.NOT_DIRECTORY.throwError(parent, "The parent file is not a folder");
        }
        checkPermission(targetDir.getUserName(), parent, DcPermissions.INSERT);

        if(attrs.length != 1){
            throw new IOException("Only one attribute allowed for dataset creation");
        }

        if(!(attrs[0] instanceof ContainerCreationAttribute)){
            throw new IOException("Creation attribute not valid for creating a dataset");
        }
        ContainerCreationAttribute dsAttr = (ContainerCreationAttribute) attrs[0];
        DatacatNode request = dsAttr.value();
        try(ContainerDAO dao = daoFactory.newContainerDAO(targetDir)){
            String fileName = targetDir.getFileName().toString();
            DatacatNode ret = dao.createNode(parent.getObject(), fileName, request);
            dao.commit();
            parent.childAdded(targetDir, FileType.DIRECTORY);
            DcFile f = buildChild(parent, targetDir, ret);
            getCache().putFile(f);
        }
    }

    public void delete(Path path) throws IOException{
        DcPath dcPath = checkPath(path);
        try(BaseDAO dao = daoFactory.newBaseDAO()) {
            DcFile file = resolveFile(dcPath);
            checkPermission(dcPath.getUserName(), file, DcPermissions.DELETE);
            dao.delete(file.getObject());
            dao.commit();
        }
        DcFile parentFile = resolveFile(dcPath.getParent());
        getCache().removeFile(dcPath);
        parentFile.childRemoved(dcPath);
    }

    private void checkPermission(String userName, DcFile file, DcPermissions permission) throws IOException{
        DcUser user = userLookupService.lookupPrincipalByName(userName);
        Set<DcGroup> usersGroups = userLookupService.lookupGroupsForUser(user);
        List<DcAclEntry> acl = file.getAcl();

        if(!permissionsCheck(usersGroups, acl, permission)){
            String err = String.format("No permission entries for %s found", permission);
            AfsException.ACCESS_DENIED.throwError(file.getPath(), err);
        }
    }

    /**
     * Do the actual permissions check.
     *
     * @param usersGroups all the groups the user is in
     * @param acl the acl for the file
     * @param permission The permission requested
     * @return
     */
    private boolean permissionsCheck(Set<DcGroup> usersGroups, List<DcAclEntry> acl, DcPermissions permission){
        for(DcAclEntry entry: acl){
            if(usersGroups.contains((DcGroup) entry.getSubject())){
                if(entry.getPermissions().contains(permission)){
                    return true;
                }
            }
        }
        return false;
    }
}
