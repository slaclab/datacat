
package org.srs.datacat.vfs;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;

/**
 * Static class to help out with composing URI. 
 * TODO: Should probably be phased out.
 * 
 * @author bvan
 */
public final class DcUriUtils {
    
    private DcUriUtils(){}
    
    public static URI toFsUri(String path, Principal principal, String experiment){
        String userName = principal != null ? principal.getName() : null;
        try {
            return new URI("dc", userName, experiment, -1, path, null, null);
        } catch(URISyntaxException ex) {
            throw new UriException("Path segnment likely incorrect", ex);
        }
    }
    
    public static URI toFsUri(String path, String user, String experiment){
        try {
            return new URI("dc", user, experiment, -1, path, null, null);
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
