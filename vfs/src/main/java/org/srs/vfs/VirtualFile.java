
package org.srs.vfs;

import java.nio.file.Path;

/**
 *
 * @author bvan
 */
public interface VirtualFile  {
    
    public Path getPath();
    public FileType getType();
    public FileAttributes getAttributes();
    
}