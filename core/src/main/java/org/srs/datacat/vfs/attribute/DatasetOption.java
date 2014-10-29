
package org.srs.datacat.vfs.attribute;

import java.nio.file.OpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author bvan
 */
public enum DatasetOption implements OpenOption {
    
    CREATE_NODE,
    CREATE_VERSION,
    CREATE_LOCATIONS,
    MERGE_VERSION,
    // These are for optimizations
    SKIP_NODE_CHECK,
    SKIP_VERSION_CHECK,
    SKIP_LOCATION_CHECK;
    
    public static Collection<DatasetOption> VERSION_WORK = 
            Collections.unmodifiableCollection(Arrays.asList(CREATE_VERSION, MERGE_VERSION));
    public static Collection<DatasetOption> VIEW_WORK = 
            Collections.unmodifiableCollection(Arrays.asList(CREATE_VERSION, MERGE_VERSION, CREATE_LOCATIONS));

}
