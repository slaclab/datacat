
package org.srs.datacat.rest.security;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.IOException;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;
import org.srs.datacat.vfs.attribute.DcAclFileAttributeView;

/**
 *
 * @author bvan
 */
@JsonPropertyOrder({"owner","acl"})
public class OwnerAclProxy {

    private final UserPrincipal owner;
    private List<AclEntry> acl;
    
    public OwnerAclProxy(DcAclFileAttributeView attr) throws IOException{
        this.owner = attr.getOwner();
        this.acl = attr.getAcl();
    }
    
    public String getOwner(){
        return owner != null ? owner.getName() : "";
    }
    
    public List<AclEntryProxy> getAcl(){
        List<AclEntryProxy> ret = new ArrayList<>();
        for(AclEntry e: acl){
            ret.add(new AclEntryProxy(e));
        }
        return ret;
    }
    
    @Override
    public String toString(){
        return String.format("owner=%s, acl=%s", getOwner(), getAcl().toString());
    }

}
