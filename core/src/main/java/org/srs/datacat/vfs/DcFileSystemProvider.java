package org.srs.datacat.vfs;

import com.google.common.base.Optional;

import java.io.IOException;
import java.net.URI;    
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.text.ParseException;

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
import org.srs.datacat.model.ModelProvider;
import org.srs.datacat.model.dataset.DatasetViewInfoModel;
import org.srs.datacat.model.dataset.DatasetWithViewModel;
import org.srs.datacat.model.dataset.DatasetOption;
import org.srs.datacat.model.container.ContainerStat;

import org.srs.datacat.model.security.AclTransformation;
import org.srs.datacat.model.security.CallContext;
import org.srs.datacat.model.security.DcAclEntry;
import org.srs.datacat.model.security.DcAclEntryScope;
import org.srs.datacat.model.security.DcGroup;
import org.srs.datacat.model.security.DcPermissions;

import org.srs.datacat.dao.BaseDAO;
import org.srs.datacat.dao.ContainerDAO;
import org.srs.datacat.dao.DatasetDAO;
import org.srs.datacat.dao.DAOFactory;
import org.srs.datacat.dao.SearchDAO;

import org.srs.datacat.vfs.DirectoryWalker.ContainerVisitor;
import org.srs.datacat.vfs.attribute.ContainerViewProvider;

import org.srs.vfs.AbstractFsProvider.AfsException;
import org.srs.vfs.FileType;
import org.srs.vfs.GlobToRegex;
import org.srs.vfs.PathProvider;
import org.srs.vfs.PathUtils;
import org.srs.vfs.VfsCache;
import org.srs.vfs.VfsSoftCache;

/**
 *
 * @author bvan
 */
public class DcFileSystemProvider {

    private static final long MAX_CHILD_CACHE = 500;
    private static final long MAX_METADATA_STRING_BYTE_SIZE = 5000;
    private static final long MAX_DATASET_CACHE_SIZE = 1 << 29; // Don't blow more than about 512MB
    private static final int NO_MAX = -1;
    private static final long MAX_CACHE_TIME = 60000L; // TODO: Get rid of this - 60 seconds

    private final DAOFactory daoFactory;
    private final ModelProvider modelProvider;
    private final VfsCache<DcFile> cache = new VfsSoftCache<>();
    
