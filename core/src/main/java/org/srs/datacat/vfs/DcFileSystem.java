
package org.srs.datacat.vfs;

import java.net.URI;
import org.srs.vfs.AbstractFs;
import org.srs.vfs.PathProvider;
import org.srs.datacat.security.DcUserLookupService;

/**
 *
 * @author bvan
 */
public class DcFileSystem extends AbstractFs<DcPath> {
        
    private final DcUserLookupService lookupService;
    private final PathProvider<DcPath> pathProvider = new PathProvider<DcPath>(){

        @Override
        public DcPath getRoot(){
            return new DcPath(null, DcFileSystem.this, "/");
        }

        @Override
        public DcPath getPath(URI uri){
            return new DcPath(uri.getUserInfo(), DcFileSystem.this, uri.getPath());
        }

        @Override
        public DcPath getPath(String userName, String path){
            return new DcPath(userName, DcFileSystem.this, path);
        }
    };

    public DcFileSystem(DcFileSystemProvider provider, DcUserLookupService lookupService){
        super(provider);
        this.lookupService = lookupService;
    }

    @Override
    public PathProvider<DcPath> getPathProvider(){
        return pathProvider;
    }
    
    @Override
    public DcUserLookupService getUserPrincipalLookupService(){
        return lookupService;
    }

    @Override
    public DcFileSystemProvider provider(){
        return (DcFileSystemProvider) super.provider();
    }

}
