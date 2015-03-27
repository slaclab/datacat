
package org.srs.datacat.vfs.attribute;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import org.srs.datacat.vfs.DcFileSystemProvider;
import org.srs.datacat.vfs.DcPath;

/**
 *
 * @author bvan
 */
public class SubdirectoryView extends ChildrenView {

    public SubdirectoryView(DcPath path){
        super( path );
    }

    @Override
    public String name(){
        return "subdirectories";
    }

    @Override
    protected void doRefreshCache() throws IOException{
        DcFileSystemProvider pro = getPath().getFileSystem().getProvider();
        try(DirectoryStream<DcPath> stream = 
                pro.directSubdirectoryStream(getPath(), DcFileSystemProvider.ACCEPT_ALL_FILTER)){
            for(DcPath child: stream){
                children.put( child.getFileName().toString(), child );
            }
        }
    }

}
