package org.srs.webapps.datacat.auth;

import java.io.IOException;
import java.security.Principal;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import org.srs.datacat.model.security.DcUser;

/**
 *
 * @author bvan
 */
@Priority(Priorities.AUTHENTICATION)
public class SimpleAuthFilter implements ContainerRequestFilter {

    private final String userHeaderName;
    @Inject
    @Context
    HttpServletRequest req;

    public SimpleAuthFilter(String userHeaderName){
        this.userHeaderName = userHeaderName;
    }

    @Override
    public void filter(ContainerRequestContext request) throws IOException{
        final String userName = req.getHeader(userHeaderName);
        if(userName == null || userName.isEmpty()){
            return;
        }
        
        request.setSecurityContext(new SecurityContext() {

            @Override
            public Principal getUserPrincipal(){
                return new DcUser(userName);
            }

            @Override
            public boolean isUserInRole(String string){
                throw new java.lang.UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public boolean isSecure(){
                return false;
            }

            @Override
            public String getAuthenticationScheme(){
                return "simple";
            }
        });
    }

}
