
package org.srs.datacat.model;

import java.util.List;

/**
 * A result set from a  search operation.
 * @author bvan
 */
public interface DatasetResultSetModel extends Iterable<DatasetModel> {
    
    /**
     * Return the datasets that matched the search.
     * @return List of Datasets
     */
    List<DatasetModel> getResults();
    
    /**
     * The total count of all results.
     * @return the count
     */
    Integer getCount();
    
    /**
     * Version Builder interface.
     */
    public interface Builder {
        
        DatasetResultSetModel build();
        Builder results(List<DatasetModel> val);
        Builder count(Integer val);
        
    }

}