
package org.srs.datacat.model;

import org.srs.datacat.shared.container.BasicStat;

/**
 * An interface denoting either a folder or a group
 * @author bvan
 */
public interface DatasetContainer {
    
    public BasicStat getStat();
    public String getDescription();
    
}
