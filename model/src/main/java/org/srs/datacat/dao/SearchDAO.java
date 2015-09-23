
package org.srs.datacat.dao;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.text.ParseException;
import java.util.LinkedList;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetView;

/**
 * A DAO object for searching datasets and containers.
 * @author bvan
 */
public interface SearchDAO extends BaseDAO{
    
    /**
     * Search containers for datasets.
     * @param containers List of Containers to search for datasets in.
     * @param datasetView Requested view of datasets to be returned.
     * @param query String to be parsed by underlying search mechanism.
     * @param metaFieldsToRetrieve A list of fields to return as metadata, or null for none.
     * @param sortFields A list of fields to sort by, or null.
     * @return
     * @throws ParseException The parser failed to parse the query.
     * @throws IOException An exception occurred performing the operation or talking to the data source.
     */
    DirectoryStream<DatasetModel> search(LinkedList<DatacatNode> containers, DatasetView datasetView, 
            String query, String[] metaFieldsToRetrieve, String[] sortFields) throws ParseException, IOException;
    
}
