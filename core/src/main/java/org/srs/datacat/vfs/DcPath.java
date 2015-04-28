
package org.srs.datacat.vfs;

import java.net.URI;
import java.net.URISyntaxException;
import org.srs.vfs.AbstractFs;
import org.srs.vfs.AbstractPath;
import org.srs.vfs.PathProvider;

/**
 *
 * @author bvan
 */
public class DcPath extends AbstractPath<DcPath> {
    
    protected DcPath(PathProvider<DcPath> pathProvider, String path){
        super(pathProvider, path);
    }

    @Override
    public AbstractFs getFileSystem(){
        return null;
    }
    
    @Override
    public URI toUri() {
        String scheme = "dc";
        String userInfo = null;
        String host = null;
        int port = -1;
        String query = null;
        String fragment = null;
        
        try {
            return new URI(scheme, userInfo, host, port, path, query, fragment);
        } catch(URISyntaxException ex) {
            return null;
        }
    }
}
