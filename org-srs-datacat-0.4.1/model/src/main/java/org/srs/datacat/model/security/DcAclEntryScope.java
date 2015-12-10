package org.srs.datacat.model.security;

/**
 * A flag denoting the scope of the DcAclEntry. 
 * 
 * -ACCESS-scoped entries provide permissions.
 * -DEFAULT-scoped entries are inherited permissions used when there are
 *  no defined ACCESS-scoped entries for a given container.
 * 
 * @author bvan
 */
public enum DcAclEntryScope {

    ACCESS,
    DEFAULT;

}
