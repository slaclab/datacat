package org.srs.datacat.vfs.security;

import java.io.IOException;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.List;

/**
 *
 * @author bvan
 */
public class DcAclFileAttributeView implements AclFileAttributeView {
    
    List<AclEntry> acl;
    
    public DcAclFileAttributeView(List<AclEntry> acl){
        this.acl = acl;
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
        throw new UnsupportedOperationException( "Owners not supported" );
    }

    @Override
    public void setOwner(UserPrincipal owner) throws IOException{
        throw new UnsupportedOperationException( "Owners not supported" );
    }


    public Object value(){
        throw new UnsupportedOperationException( "Not supported yet." );
    }

}
