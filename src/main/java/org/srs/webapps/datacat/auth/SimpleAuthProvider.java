package org.srs.webapps.datacat.auth;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import org.srs.datacat.model.security.DcGroup;
import org.srs.datacat.model.security.DcUser;
import org.srs.datacat.security.DcUserLookupService;

/**
 *
 * @author bvan
 */
public class SimpleAuthProvider extends DcUserLookupService {
    private Map<String, Set<String>> userGroupMapping;

    public static class SimpleAuthFeature implements Feature {

        @Override
        public boolean configure(FeatureContext context){
            context.register(SimpleAuthFilter.class);
            context.register(SimpleAuthProvider.class);
            return true;
        }

    }

    public SimpleAuthProvider(Map<String, Set<String>> userGroupMapping){
        this.userGroupMapping = userGroupMapping;
    }

    @Override
    public DcUser lookupPrincipalByName(String name) throws IOException{
        if(userGroupMapping.containsKey(name)){
            return new DcUser(name);
        }
        return new DcUser("$PUBLIC$");
    }

    @Override
    public Set<DcGroup> lookupGroupsForUser(DcUser member) throws IOException{
        HashSet<DcGroup> userGroups = new HashSet<>(Arrays.asList(DcGroup.PUBLIC_GROUP));
        String userName = member.getName();
        if(userGroupMapping.containsKey(userName)){
            for(String groupName: userGroupMapping.get(userName)){
                userGroups.add(new DcGroup(groupName, null));
            }
        }
        return userGroups;
    }

}
