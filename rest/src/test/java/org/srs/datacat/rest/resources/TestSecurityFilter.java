
package org.srs.datacat.rest.resources;

import java.io.IOException;
import java.security.Principal;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.SecurityContext;
import org.srs.datacat.security.DcUser;

/**
 *
 * @author bvan
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class TestSecurityFilter implements ContainerRequestFilter{


    public TestSecurityFilter(){ }
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException{
        SecurityContext sc = null;
        final String userName = requestContext.getHeaderString("authentication");
        
        sc = new SecurityContext(){

            @Override
            public Principal getUserPrincipal(){
                if(userName == null){
                    return null;
                }
                return new DcUser(userName);
            }

            @Override
            public boolean isUserInRole(String role){
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isSecure(){
                throw new UnsupportedOperationException();
            }

            @Override
            public String getAuthenticationScheme(){
                throw new UnsupportedOperationException();
            }
        };
        requestContext.setSecurityContext(sc);
    }

}
