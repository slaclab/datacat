
package org.srs.datacat.model.security;

import java.util.Set;

/**
 * A CallContext object is used for verification of filesystem operations.
 * It includes the user who initiated the call into the provider, and the user's groups.
 * @author bvan
 */
public class CallContext {
    
    private final DcUser subject;
    private final Set<DcGroup> groups;

    public CallContext(DcUser subject, Set<DcGroup> groups){
        this.subject = subject;
        this.groups = groups;
    }

    public DcUser getSubject(){
        return subject;
    }

    public Set<DcGroup> getGroups(){
        return groups;
    }

}
