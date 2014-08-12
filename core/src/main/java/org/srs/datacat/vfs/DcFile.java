
package org.srs.datacat.vfs;


import java.io.IOException;
import static org.srs.vfs.VirtualFile.FileType;
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
import org.srs.datacat.vfs.security.DcAclFileAttributeView;


/**
 *
 * @author bvan
 */
public class DcFile extends AbstractVirtualFile<DcPath, Long> implements BasicFileAttributeView {
    
    {
        addViewName( DcAclFileAttributeView.class, "acl");
        addViewName( DcFile.class, "basic");
    }
    
    private DatacatObject object;
    
    public DcFile(DcPath path, DatacatObject object){
        super(path, packedType(object));
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
            addAttributeViews(new ContainerViewProvider(this));
        }
    }
    
    protected static int packedType(DatacatObject o){
        Type fileType = Type.typeOf( o );
        int ft = fileType.ordinal();
        switch (Type.typeOf( o )){
            case GROUP:
            case FOLDER:
                ft |= FileType.CONTAINER;
                break;
            case DATASET:
                ft |= FileType.FILE;
        }
        return ft;
    }

    public DcFile(DcPath path, int type, Collection<? extends AttributeView> views){
        super( path, type, views);
    }

    @Override
    public DcPath getPath(){
        return (DcPath) super.getPath();
    }
       
    public Type getDatacatType(){
        return Type.typeOf( object );
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
        return getDatacatType() == Type.DATASET;
    }

    @Override
    public boolean isDirectory(){
        return getDatacatType().isContainer();
    }

    @Override
    public boolean isSymbolicLink(){
        return false;
    }

    @Override
    public boolean isOther(){
        return getDatacatType() == Type.GROUP;
    }

    @Override
    public long size(){
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    @Override
    public Long fileKey(){
        return object.getPk();
    }
    
    public DatacatObject getDatacatObject(){
        return this.object;
    }

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime,
            FileTime createTime) throws IOException{
        throw new UnsupportedOperationException( "Not supported yet." );
    }
    
}
