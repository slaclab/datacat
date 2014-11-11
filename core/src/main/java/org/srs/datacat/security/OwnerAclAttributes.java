
package org.srs.datacat.security;

import java.nio.file.attribute.AclEntry;
import java.util.List;

/**
 *
 * @author bvan
 */
public class OwnerAclAttributes {
    private DcUser owner;
    private List<AclEntry> acl;
    
    public OwnerAclAttributes(){ }

    public OwnerAclAttributes(DcUser owner, List<AclEntry> acl){
        this.owner = owner;
        this.acl = acl;
    }

    public DcUser getOwner(){
        return owner;
    }

    public List<AclEntry> getAcl(){
        return acl;
    }

    @Override
    public String toString(){
        return "OwnerAclAttributes{" + "owner=" + owner + ", acl=" + acl + '}';
    }

}
