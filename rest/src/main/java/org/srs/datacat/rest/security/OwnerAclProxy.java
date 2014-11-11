
package org.srs.datacat.rest.security;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.IOException;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.srs.datacat.vfs.attribute.DcAclFileAttributeView;

/**
 *
 * @author bvan
 */
@XmlRootElement(name="permissions")
@XmlType(propOrder={"owner","acl"})
@JsonPropertyOrder({"owner","acl"})
public class OwnerAclProxy {

    private final UserPrincipal owner;
    private List<AclEntry> acl;
    
    public OwnerAclProxy(DcAclFileAttributeView attr) throws IOException{
        this.owner = attr.getOwner();
        this.acl = attr.getAcl();
    }
    
    @XmlElement
    public String getOwner(){
        return owner != null ? owner.getName() : "";
    }
    
    @XmlElement(required=false)
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
