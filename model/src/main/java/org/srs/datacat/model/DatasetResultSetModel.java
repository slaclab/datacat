
package org.srs.datacat.model;

import java.util.List;

/**
 * A result set from a  search operation.
 * @author bvan
 */
public interface DatasetResultSetModel {
    
    /**
     * Return the datasets that matched the search.
     * @return 
     */
    List<DatasetModel> getResults();
    
    /**
     * The total count of all results.
     * @return 
     */
    Integer getCount();
    
    /**
     * Version Builder interface.
     * @param <U> 
     */
    public interface Builder {
        
        DatasetResultSetModel build();
        Builder results(List<DatasetModel> val);
        Builder count(Integer val);
        
    }

}