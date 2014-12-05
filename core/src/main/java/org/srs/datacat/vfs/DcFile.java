package org.srs.datacat.vfs;

import java.io.IOException;
import org.srs.datacat.shared.DatacatObject.Type;

import java.nio.file.attribute.AttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.model.DatasetModel;
import org.srs.vfs.AbstractVirtualFile;
import org.srs.vfs.ChildrenView;
import org.srs.datacat.vfs.attribute.ContainerViewProvider;
import org.srs.datacat.vfs.attribute.DatasetViewProvider;
import org.srs.datacat.vfs.attribute.SubdirectoryView;
import org.srs.datacat.vfs.attribute.DcAclFileAttributeView;
import org.srs.vfs.FileType;

/**
 * A wrapper class that represents a materialized DatacatObject with some common file-like mappings.
 * It's used for caching, and it can contain any number of views.
 * 
 * @author bvan
 */
public class DcFile extends AbstractVirtualFile<DcPath, Long> implements BasicFileAttributeView {

    {
        addViewName(DcAclFileAttributeView.class, "acl");
        addViewName(DcFile.class, "basic");
        addViewName(SubdirectoryView.class, "subdirectories");
    }

    /**
     * Marker for alternative class of FileType.Directory.
     */
    public static class GroupType extends FileType.Directory {}

    private final DatacatNode dcObject;
    private final long dcObjectCreation = System.currentTimeMillis();

    public DcFile(DcPath path, DatacatNode object, DcAclFileAttributeView aclView){
        super(path, fileType(object));
        this.dcObject = object;
        addAttributeViews(aclView);
        initViews(object);
    }

    private void initViews(DatacatNode orig){
        addAttributeViews(this);
        if(isRegularFile() && orig instanceof DatasetModel){
            addAttributeViews(new DatasetViewProvider(this, (DatasetModel) orig));
        }
        if(isDirectory()){
            addAttributeViews(new ChildrenView<>(getPath()));
            addAttributeViews(new SubdirectoryView(getPath()));
            addAttributeViews(new ContainerViewProvider(this));
        }
    }

    protected static FileType fileType(DatacatNode o){
        switch(Type.typeOf(o)){
            case GROUP:
                return new GroupType();
            case FOLDER:
                return FileType.DIRECTORY;
            case DATASET:
                return FileType.FILE;
            default:
                return FileType.FILE;
        }
    }

    @Override
    public DcPath getPath(){
        return (DcPath) super.getPath();
    }

    @Override
    public <T extends AttributeView> T getAttributeView(Class<T> view){
        return (T) getAttributeViews().get(getViewName(view));
    }

    @Override
    public Collection<? extends AttributeView> getAttributeViews(
            Class<? extends AttributeView>... views){
        ArrayList<AttributeView> attrViews = new ArrayList<>();
        for(Class<? extends AttributeView> v: views){
            attrViews.add(getAttributeView(v));
        }
        return attrViews;
    }

    @Override
    public FileTime lastModifiedTime(){
        // TODO: Fix Times
        return FileTime.fromMillis(dcObjectCreation);
    }

    @Override
    public FileTime lastAccessTime(){
        // TODO: Fix Times
        return FileTime.fromMillis(dcObjectCreation);
    }

    @Override
    public FileTime creationTime(){
        // TODO: Fix Times
        return FileTime.fromMillis(dcObjectCreation);
    }

    @Override
    public boolean isRegularFile(){
        return getType() instanceof FileType.File;
    }

    @Override
    public boolean isDirectory(){
        return getType() instanceof FileType.Directory;
    }

    @Override
    public boolean isSymbolicLink(){
        return false;
    }

    @Override
    public boolean isOther(){
        return getType() instanceof GroupType;
    }

    @Override
    public long size(){
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Long fileKey(){
        return dcObject.getPk();
    }

    public DatacatNode getObject(){
        return this.dcObject;
    }

    public DatacatRecord asRecord(){
        return this.dcObject;
    }

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime,
            FileTime createTime) throws IOException{
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void childRemoved(DcPath child){
        if(!isDirectory()){
            return;
        }
        String fname = child.getFileName().toString();
        getAttributeView(ChildrenView.class).unlink(fname);
        getAttributeView(SubdirectoryView.class).unlink(fname);
        getAttributeView(ContainerViewProvider.class).clearStats();
    }

    public void datasetAdded(DcPath child){
        getAttributeView(ChildrenView.class).link(child);
        getAttributeView(ContainerViewProvider.class).clearStats();
    }

    public void childAdded(DcPath child, FileType fileType){
        getAttributeView(ChildrenView.class).link(child);
        if(fileType instanceof FileType.Directory){
            getAttributeView(SubdirectoryView.class).link(child);
        }
        getAttributeView(ContainerViewProvider.class).clearStats();
    }

    public void childModified(DcPath child){
        if(!isDirectory()){
            return;
        }
        String fname = child.getFileName().toString();
        if(getAttributeView(ChildrenView.class).unlink(fname)){
            getAttributeView(ChildrenView.class).link(child);
        }
        if(getAttributeView(SubdirectoryView.class).unlink(fname)){
            getAttributeView(SubdirectoryView.class).link(child);
        }
        getAttributeView(ContainerViewProvider.class).clearStats();
    }

    @Override
    public String toString(){
        return "DcFile{" + "object=" + dcObject.toString() + '}';
    }
}
