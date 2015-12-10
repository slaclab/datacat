
package org.srs.datacat.model.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * A class defining the DcPermissions model.
 * 
 * @author bvan
 */
public enum DcPermissions {
    

    /**
     *  READ allows a user to lookup the contents of a directory and read the datasets in
     *  in that directory. It does NOT necessarily allow the user to lookup the contents
     *  of a subdirectory
     */
    READ,
    
    /**
     * INSERT allows a user to create a dataset (and add versions/locations) or subdirectory.
     * It DOES NOT imply that a user will be able to overwrite a dataset, version,
     * locations, or modify or add metadata.
     */
    INSERT,
    
    /**
     * DELETE allows a user to delete datasets from a directory or the directory itself.
     * It does not necessarily apply to the subdirectories. If a user does not have permission
     * to delete the subdirectories, the user will not have permission to delete the directory.
     */
    DELETE,
    
    /**
     * MODIFY allows a user to modify this directory, including it's name and metadata,
     * as well as modify datasets, versions, locations, and their metadata. It does not 
     * allow a user to delete a dataset, but a user may be able to delete versions and locations.
     * MODIFY is necessary for dataset view merging, as a MERGE will remove the current version.
     */
    WRITE,
    
    /**
     * ADMIN allows a user in a group to administer the ACLs.
     */
    ADMIN;
    
    public static String pack(DcPermissions permission){
        switch (permission){
            case READ:
                return "r";
            case INSERT:
                return "i";
            case DELETE:
                return "d";
            case WRITE:
                return "w";
            case ADMIN:
                return "a";
            default:
                throw new IllegalArgumentException("Unable to process permission: " + permission);
        }
    }
    
    public static DcPermissions unpack(char permission){
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
            default:
                throw new IllegalArgumentException("Unable to unpack permission");
        }
    }
        
    public static String pack(Set<DcPermissions> permissions){
        List<DcPermissions> plist = new ArrayList<>(permissions);
        Collections.sort(plist);
        StringBuilder b = new StringBuilder();
        for(DcPermissions e: plist){
            b.append(pack(e));
        }
        return b.toString();
    }
        
    public static Set<DcPermissions> unpackString(String permissions){
        HashSet<DcPermissions> perms = new HashSet<>(permissions.length());
        for(int i = 0; i < permissions.length(); i++){
            perms.add(unpack(permissions.charAt(i)));
        }
        return perms;
    }
    
    public static boolean check(Set<DcGroup> usersGroups, List<DcAclEntry> acl, DcPermissions permission){
        for(DcAclEntry entry: acl){
            if(usersGroups.contains((DcGroup) entry.getSubject())){
                if(entry.getPermissions().contains(permission)){
                    return true;
                }
            }
        }
        return false;
    }    
    
}
