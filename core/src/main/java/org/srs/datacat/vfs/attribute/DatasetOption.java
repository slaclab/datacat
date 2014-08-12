
package org.srs.datacat.vfs.attribute;

import java.nio.file.OpenOption;

/**
 *
 * @author bvan
 */
public enum DatasetOption implements OpenOption {
    
    CREATE_NODE,
    CREATE_VERSION,
    CREATE_LOCATION,
    MERGE_VERSION;

}
