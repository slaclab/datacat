package org.srs.datacat.vfs.attribute;

import java.io.IOException;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.List;
import org.srs.datacat.security.DcUser;
import org.srs.datacat.security.OwnerAclAttributes;

/**
 *
 * @author bvan
 */
public class DcAclFileAttributeView implements AclFileAttributeView {
    
    List<AclEntry> acl;
    DcUser owner;
    
    public DcAclFileAttributeView(OwnerAclAttributes attributes){
        this.owner = attributes.getOwner();
        this.acl = attributes.getAcl();
    }
    
    public DcAclFileAttributeView(DcUser owner, List<AclEntry> acl){
        this.acl = acl;
        this.owner = owner;
    }

    @Override
    public String name(){
        return "acl";
    }

    @Override
    public List<AclEntry> getAcl() throws IOException{
        return acl;
    }

    @Override
    public void setAcl(List<AclEntry> acl) throws IOException{
        this.acl = acl;
    }

    @Override
    public UserPrincipal getOwner() throws IOException{
        return owner;
    }

    @Override
    public void setOwner(UserPrincipal owner) throws IOException{
        throw new UnsupportedOperationException( "Owners not supported" );
    }

    @Override
    public String toString(){
        return "DcAclFileAttributeView{" + "owner=" + owner +  ", acl=" + acl + "'}'";
    }

}
