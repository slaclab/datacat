
package org.srs.vfs;

import java.nio.file.attribute.AttributeView;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author bvan
 */
public interface VirtualFile  {
    
    public AbstractPath getPath();
    public FileType getType();
    
    public Map<String, AttributeView> getAttributeViews();
    public <T extends AttributeView> T getAttributeView(Class<T> view);
    public Collection<? extends AttributeView> getAttributeViews(Class<? extends AttributeView>... views); // TODO: Maybe remove
    public void addAttributeViews(AttributeView... views);
    public void addAttributeViews(Collection<? extends AttributeView> views); // TODO: Maybe remove
    public void clearAttributeViews(); // TODO: Maybe remove
    
}