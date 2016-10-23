
package org.srs.datacat.security;

import java.io.IOException;
import java.util.Set;
import org.srs.datacat.model.security.DcGroup;
import org.srs.datacat.model.security.DcUser;

/**
 * Basic lookup service.
 * @author bvan
 */
public interface DcUserLookupService {

    DcUser lookupPrincipalByName(String name) throws IOException;
    
    Set<DcGroup> lookupGroupsForUser(DcUser member) throws IOException;
    
}
