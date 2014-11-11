
package org.srs.datacat.security;

import com.google.common.base.Optional;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author bvan
 */
public class DcPermissions {

    /**
     *  READ allows a user to lookup the contents of a directory and read the datasets in
     *  in that directory. It does NOT necessarily allow the user to lookup the contents
     *  of a subdirectory
     */
    public static final AclEntryPermission READ = AclEntryPermission.READ_DATA;
    
    /**
     * INSERT allows a user to create a dataset (and add versions/locations) or subdirectory.
     * It DOES NOT imply that a user will be able to overwrite a dataset, version,
     * locations, or modify or add metadata.
     */
    public static final AclEntryPermission INSERT = AclEntryPermission.APPEND_DATA;
    
    /**
     * DELETE allows a user to delete datasets from a directory or the directory itself.
     * It does not necessarily apply to the subdirectories. If a user does not have permission
     * to delete the subdirectories, the user will not have permission to delete the directory.
     */
    public static final AclEntryPermission DELETE = AclEntryPermission.DELETE;
    
    /**
     * MODIFY allows a user to modify this directory, including it's name and metadata,
     * as well as modify datasets, versions, locations, and their metadata. It does not 
     * allow a user to delete a dataset, but a user may be able to delete versions and locations.
     * MODIFY is necessary for dataset view merging, as a MERGE will remove the current version.
     */
    public static final AclEntryPermission WRITE = AclEntryPermission.WRITE_DATA;
    
    /**
     * ADMIN allows a user in a group to administer the ACLs.
     */
    public static final AclEntryPermission ADMIN = AclEntryPermission.WRITE_ACL;
    
    public static String pack(AclEntryPermission permission){
        if(permission == READ)
            return "r";
        if(permission == INSERT)
            return "i";
        if(permission == DELETE)
            return "d";
        if(permission == WRITE)
            return "w";
        if(permission == ADMIN)
            return "a";
        return "";
    }
    
    public static AclEntryPermission unpack(char permission){
        switch(permission){
            case 'r':
                return READ;
            case 'i':
                return INSERT;
            case 'd':
                return DELETE;
            case 'w':
                return WRITE;
            case 'a':
                return ADMIN;
        }
        throw new IllegalArgumentException("Unable to unpack permission");
    }

    
    public static Optional<OwnerAclAttributes> getOwnerAclAttributes(String aclString){
        if(aclString == null || aclString.isEmpty()){
            return Optional.absent();
        }
        List<AclEntry> acl = new ArrayList<>();
        DcUser owner = null;
        String aclEntries[] = aclString.split(",");
        for(String aclEntry: aclEntries){
            String[] ace = aclEntry.split( ":");
            String uprin = ace[0];
            String uprinType = ace[1];
            UserPrincipal up = null;
            String[] uprinWithExp = uprin.split("@");
            String user = uprinWithExp[0];
            String experiment = uprinWithExp.length > 1 ? uprinWithExp[1] : null;
            switch(uprinType){
                case "g":
                    up = new DcGroup(user, experiment);
                    break;
                case "o":
                    owner = new DcUser(user);
                    break;
                default:
                    
            }
            
            if(up != null){
                Set<AclEntryPermission> perms = new HashSet<>();
                String permissions = ace[2];
                for(int i = 0; i < permissions.length(); i++){
                    perms.add(DcPermissions.unpack(permissions.charAt(i)));
                }
                acl.add(AclEntry.newBuilder().setPrincipal(up).setPermissions(perms).setType(AclEntryType.ALLOW).build());
            }
        }
        return Optional.fromNullable(new OwnerAclAttributes(owner, acl));
    }
}
