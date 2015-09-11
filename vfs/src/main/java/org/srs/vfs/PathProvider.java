package org.srs.vfs;

import java.net.URI;

/**
 *
 * @author bvan
 */
public class PathProvider<T extends AbstractPath> {

    public AbstractPath getRoot(){
        return new AbstractPath(this, "/");
    }

    public AbstractPath getPath(URI uri){
        return new AbstractPath(this, uri.getPath());
    }

    public AbstractPath getPath(String path){
        return new AbstractPath(this, path);
    }

    public AbstractPath getPath(String path, String... more){
        if(more.length != 0){
            StringBuilder sb = new StringBuilder();
            sb.append(path);
            for(String segment: more){
                if(segment.length() > 0){
                    if(sb.length() > 0) {
                        sb.append('/');
                    }
                    sb.append(segment);
                }
            }
            path = sb.toString();
        }
        return getPath(path);
    }

}
