
package org.srs.datacat.vfs;

import java.net.URI;
import java.net.URISyntaxException;
import org.srs.datacat.model.DatasetView;
import org.srs.vfs.AbstractPath;

/**
 *
 * @author bvan
 */
public class DcPath extends AbstractPath<DcPath> {
    private DatasetView view;
    
    public DcPath(String userName, DcFileSystem fs, String path){
        super(userName, fs, path);
    }

    @Override
    public DcFileSystem getFileSystem(){
        return (DcFileSystem) super.getFileSystem();
    }
    
    public DatasetView getView(){
        return view;
    }
    
    public void setView(DatasetView view){
        this.view = view;
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
