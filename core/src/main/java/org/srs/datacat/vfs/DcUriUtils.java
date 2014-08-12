
package org.srs.datacat.vfs;

import java.net.URI;
import java.net.URISyntaxException;

/**
 *
 * @author bvan
 */
public class DcUriUtils {
    
    public static URI toFsUri(String path, String user, String experiment){
        String authority = null;
        if(user != null){
             authority = user + ":" + experiment;
        }
        try {
            return new URI("dc", authority, path, null, null);
        } catch(URISyntaxException ex) {
            throw new UriException("Path segnment likely incorrect", ex);
        }
    }
    
    public static class UriException extends RuntimeException {
        public UriException(String message, Throwable cause){
            super(message, cause);
        }
    }

}
