
package org.srs.datacat.rest.security;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.srs.datacat.model.security.DcAclEntry;
import org.srs.datacat.model.security.DcPermissions;

/**
 *
 * @author bvan
 */
@JsonPropertyOrder({"subject","permissions"})
public class AclEntryProxy {
    private DcAclEntry entry;

    public AclEntryProxy(DcAclEntry entry){
        this.entry = entry;
    }

    public String getSubject(){
        return entry.getSubject().toString();
    }

    public String getPermissions(){
        return DcPermissions.pack(entry.getPermissions());
    }

    @Override
    public String toString(){
        return String.format( "subject:%s, permissions:%s", getSubject() ,getPermissions());
    }

}
