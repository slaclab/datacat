
package org.srs.webapps.datacat.auth;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.srs.datacat.model.security.DcGroup;
import org.srs.datacat.model.security.DcUser;
import org.srs.datacat.security.DcUserLookupService;
import org.srs.groupmanager.client.RestClient;
import org.srs.groupmanager.model.GroupManagerException;
import org.srs.groupmanager.model.GroupModel;
import org.srs.groupmanager.model.UserModel;

/**
 *
 * @author bvan
 */
public class GroupManagerAuthProvider implements DcUserLookupService {
    
    RestClient rc;
    private final LoadingCache<String, DcUser> usersCache;
    private final LoadingCache<DcUser, Set<DcGroup>> groupsCache;
    
    public GroupManagerAuthProvider(){
        rc = new RestClient("http://srs.slac.stanford.edu/GroupManager/rest");
        usersCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build(
                    new CacheLoader<String, DcUser>() {
                        @Override
                        public DcUser load(String name) throws IOException, GroupManagerException{
                            UserModel user = rc.getUser(Integer.parseInt(name));
                            return new DcUser(Integer.toString(user.getId()));
                        }
                    });
        
        groupsCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build(
                    new CacheLoader<DcUser, Set<DcGroup>>() {
                        @Override
                        public Set<DcGroup> load(DcUser member) throws IOException, GroupManagerException{
                            Set<DcGroup> userGroups = new HashSet<>();
                            String userIdString = member.getName();
                            Integer userId = Integer.parseInt(userIdString);
                            List<GroupModel> groups = rc.getUserGroups(userId, Optional.<String>absent(), Optional.<String>absent());
                            for(GroupModel group: groups){
                                userGroups.add(new DcGroup(group.getName() + "@" + group.getProject()));
                            }
                            return userGroups;
                        }
                    });
    }
    
    @Override
    public DcUser lookupPrincipalByName(String name) throws IOException{
        try {
            return name == null || name.isEmpty() ? usersCache.get(name) : null;
        } catch(ExecutionException ex) {
            throw new IOException("Error reading user id", ex.getCause());
        }
    }
    
    @Override
    public Set<DcGroup> lookupGroupsForUser(DcUser member) throws IOException{
        try {
            if(member == null){
                return new HashSet<>();
            }
            return groupsCache.get(member);
        } catch(ExecutionException ex) {
            throw new RuntimeException("unknown group manager exception", ex.getCause());
        }
    }

}
