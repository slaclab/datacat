
package org.srs.datacat.dao;

import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedList;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetResultSetModel;
import org.srs.datacat.model.DatasetView;

/**
 * A DAO object for searching datasets and containers.
 * @author bvan
 */
public interface SearchDAO extends BaseDAO{
    
    /**
     * Search containers for datasets.
     * @param containers
     * @param datasetView
     * @param query
     * @param metaFieldsToRetrieve
     * @param sortFields
     * @param offset
     * @param max
     * @return
     * @throws ParseException
     * @throws IOException 
     */
    DatasetResultSetModel search(LinkedList<DatacatNode> containers, DatasetView datasetView, 
            String query, String[] metaFieldsToRetrieve, String[] sortFields, 
            int offset, int max) throws ParseException, IOException;
    
}
