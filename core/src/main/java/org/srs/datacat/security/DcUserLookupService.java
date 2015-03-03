
package org.srs.datacat.security;

import java.io.IOException;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.srs.datacat.model.security.DcGroup;
import org.srs.datacat.model.security.DcUser;

/**
 * Basic lookup service.
 * @author bvan
 */
public class DcUserLookupService extends UserPrincipalLookupService {

    @Override
    public DcUser lookupPrincipalByName(String name) throws IOException{
        if(name == null){
            return new DcUser("$PUBLIC$");
        }
        return new DcUser(name);
    }

    @Override
    public DcGroup lookupPrincipalByGroupName(String group) throws IOException{
        throw new UnsupportedOperationException("Querying groups not supported");
    }
    
    public Set<DcGroup> lookupGroupsForUser(DcUser member){
        return new HashSet<>(Arrays.asList(DcGroup.PUBLIC_GROUP));
    }
    
}
