package org.srs.vfs;

import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.AttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class representing all attributes and their views for a given file.
 * @author bvan
 */
public class FileAttributes {

    private final HashMap<String, AttributeView> attributeViews = new HashMap<>();
    private final HashMap<Class<? extends AttributeView>, String> attrNames = new HashMap<>();

    public FileAttributes(){
        attrNames.put(BasicFileAttributeView.class, "basic");
        attrNames.put(AclFileAttributeView.class, "acl");
    }
    
    public Map<String, AttributeView> getAttributeViews(){
        return Collections.unmodifiableMap(attributeViews);
    }
    
    public AttributeView getAttributeView(String name){
        return attributeViews.get(name);
    }
    
    public <T extends AttributeView> T getAttributeView(Class<T> view){
        return (T) getAttributeViews().get(getViewName(view));
    }
    
    public void putAttributeViews(AttributeView... views){
        synchronized(attributeViews){
            for(AttributeView view: views){
                attributeViews.put( view.name(), view );
                if(!attrNames.containsKey( view.getClass())){
                    attrNames.put( view.getClass(), view.name());
                }
            }
        }
    }
    
    public void addAttributeViews(Collection<? extends AttributeView> views){
        putAttributeViews( views.toArray( new AttributeView[0] ));
    }

    public void clearAttributeViews(){
        synchronized(attributeViews){
            attributeViews.clear();
        }
    }
    
    protected String getViewName(Class<? extends AttributeView> view){
        return attrNames.get( view );
    }

}
