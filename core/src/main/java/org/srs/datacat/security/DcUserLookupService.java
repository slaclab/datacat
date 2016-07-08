
package org.srs.datacat.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.srs.datacat.model.security.DcGroup;
import org.srs.datacat.model.security.DcUser;

/**
 * Basic lookup service.
 * @author bvan
 */
public class DcUserLookupService {

    public DcUser lookupPrincipalByName(String name) throws IOException{
        if(name == null){
            return new DcUser("$PUBLIC$");
        }
        return new DcUser(name);
    }
    
    public Set<DcGroup> lookupGroupsForUser(DcUser member) throws IOException{
        return new HashSet<>(Arrays.asList(DcGroup.PUBLIC_GROUP));
    }
    
}
