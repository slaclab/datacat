
package org.srs.datacat.vfs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import javax.sql.DataSource;
import org.srs.vfs.AbstractFs;
import org.srs.vfs.PathProvider;
import org.srs.datacat.vfs.security.DcGroup;

/**
 *
 * @author bvan
 */
public class DcFileSystem extends AbstractFs<DcPath> {
    private final DataSource dataSource;
        
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

    public DcFileSystem(DcFileSystemProvider provider, DataSource dataSource){
        super(provider);
        this.dataSource = dataSource;
    }

    public DataSource getDataSource(){
        return dataSource;
    }

    @Override
    public Class<DcPath> getPathClass(){
        return DcPath.class;
    }

    @Override
    public PathProvider<DcPath> getPathProvider(){
        return pathProvider;
    }
    
    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService(){
        return new UserPrincipalLookupService(){

            @Override
            public UserPrincipal lookupPrincipalByName(String name) throws IOException{
                if(name == null){
                    return DcGroup.PUBLIC_GROUP;
                }
                return null;
            }

            @Override
            public GroupPrincipal lookupPrincipalByGroupName(String group) throws IOException{
                throw new UnsupportedOperationException();
            }
            
        };
    }

    @Override
    public DcFileSystemProvider provider(){
        return (DcFileSystemProvider) super.provider();
    }

}
