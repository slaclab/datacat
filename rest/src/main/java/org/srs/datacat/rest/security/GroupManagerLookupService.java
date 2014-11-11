
package org.srs.datacat.rest.security;

import java.util.Set;
import org.srs.datacat.security.DcGroup;
import org.srs.datacat.security.DcUser;
import org.srs.datacat.security.DcUserLookupService;

/**
 *
 * @author bvan
 */
public class GroupManagerLookupService extends DcUserLookupService {
    


    @Override
    public Set<DcGroup> lookupGroupsForUser(DcUser member){
        Set<DcGroup> userGroups = super.lookupGroupsForUser(member);
        
        return userGroups;
    }
    
}
