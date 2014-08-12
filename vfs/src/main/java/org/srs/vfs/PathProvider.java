
package org.srs.vfs;

import java.net.URI;

/**
 *
 * @author bvan
 */
public abstract class PathProvider<T extends AbstractPath> {

    public abstract T getRoot();
    public abstract T getPath(URI uri);
    public abstract T getPath(String userName, String path);
    public T getPath(String userName, String path, String... more){
        if (more.length != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(path);
            for (String segment: more) {
                if (segment.length() > 0) {
                    if (sb.length() > 0)
                        sb.append('/');
                    sb.append(segment);
                }
            }
            path = sb.toString();
        }
        return getPath(userName, path);
    }

}
