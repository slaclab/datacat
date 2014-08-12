
package org.srs.datacat.vfs.security;


import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.file.attribute.UserPrincipal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.srs.datacat.vfs.security.DcGroup.DcGroupPrincipal;

/**
 *
 * @author bvan
 */
public class DcUser implements UserPrincipal {
    
    private final String name;
    private final HashMap<String, Set<DcGroupPrincipal>> experimentGroups = new HashMap<>();
    
    private DcUser(String name){
        this.name = checkNotNull(name);
    }
    
    public DcUser(String name, HashMap<String, List<String>> experiments){
        this(name);
        checkNotNull(experiments);
        for(Entry<String, List<String>> e: experiments.entrySet()){
            HashSet<DcGroupPrincipal> principals = new HashSet<>();
            for(String groupName: e.getValue()){
                principals.add(new DcGroupPrincipal(groupName));
            }
            experimentGroups.put( e.getKey(), principals);
        }
    }

    @Override
    public String getName(){
        return name;
    }
    
    public Set<DcGroupPrincipal> getPrincipals(String experiment){
        return experimentGroups.get(experiment);
    }

}
