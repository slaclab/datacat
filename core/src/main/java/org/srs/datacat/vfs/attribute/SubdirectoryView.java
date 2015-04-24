
package org.srs.datacat.vfs.attribute;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import org.srs.datacat.vfs.DcFileSystemProvider;
import org.srs.datacat.vfs.DcPath;

/**
 *
 * @author bvan
 */
public class SubdirectoryView extends ChildrenView {
    DcFileSystemProvider provider;

    public SubdirectoryView(Path path, DcFileSystemProvider provider){
        super(path, provider);
        this.provider = provider;
    }

    @Override
    public String name(){
        return "subdirectories";
    }

    @Override
    protected void doRefreshCache() throws IOException{
        try(DirectoryStream<DcPath> stream = 
                provider.unCachedDirectoryStream(getPath(), DcFileSystemProvider.ACCEPT_ALL_FILTER, null, false)){
            for(DcPath child: stream){
                children.put( child.getFileName().toString(), child );
            }
        }
    }

}
