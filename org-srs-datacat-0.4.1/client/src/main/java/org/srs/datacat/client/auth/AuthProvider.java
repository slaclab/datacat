
package org.srs.datacat.client.auth;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import javax.ws.rs.client.ClientRequestFilter;

/**
 *
 * @author bvan
 */
public class AuthProvider {

    protected AuthProvider(){
        
    }
    
    public static ClientRequestFilter fromConfig(Map<String, String> config) throws URISyntaxException{
        String authType = config.get("authType");
        if(authType != null){
            if("HMACAuthSRS".equals(authType)){
                return getAuthSRS(config);
            } else if("HMACAuth".equals(authType)){
                return getAuthFilter(config);
            }
        }
        return null;        
    }
        
    private static HmacAuthFilter getAuthFilter(Map<String, String> config) throws URISyntaxException{
        String url = config.get("url");
        String keyId = Objects.requireNonNull(config.get("authKeyId"), "Must have a valid key id");
        String secretKey = 
                Objects.requireNonNull(config.get("authSecretKey"), "Must have a valid secret key");
        String headerName = 
                Objects.requireNonNull(config.get("authHeaderName"), "Need an authorization header name");
        String signatureFormat = 
                Objects.requireNonNull(config.get("authSignatureFormat"), "Need a valid signature format");
        return new HmacAuthFilter(keyId, secretKey, headerName, signatureFormat, new URI(url));
    }
    
    private static HmacAuthSRS getAuthSRS(Map<String, String> config) throws URISyntaxException{
        String url = config.get("url");
        String keyId = Objects.requireNonNull(config.get("authKeyId"), "Must have a valid key id");
        String secretKey = Objects.requireNonNull(config.get("authSecretKey"), "Must have a valid secret key");
        return new HmacAuthSRS(keyId, secretKey, new URI(url));
    }
}
