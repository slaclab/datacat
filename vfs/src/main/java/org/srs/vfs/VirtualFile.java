
package org.srs.vfs;

import java.nio.file.attribute.AttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author bvan
 */
public interface VirtualFile extends BasicFileAttributes {
    
    public AbstractPath getPath();
    public int getType();
    public void setType(int type);
    
    public Map<String, AttributeView> getAttributeViews();
    public <T extends AttributeView> T getAttributeView(Class<T> view);
    public Collection<? extends AttributeView> getAttributeViews(Class<? extends AttributeView>... views);
    public void addAttributeViews(AttributeView... views);
    public void addAttributeViews(Collection<? extends AttributeView> views);
    public void clearAttributeViews();
    
    // Type is stored in the high bits, allowing the user to define 
    // bits (or an enum for that matter) and store it in the bottom.
    public static class FileType {
        
        public static final int FILE = 1<<20;        // Files
        public static final int CONTAINER = 1<<21;   // Includes directories
        public static final int LINK = 1<<22;        // Links
        public static final int VIRTUAL = 1<<23;     // Files which may not exist
        public static final int COMPOSITE = 1<<24;

        public static final int USERMASK = 0x0000FFFF;
        
        public static boolean isFile(int type){
            return check(type, FILE);
        }
        
        public static boolean isContainer(int type){
            return check(type, CONTAINER);
        }
        
        public static boolean isLink(int type){
            return check(type, LINK);
        }
        
        public static boolean isVirtual(int type){
            return check(type, VIRTUAL);
        }
        
        public static boolean isComposite(int type){
            return check(type, COMPOSITE);
        }
        
        public static int userType(int type){
            return type & USERMASK;
        }
        
        private static boolean check(int actual, int expected){
            return (actual & expected) != 0;
        }
    }
}
