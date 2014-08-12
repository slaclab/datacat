
package org.srs.datacat.vfs.security;

import java.nio.file.attribute.AclEntryPermission;

/**
 *
 * @author bvan
 */
public class DcPermissions {

    public static final AclEntryPermission CREATE_CHILD = AclEntryPermission.ADD_FILE;
    public static final AclEntryPermission DELETE = AclEntryPermission.DELETE;
    public static final AclEntryPermission READ = AclEntryPermission.READ_DATA;
    public static final AclEntryPermission MODIFY = AclEntryPermission.WRITE_DATA;
    public static final AclEntryPermission ADMIN = AclEntryPermission.WRITE_ACL;
    
}
