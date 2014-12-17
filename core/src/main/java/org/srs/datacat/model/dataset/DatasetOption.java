
package org.srs.datacat.model.dataset;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author bvan
 */
public enum DatasetOption {
    
    CREATE_NODE,
    CREATE_VERSION,
    CREATE_LOCATIONS,
    MERGE_VERSION,
    // These are for optimizations
    SKIP_NODE_CHECK,
    SKIP_VERSION_CHECK,
    SKIP_LOCATION_CHECK;
    
    public static final Collection<DatasetOption> VERSION_WORK = 
            Collections.unmodifiableCollection(Arrays.asList(CREATE_VERSION, MERGE_VERSION));
    
    public static final Collection<DatasetOption> VIEW_WORK = 
            Collections.unmodifiableCollection(Arrays.asList(CREATE_VERSION, MERGE_VERSION, CREATE_LOCATIONS));

}
