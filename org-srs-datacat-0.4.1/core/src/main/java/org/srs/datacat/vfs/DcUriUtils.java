
package org.srs.datacat.vfs;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Static class to help out with composing URI. 
 * TODO: Should probably be phased out.
 * 
 * @author bvan
 */
public final class DcUriUtils {
    
    private DcUriUtils(){}
        
    public static URI toFsUri(String path){
        try {
            return new URI("dc", null, null, -1, path, null, null);
        } catch(URISyntaxException ex) {
            throw new UriException("Path segnment likely incorrect", ex);
        }
    }
    
    /**
     * Convenience exception.
     */
    public static class UriException extends RuntimeException {
        public UriException(String message, Throwable cause){
            super(message, cause);
        }
    }

}
