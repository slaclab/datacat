
package org.srs.datacat.dao;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.shared.DatacatObject;

/**
 *
 * @author bvan
 */
public interface BaseDAO extends AutoCloseable {

    void commit() throws IOException;
    
    @Override
    public void close() throws IOException;

    DatacatObject getObjectInParent(DatacatRecord parent, String path) throws IOException, NoSuchFileException;
    
    void addMetadata(DatacatRecord record, Map metaData) throws IOException;
    
    void delete(DatacatRecord record) throws IOException;
    
}
