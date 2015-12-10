
package org.srs.datacat.rest.security;

import java.io.IOException;
import java.util.Set;
import org.srs.datacat.model.security.DcGroup;
import org.srs.datacat.model.security.DcUser;
import org.srs.datacat.security.DcUserLookupService;

/**
 *
 * @author bvan
 */
public class GroupManagerLookupService extends DcUserLookupService {
    


    @Override
    public Set<DcGroup> lookupGroupsForUser(DcUser member) throws IOException{
        Set<DcGroup> userGroups = super.lookupGroupsForUser(member);
        
        return userGroups;
    }
    
}
