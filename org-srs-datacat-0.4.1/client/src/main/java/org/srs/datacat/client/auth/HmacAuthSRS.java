
package org.srs.datacat.client.auth;

import java.net.URI;

/**
 *
 * @author bvan
 */
public class HmacAuthSRS extends HmacAuthFilter {

    public HmacAuthSRS(String keyId, String secretKey, URI url){
        super(keyId, secretKey, "Authorization", "SRS:{0}:{1}", url);
    }

}
