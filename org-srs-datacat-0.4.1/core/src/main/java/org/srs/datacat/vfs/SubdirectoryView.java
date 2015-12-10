
package org.srs.datacat.vfs;

import com.google.common.base.Optional;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import org.srs.datacat.model.DatasetView;

/**
 * Children view for subdirectories only.
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
        try(DirectoryStream<Path> stream = 
                provider.unCachedDirectoryStream(getPath(), 
                        DcFileSystemProvider.ACCEPT_ALL_FILTER, Optional.<DatasetView>absent(), false)){
            for(Path child: stream){
                children.put( child.getFileName().toString(), child );
            }
        }
    }

}
