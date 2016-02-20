package org.srs.datacat.vfs;

import java.nio.file.Path;

import java.nio.file.attribute.AttributeView;
import java.nio.file.attribute.FileTime;
import java.util.List;

import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.RecordType;
import org.srs.datacat.model.security.DcAclEntry;
import org.srs.datacat.vfs.attribute.ContainerViewProvider;
import org.srs.datacat.vfs.attribute.DatasetViewProvider;
import org.srs.vfs.AbstractVirtualFile;
import org.srs.vfs.FileAttributes;
import org.srs.vfs.FileType;

/**
 * A wrapper class that represents a materialized DatacatObject with some common file-like mappings.
 * It's used for caching, and it can contain any number of views.
 * 
 * @author bvan
 */
public class DcFile extends AbstractVirtualFile<Path, Long> {

    /**
     * Marker for alternative class of FileType.Directory.
     */
    public static class GroupType extends FileType.Directory {}

    private final DatacatNode dcObject;
    private final List<DcAclEntry> acl;
    private final long dcObjectCreation = System.currentTimeMillis();
    private final DcFileSystemProvider provider;

    public DcFile(Path path, DcFileSystemProvider provider, DatacatNode object, List<DcAclEntry> acl){
        super(path, fileType(object));
        this.provider = provider;
        this.dcObject = object;
        this.acl = acl;
        initViews(object, getAttributes());
    }

    private void initViews(DatacatNode orig, FileAttributes attributes){
        if(isRegularFile() && orig instanceof DatasetModel){
            attributes.putAttributeViews(new DatasetViewProvider(provider, (DatasetModel) orig));
        }
        if(isDirectory()){
            attributes.putAttributeViews(new ChildrenView(getPath(), provider));
            attributes.putAttributeViews(new SubdirectoryView(getPath(), provider));
            attributes.putAttributeViews(new ContainerViewProvider((DatasetContainer) orig, provider));
        }
    }

    protected static FileType fileType(DatacatNode o){
        switch(RecordType.typeOf(o)){
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

    protected List<DcAclEntry> getAcl(){
        return acl;
    }

    @Override
    public Path getPath(){
        return super.getPath();
    }

    public <T extends AttributeView> T getAttributeView(Class<T> view){
        return getAttributes().getAttributeView(view);
    }
    
    public FileTime creationTime(){
        // TODO: Fix Times
        return FileTime.fromMillis(dcObjectCreation);
    }

    public FileTime lastModifiedTime(){
        // TODO: Fix Times
        return FileTime.fromMillis(dcObjectCreation);
    }

    public boolean isRegularFile(){
        return getType() instanceof FileType.File;
    }

    public boolean isDirectory(){
        return getType() instanceof FileType.Directory;
    }

    public DatacatNode getObject(){
        return this.dcObject;
    }

    @Override
    public String toString(){
        return "DcFile{" + "object=" + dcObject.toString() + '}';
    }
    
}
