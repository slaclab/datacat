
package org.srs.vfs;

import java.io.IOException;

import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.AttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author bvan
 * @param <P> AbstractPath type
 * @param <F> fileKey type
 */
public abstract class AbstractVirtualFile<P extends AbstractPath, F> implements VirtualFile {
    private final P path;
    private int type;
    private final HashMap<String, AttributeView> attributeViews = new HashMap<>();
    private final HashMap<Class<? extends AttributeView>, String> attrNames = new HashMap<>();
    
    {
        attrNames.put( BasicFileAttributeView.class, "basic");
        attrNames.put( AclFileAttributeView.class, "acl");
        attrNames.put( ChildrenView.class, "children");
    }
    
    public AbstractVirtualFile(P path, int type){
        this.path = path;
        this.type = type;
    }
    
    public AbstractVirtualFile(P path, int type, 
            Collection<? extends AttributeView> views){
        this.path = path;
        this.type = type;
        for(AttributeView view: views){
            attributeViews.put( view.name(), view);
        }
    }
    
    public String name(){
        return "basic";
    }

    public BasicFileAttributes readAttributes() throws IOException{
        return this;
    }

    @Override
    public P getPath(){
        return path;
    }

    @Override
    public Map<String, AttributeView> getAttributeViews(){
        return Collections.unmodifiableMap(attributeViews);
    }

    @Override
    public int getType(){
        return type;
    }
    
    @Override
    public void setType(int type){
        this.type = type;
    }
    
    @Override
    public void addAttributeViews(AttributeView... views){
        synchronized(attributeViews){
            for(AttributeView view: views){
                attributeViews.put( view.name(), view );
                if(!attrNames.containsKey( view.getClass())){
                    attrNames.put( view.getClass(), view.name());
                }
            }
        }
    }
    
    @Override
    public void addAttributeViews(Collection<? extends AttributeView> views){
        addAttributeViews( views.toArray( new AttributeView[0] ));
    }

    @Override
    public void clearAttributeViews(){
        synchronized(attributeViews){
            attributeViews.clear();
        }
    }

    protected void addViewName(Class<? extends AttributeView> view, String viewName){
        attrNames.put( view, viewName );
    }
    
    protected String getViewName(Class<? extends AttributeView> view){
        return attrNames.get( view );
    }
    
    @Override
    public abstract F fileKey();

}