
package org.srs.datacat.vfs;

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
    
}
