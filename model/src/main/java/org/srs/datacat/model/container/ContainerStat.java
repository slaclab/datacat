
package org.srs.datacat.model.container;

/**
 * Marker interface for container stat objects. Objects returned from a stat command
 * should implement this interface.
 * @author bvan
 */
public interface ContainerStat {
    
    Integer getChildCount();
    
}
