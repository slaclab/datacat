
package org.srs.datacat.vfs;


import java.io.IOException;
import org.srs.datacat.shared.DatacatObject.Type;

import java.nio.file.attribute.AttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import org.srs.vfs.AbstractVirtualFile;
import org.srs.vfs.ChildrenView;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.vfs.attribute.ContainerViewProvider;
import org.srs.datacat.vfs.attribute.DatasetViewProvider;
import org.srs.datacat.vfs.attribute.SubdirectoryView;
import org.srs.datacat.vfs.security.DcAclFileAttributeView;
import org.srs.vfs.FileType;


/**
 *
 * @author bvan
 */
public class DcFile extends AbstractVirtualFile<DcPath, Long> implements BasicFileAttributeView {
    
    {
        addViewName( DcAclFileAttributeView.class, "acl");
        addViewName( DcFile.class, "basic");
        addViewName( SubdirectoryView.class, "subdirectories");
    }
    
    public static class GroupType extends FileType.Directory {}
    
    private final DatacatObject object;
    
    public DcFile(DcPath path, DatacatObject object){
        super(path, fileType(object));
        this.object = object;
        initViews();
    }
    
    private void initViews(){
        addAttributeViews(this);
        if(isRegularFile()){
            addAttributeViews(new DatasetViewProvider(this));
        }
        if(isDirectory()){
            addAttributeViews(new ChildrenView<>(getPath()));
            addAttributeViews(new SubdirectoryView(getPath()));
            addAttributeViews(new ContainerViewProvider(this));
        }
    }

    protected static FileType fileType(DatacatObject o){
        switch (Type.typeOf( o )){
            case GROUP:
                return new GroupType();
            case FOLDER:
                return FileType.DIRECTORY;
            case DATASET:
                return FileType.FILE;
        }
        return FileType.FILE;
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
        return FileTime.fromMillis( System.currentTimeMillis() );
    }

    @Override
    public FileTime lastAccessTime(){
        // TODO: Fix Times
        return FileTime.fromMillis( System.currentTimeMillis() );
    }

    @Override
    public FileTime creationTime(){
        // TODO: Fix Times
        return FileTime.fromMillis( System.currentTimeMillis() );
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
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    @Override
    public Long fileKey(){
        return object.getPk();
    }
    
    public DatacatObject getObject(){
        return this.object;
    }

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime,
            FileTime createTime) throws IOException{
        throw new UnsupportedOperationException( "Not supported yet." );
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
            getAttributeView(SubdirectoryView.class).link( child );
        }
        getAttributeView(ContainerViewProvider.class).clearStats();
    }

    @Override
    public String toString(){
        return "DcFile{" + "object=" + object.toString() + '}';
    }
}
