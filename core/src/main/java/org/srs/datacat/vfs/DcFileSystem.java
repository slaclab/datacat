
package org.srs.datacat.vfs;

import java.net.URI;
import java.nio.file.PathMatcher;
import org.srs.vfs.PathProvider;
import org.srs.vfs.PathMatchers;

/**
 *
 * @author bvan
 */
public class DcFileSystem {
        
    private final PathProvider<DcPath> pathProvider = new PathProvider<DcPath>(){

        @Override
        public DcPath getRoot(){
            return new DcPath(null, this, "/");
        }

        @Override
        public DcPath getPath(URI uri){
            return new DcPath(uri.getUserInfo(), this, uri.getPath());
        }

        @Override
        public DcPath getPath(String userName, String path){
            return new DcPath(userName, this, path);
        }
    };

    public DcFileSystem(){}

    public PathProvider<DcPath> getPathProvider(){
        return pathProvider;
    }
    
    public PathMatcher getPathMatcher(String syntaxAndPattern){
        return PathMatchers.getPathMatcher(syntaxAndPattern, "/");
    }

}
