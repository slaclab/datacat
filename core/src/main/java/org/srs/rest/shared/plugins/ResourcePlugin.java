
package org.srs.rest.shared.plugins;

import javax.ws.rs.Path;

/**
 *
 * @author bvan
 */
public abstract class ResourcePlugin {
    
    public String getPath(){
        return getClass().getAnnotation( Path.class ).value();
    }

}
