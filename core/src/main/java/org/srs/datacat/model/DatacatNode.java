
package org.srs.datacat.model;

import org.srs.datacat.security.OwnerAclAttributes;

/**
 *
 * @author bvan
 */
public interface DatacatNode extends DatacatRecord {
    
    Long getParentPk();
    String getName();
    OwnerAclAttributes getAclAttributes();
    
}
