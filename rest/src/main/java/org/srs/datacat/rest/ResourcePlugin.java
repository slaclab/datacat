
package org.srs.datacat.rest;

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
