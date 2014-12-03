
package org.srs.datacat.model;

/**
 *
 * @author bvan
 */
public interface DatacatNode extends DatacatRecord {
    
    Long getParentPk();
    String getName();
    String getAcl();
    
}
