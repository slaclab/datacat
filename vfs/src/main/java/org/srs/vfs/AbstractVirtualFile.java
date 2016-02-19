
package org.srs.vfs;

import java.nio.file.Path;

import java.nio.file.attribute.AttributeView;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.Collection;

/**
 *
 * @author bvan
 * @param <P> AbstractPath type
 * @param <F> fileKey type
 */
public class AbstractVirtualFile<P extends Path, F> implements VirtualFile {
    private final P path;
    private final FileType type;
    private final FileAttributes attributes = new FileAttributes();
        
    public AbstractVirtualFile(P path, FileType type){
        this.path = path;
        this.type = type;
    }
    
    public AbstractVirtualFile(P path, FileType type, 
            Collection<? extends AttributeView> views){
        this.path = path;
        this.type = type;
        attributes.addAttributeViews(views);
    }
    
    @Override
    public P getPath(){
        return path;
    }  

    @Override
    public FileAttributes getAttributes(){
        return this.attributes;
    }

    @Override
    public FileType getType(){
        return type;
    }
    
}
