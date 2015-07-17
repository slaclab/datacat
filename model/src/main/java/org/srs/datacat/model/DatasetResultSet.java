
package org.srs.datacat.model;

import java.util.List;

/**
 * A result set from a  search operation.
 * @author bvan
 */
public interface DatasetResultSet {
    
    /**
     * Return the datasets that matched the search.
     * @return 
     */
    List<DatasetModel> getResults();
    
    /**
     * The total count of all results.
     * @return 
     */
    int getCount();

}