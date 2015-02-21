
package org.srs.datacat.rest.security;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.nio.file.attribute.AclEntry;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.srs.datacat.security.DcPermissions;

/**
 *
 * @author bvan
 */
@JsonPropertyOrder({"subject","permissions"})
public class AclEntryProxy {
    private AclEntry entry;

    public AclEntryProxy(AclEntry entry){
        this.entry = entry;
    }

    public String getSubject(){
        return entry.principal().toString();
    }

    public HashMap<String, Boolean> getPermissions(){
        LinkedHashMap<String, Boolean> permissions = new LinkedHashMap<>();
        permissions.put( "read", entry.permissions().contains( DcPermissions.READ ) );
        permissions.put( "insert", entry.permissions().contains( DcPermissions.INSERT ) );
        permissions.put( "delete", entry.permissions().contains( DcPermissions.DELETE ) );
        permissions.put( "write", entry.permissions().contains( DcPermissions.WRITE ) );
        permissions.put( "admin", entry.permissions().contains( DcPermissions.ADMIN ) );
        return permissions;
    }

    @Override
    public String toString(){
        return String.format( "subject:%s, permissions:%s", entry.principal().toString(), 
                getPermissions().toString());
    }

}