    public DcFileSystemProvider(DAOFactory daoFactory, ModelProvider modelProvider) throws IOException{
        this.daoFactory = daoFactory;
        this.modelProvider = modelProvider;
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
    
    private static final PathProvider<DcPath> PATH_PROVIDER = new PathProvider<DcPath>(){

        @Override
        public DcPath getRoot(){
            return new DcPath(this, "/");
        }

        @Override
        public DcPath getPath(URI uri){
            return new DcPath(this, uri.getPath());
        }

        @Override
        public DcPath getPath(String path){
            return new DcPath(this, path);
        }
    };
    
    private VfsCache<DcFile> getCache(){
        return cache;
    }
    
    private DcFile resolveFile(Path path) throws NoSuchFileException, IOException {
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

    public DirectoryStream<Path> newOptimizedDirectoryStream(Path dir, CallContext context,
            final DirectoryStream.Filter<? super Path> filter, int max, 
            Optional<DatasetView> viewPrefetch) throws IOException{
        DcFile dirFile = resolveFile(dir);
        checkPermission(context, dirFile, DcPermissions.READ);
        if(!dirFile.isDirectory()){
            throw new NotDirectoryException(dirFile.toString());
        }
        ChildrenView view = dirFile.getAttributeView(ChildrenView.class);
        DirectoryStream<Path> stream;
        boolean useCache = viewPrefetch.isPresent() ? maybeUseCache(dirFile, viewPrefetch.get()) : false;
        if(view != null && useCache){
            if(!view.hasCache()){
                view.refreshCache();
            }
            stream = cachedDirectoryStream(dir, context, filter);
        } else {
            boolean fillCache = viewPrefetch.isPresent() ? 
                    canFitDatasetsInCache(dirFile, max, viewPrefetch.get()) : false;
            stream = unCachedDirectoryStream(dir, filter, viewPrefetch, fillCache);
        }
        return stream;
    }
    
    protected DirectoryStream<Path> unCachedDirectoryStream(final Path dir,
            final DirectoryStream.Filter<? super Path> filter, final Optional<DatasetView> view,
            final boolean cacheDatasets) throws IOException{
        final DcFile dirFile = resolveFile(dir);
        if(!dirFile.isDirectory()){
            throw new NotDirectoryException(dirFile.toString());
        }

        // !IMPORTANT!: This object is closed when the stream is closed
        final ContainerDAO dao = daoFactory.newContainerDAO();
        DirectoryStream<DatacatNode> stream;
        stream = dao.getChildrenStream(dirFile.getObject(), view);

        final Iterator<DatacatNode> iter = stream.iterator();
        final AtomicInteger dsCount = new AtomicInteger();
        DirectoryStreamWrapper.IteratorAcceptor acceptor
            = new DirectoryStreamWrapper.IteratorAcceptor() {

                @Override
                public boolean acceptNext() throws IOException{
                    while(iter.hasNext()){
                        DatacatNode child = iter.next();
                        Path maybeNext = dir.resolve(child.getName());
                        DcFile file = DcFileSystemProvider.this.
                                buildChild(dirFile, maybeNext, child);
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

        DirectoryStreamWrapper<Path> wrapper
            = new DirectoryStreamWrapper<Path>(stream, acceptor) {

                @Override
                public void close() throws IOException{
                    // TODO: This assumes datasets
                    if(dsCount.get() > 0){
                        dirFile.getAttributeView(ContainerViewProvider.class)
                            .setViewStats(view.get(), dsCount.get());
                    }
                    super.close();
                    dao.close();  // Make sure to close dao (and underlying connection)
                }

            };
        return wrapper;
    }

    protected DirectoryStream<Path> cachedDirectoryStream(Path dir, CallContext context,
            final DirectoryStream.Filter<? super Path> filter) throws IOException{
        final DcFile dirFile = resolveFile(dir);
        checkPermission(context, dirFile, DcPermissions.READ);
        final ChildrenView view = dirFile.getAttributeView(ChildrenView.class);
        if(!view.hasCache()){
            throw new IOException("Error attempting to use cached child entries");
        }

        final Iterator<Path> iter = view.getChildrenPaths().iterator();
        DirectoryStreamWrapper<Path> wrapper = new DirectoryStreamWrapper<>(
                new DirectoryStreamWrapper.IteratorAcceptor() {

                    @Override
                    public boolean acceptNext() throws IOException{
                        while(iter.hasNext()){
                            Path maybeNext = iter.next();
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
        long count = max;
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
     * @param path Datacat path
     * @param context Call context.
     * @return The DcFile at given path
     */
    public DcFile getFile(Path path, CallContext context) throws IOException, NoSuchFileException{
        /* TODO: When we have control over file creation, remove this and replace it with
         some sort of distributed consensus stuff potentially.
         */
        DcFile f = resolveFile(path);
        if((System.currentTimeMillis() - f.lastModifiedTime().toMillis()) > MAX_CACHE_TIME){
            getCache().removeFile(path);
            f = resolveFile(path);
        }
        checkPermission(context, f, DcPermissions.READ);
        return f;
    }
    
    public Path getPath(String path){
        return PATH_PROVIDER.getPath(path);
    }
    
    private boolean exists(Path path){
        try {
            resolveFile(path);
            // file exists
            return true;
        } catch(IOException x) {
            // does not exist or unable to determine if file exists
            return false;
        }
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
     * @param context Call context.
     * @param dsReq Representation of dataset to create.
     * @param options Optimization flags.
     * @return Dataset, FlatDataset, or FullDataset
     */
    public DatasetModel createDataset(Path path, CallContext context, 
            DatasetModel dsReq, Set<DatasetOption> options) throws IOException{
        if(dsReq == null){
            throw new IOException("Not enough information to create create a Dataset node or view");
        }
        
        DcFile dsParent = resolveFile(path.getParent());
        String dsName = path.getFileName().toString();
        Set<DatasetOption> dsOptions = new HashSet<>(options); // make a copy

        Optional<DatasetModel> requestDataset = Optional.absent();
        Optional<DatasetViewInfoModel> requestView = Optional.absent();

        boolean createNode = dsOptions.remove(DatasetOption.CREATE_NODE);

        if(createNode){
            checkPermission(context, dsParent, DcPermissions.INSERT);
            requestDataset = Optional.of(dsReq);
        }
        HashSet<DatasetOption> viewWork = new HashSet<>(Arrays.asList(
                DatasetOption.CREATE_VERSION,
                DatasetOption.MERGE_VERSION,
                DatasetOption.CREATE_LOCATIONS));
        viewWork.retainAll(dsOptions);
        if(!viewWork.isEmpty()){
            checkPermission(context, dsParent, DcPermissions.WRITE);
            if(dsReq instanceof DatasetWithViewModel){
                requestView = Optional.of(((DatasetWithViewModel) dsReq).getViewInfo());
            } else {
                throw new IllegalArgumentException("Unable to fulfill request");
            }
        }
        try(DatasetDAO dao = daoFactory.newDatasetDAO(path)) {
            DatasetModel ret = dao.
                    createDataset(dsParent.getObject(), dsName, requestDataset, requestView, dsOptions);
            dao.commit();
            dsParent.childAdded(path, FileType.FILE);
            return ret;
        }
    }
    
    /**
     * Patch ACLs.
     *
     * @param path Path of this DatacatNode to patch
     * @param context Call context.
     * @param request List of ACLs to to merge.
     * @param clear Flag to overwrite access-scoped entries and ignore default-scoped entries
     * @return Updated representation of the DcFile.
     */
    public DcFile mergeContainerAclEntries(Path path, CallContext context, 
            List<DcAclEntry> request, boolean clear) throws IOException{
        DcFile f = getFile(path, context);
        try {
            checkPermission(context, f, DcPermissions.ADMIN);
        } catch (AccessDeniedException ex){
            // If there is admin entries on the root folder, allow those through as well.
            checkPermission(context, getFile(path.getName(0), context), DcPermissions.ADMIN);
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
        getCache().removeFile(path);
        return getFile(path, context);
    }
    
    /**
     * Patch a container.
     *
     * @param path Path of container to patch.
     * @param context Call context.
     * @param request An object representing the diff of the object to be patched.
     * @return DcFile representing updated container
     */
    public DcFile patchContainer(Path path, CallContext context, DatasetContainer request) throws IOException{
        DcFile f = getFile(path, context);
        checkPermission(context, f, DcPermissions.WRITE);
        
        if(f.getType() != FileType.DIRECTORY){ // Use the constant instead of instanceof
            AfsException.NO_SUCH_FILE.throwError(f, "The file to be patched is a container");
        }
        
        DatacatNode container = f.getObject();
                
        try(ContainerDAO dao = daoFactory.newContainerDAO(path)) {
            dao.patchContainer(container, request);
            dao.commit();
        }
        getCache().removeFile(path);
        return getFile(path, context);
    }
    
    /**
     * Search using a path pattern and a query.
     * @param pathPattern A glob or regex pattern
     * @param context Call Context
     * @param checkFolders Check inside folders
     * @param checkGroups Check inside Groups
     * @param datasetView Apply this view to all datasets
     * @param query A Query String
     * @param retrieveFields Metadata fields to retrieve
     * @param sortFields Fields to sort on.
     * @return Stream of datasets. Make sure to close the stream when done.
     */
    public DirectoryStream<DatasetModel> search(String pathPattern, CallContext context, 
            Boolean checkFolders, Boolean checkGroups, DatasetView datasetView, String query, 
            String[] retrieveFields, String[] sortFields) throws IOException, ParseException{
        String searchBase = PathUtils.normalizeRegex(GlobToRegex.toRegex(pathPattern, "/"));
        Path root = PATH_PROVIDER.getRoot();
        Path searchPath = root.resolve(searchBase);
        ContainerVisitor visitor= new ContainerVisitor(pathPattern, checkGroups, checkFolders);
        DirectoryWalker walker = new DirectoryWalker(this, visitor, 100 /* max depth */);
        walker.walk(searchPath, context);
        final SearchDAO dao = daoFactory.newSearchDAO();
        
        final DirectoryStream<DatasetModel> search;
        
        // The retrieval of the DirectoryStream can fail, so we should clean up if that happens
        try {
            search = dao.search(visitor.files, datasetView, query, retrieveFields, sortFields);
        } catch (ParseException | RuntimeException | IOException ex){
            dao.close();
            throw ex;
        }

        // Wrap the actual DirectoryStream and add method to close DAO
        return new DirectoryStream<DatasetModel>(){

            @Override
            public Iterator<DatasetModel> iterator(){
                return search.iterator();
            }

            @Override
            public void close() throws IOException{
                if(search != null){
                    search.close();
                }
                dao.close();
            }

        };
    }
    
    /**
     * Patch a dataset.
     *
     * @param path Path of dataset to patch. Dataset must exist.
     * @param context Call Context.
     * @param view DatasetView to patch.
     * @param request A diff representation of the dataset to be patched
     * @return DcFile representing the updated DcFile and dataset
     */
    public DcFile patchDataset(Path path, CallContext context, 
            DatasetView view, DatasetModel request) throws IOException{
        DcFile f = getFile(path, context);
        checkPermission(context, f, DcPermissions.WRITE);
        DatacatNode ds = f.getObject();
        
        Optional<DatasetModel> requestDataset = Optional.of(request);
        Optional<DatasetViewInfoModel> requestView = Optional.absent();
        
        if(f.getType() != FileType.FILE){ // Use the constant instead of instanceof
            AfsException.NO_SUCH_FILE.throwError(f, "The file to be patched is a container");
        }

        if(request instanceof DatasetWithViewModel){
            requestView = Optional.of(((DatasetWithViewModel) request).getViewInfo());
        }
        
        try(DatasetDAO dao = daoFactory.newDatasetDAO(path)) {
            dao.patchDataset(ds, view, requestDataset, requestView);
            dao.commit();
        }
        getCache().removeFile(path);
        return getFile(path, context);
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

    public void createDirectory(Path path, CallContext context,
            DatacatNode request) throws IOException{
        if(exists(path)){
            String msg = "A group or folder already exists at this location";
            AfsException.FILE_EXISTS.throwError(path, msg);
        }
        DcFile parent = resolveFile(path.getParent());
        if(parent.getType() != FileType.DIRECTORY){ // Use the constant instead of instanceof
            AfsException.NOT_DIRECTORY.throwError(parent, "The parent file is not a folder");
        }
        checkPermission(context, parent, DcPermissions.INSERT);

        try(ContainerDAO dao = daoFactory.newContainerDAO(path)){
            String fileName = path.getFileName().toString();
            DatacatNode ret = dao.createNode(parent.getObject(), fileName, request);
            dao.commit();
            parent.childAdded(path, FileType.DIRECTORY);
            DcFile f = buildChild(parent, path, ret);
            getCache().putFile(f);
        }
    }

    public void delete(Path path, CallContext context) throws IOException{
        try(BaseDAO dao = daoFactory.newBaseDAO()) {
            DcFile file = resolveFile(path);
            checkPermission(context, file, DcPermissions.DELETE);
            dao.delete(file.getObject());
            dao.commit();
        }
        DcFile parentFile = resolveFile(path.getParent());
        getCache().removeFile(path);
        parentFile.childRemoved(path);
    }

    private void checkPermission(CallContext context, DcFile file, DcPermissions permission) throws IOException{
        Set<DcGroup> usersGroups = context.getGroups();
        List<DcAclEntry> acl = file.getAcl();

        if(!DcPermissions.check(usersGroups, acl, permission)){
            String err = String.format("No permission entries for %s found", permission);
            AfsException.ACCESS_DENIED.throwError(file.getPath(), err);
        }
    }

}
