package org.srs.webapps.datacat.auth.tomcat;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.catalina.Role;
import org.apache.catalina.UserDatabase;
import org.srs.datacat.model.security.DcGroup;
import org.srs.datacat.model.security.DcUser;
import org.srs.datacat.security.DcUserLookupService;

/**
 * If the web application is deployed to Tomcat, piggy back off of Tomcat's UserDatabase. This
 * Feature assumes the UserDatabase is mapped via a ResourceLink to the name "users".
 *
 * We don't need to have any sort of Jersey Filters involved like we do with the Group Manager
 * because Tomcat handles the Authentication and sets the Principal.
 *
 * @author bvan
 */
public class TomcatUserAuthProvider extends DcUserLookupService {
    private UserDatabase userDb;

    public TomcatUserAuthProvider() throws NamingException{
        javax.naming.Context ctx = new InitialContext();
        // relative to standard JNDI root for J2EE app
        javax.naming.Context envCtx = (javax.naming.Context) ctx.lookup("java:comp/env");
        this.userDb = (UserDatabase) envCtx.lookup("users");
    }

    @Override
    public DcUser lookupPrincipalByName(String name) throws IOException{
        if(userDb.findUser(name) != null){
            return new DcUser(name);
        }
        return new DcUser("$PUBLIC$");
    }

    @Override
    public Set<DcGroup> lookupGroupsForUser(DcUser member) throws IOException{
        HashSet<DcGroup> userGroups = new HashSet<>(Arrays.asList(DcGroup.PUBLIC_GROUP));
        String userName = member.getName();
        if(userDb.findUser(userName) != null){
            for(Iterator<Role> iter = userDb.findUser(userName).getRoles(); iter.hasNext();){
                userGroups.add(new DcGroup(iter.next().getRolename(), null));
            }
        }
        return userGroups;
    }

}
