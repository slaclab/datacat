
package org.srs.webapps.datacat.auth;

import javax.ws.rs.core.SecurityContext;
import org.srs.datacat.model.security.DcUser;

/**
 *
 * @author bvan
 */
public class GroupManagerSecurityContext implements SecurityContext {
    private DcUser user;
    private String authScheme;
    
    public GroupManagerSecurityContext(DcUser user, String authScheme){
        this.user = user;
        this.authScheme = authScheme;
    }

    @Override
    public DcUser getUserPrincipal(){
        return user;
    }

    @Override
    public boolean isUserInRole(String role){
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isSecure(){
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getAuthenticationScheme(){
        return authScheme;
    }

}
