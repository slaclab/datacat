
package org.srs.datacat.vfs.security;

import java.nio.file.attribute.AclEntryPermission;

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
    
    public static AclEntryPermission unpack(String permission){
        switch(permission){
            case "r":
                return READ;
            case "i":
                return INSERT;
            case "d":
                return DELETE;
            case "w":
                return WRITE;
            case "a":
                return ADMIN;
        }
        throw new IllegalArgumentException("Unable to unpack permission");
    }

}
