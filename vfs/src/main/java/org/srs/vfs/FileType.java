
package org.srs.vfs;

/**
 *
 * @author bvan
 */
public interface FileType {
       
    public static class Directory implements FileType {}
    public static class File implements FileType {}
    public static class Virtual implements FileType {}
    public static class Link implements FileType {}
    public static class Union implements FileType {}
    
    public static final Directory DIRECTORY = new Directory();
    public static final File FILE = new File();
    public static final Virtual VIRTUAL = new Virtual();
    public static final Link LINK = new Link();
    public static final Union UNION = new Union();
    
}