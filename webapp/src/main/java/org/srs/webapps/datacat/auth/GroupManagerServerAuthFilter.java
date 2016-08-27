
package org.srs.webapps.datacat.auth;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;

import org.srs.datacat.model.security.DcUser;
import org.srs.groupmanager.client.RestClient;
import org.srs.groupmanager.model.GroupManagerException;
import org.srs.groupmanager.model.UserModel;

/**
 *
 * @author bvan
 */
@Priority(Priorities.AUTHENTICATION)
public class GroupManagerServerAuthFilter implements ContainerRequestFilter {

    @Inject @Context HttpServletRequest req;
    
    private static LoadingCache<String, DcUser> usersCache;
    
    {
        usersCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build(
                        new CacheLoader<String, DcUser>() {
                            @Override
                            public DcUser load(String name) throws IOException, GroupManagerException{
                                final RestClient rc = new RestClient("http://srs.slac.stanford.edu/GroupManager/rest");
                                UserModel user = rc.findUserByAccount(name, Optional.<String>absent());
                                return new DcUser(Integer.toString(user.getId()));
                            }
                        });
    }
    
    @Override
    public void filter(ContainerRequestContext request) throws IOException{
        String name = (String) req.getSession().getAttribute("userName");
        try {
            if(name == null || name.isEmpty()){
                return;
            }
            GroupManagerSecurityContext sc = new GroupManagerSecurityContext(usersCache.get(name), "none");
            request.setSecurityContext(sc);
        } catch(ExecutionException ex) {
            throw new IOException("Error reading user id", ex.getCause());
        }
    }

}
