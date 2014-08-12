
package org.srs.datacat.vfs.attribute;

import java.io.IOException;
import java.nio.file.attribute.FileAttributeView;
import org.srs.datacat.shared.DatacatObject;

/**
 *
 * @author bvan
 */
public interface DcViewProvider<T> extends FileAttributeView {
    
    public DatacatObject withView(T viewDescriptor) throws IOException;

}
