
package org.srs.datacat.client.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.srs.datacat.client.Client;
import org.srs.datacat.model.DatasetResultSetModel;

/**
 *
 * @author bvan
 */
public class SearchBuilder {
    
    private Client client;
    private String target;
    private String versionId;
    private String site;
    private String query;
    private List<String> sort = new ArrayList<>();
    private List<String> show = new ArrayList<>();
    private Integer offset;
    private Integer max;

    public SearchBuilder(Client client){
        this.client = client;
    }
    
    /**
     * Set search target. A target is a Container of some sort. It may also be specified as a glob, as in: <p>
     *   1. {@code /path/to} - target {@code /path/to} _only_ <p>
     *   2. {@code /path/to/*} - target is all containers directly in {@code /path/to/}<p>
     *   3. {@code /path/to/**} - target is all containers, recursively, under {@code /path/to/} <p>
     *   4. {@code /path/to/*$} - target is only folders directly under {@code /path/to/} <p>
     *   5. {@code /path/to/**^} - target is only groups, recursively, under {@code /path/to/}<p>
     * @param target The path (or glob-like path) of which to search
    */
    public void setTarget(String target){
        this.target = target;
    }

    /**
     * Set version id to search. The string can be a specific number or the word "current". 
     * By default, searches will always search the most current version.
     * @param versionId Version Id to return
     */
    public void setVersionId(String versionId){
        this.versionId = versionId;
    }

    /**
     * Site to search. The String can be an actual site, i.e. SLAC, or the word "master" or 
     * "canonical".
     * @param site 
     */
    public void setSite(String site){
        this.site = site;
    }

    /**
     * Query String.
     * @param query 
     */
    public void setQuery(String query){
        this.query = query;
    }
    
    /**
     * Add field to sort by.
     * @param sortItem
     */
    public void addSort(String sortItem){
        this.sort.add(sortItem);
    }

    /**
     * Set the sort list. This makes  a copy of the list.
     * @param sort 
     */
    public void setSort(List<String> sort){
        Objects.requireNonNull(sort, "Sort list must not be null");
        this.sort = new ArrayList(sort);
    }
    
    /**
     * Add a field to return.
     * @param showItem
     */
    public void addShow(String showItem){
        this.show.add(showItem);
    }

    /**
     * Set the list of fields to return. This method makes a copy of the list.
     * @param show 
     */
    public void setShow(List<String> show){
        Objects.requireNonNull(show, "Sort list must not be null");
        this.show = new ArrayList(show);
    }

    public void setOffset(Integer offset){
        this.offset = offset;
    }

    public void setMax(Integer max){
        this.max = max;
    }
    
    /**
     * @param sort Fields and Metadata fields to sort on.
     * @param show Metadata fields to optionally return
     * @param offset Offset at which to start returning objects.
     * @param max Maximum number of datasets to return
     * @return Response object of the search
     */
    public DatasetResultSetModel search(){
        return client.searchForDatasets(target, versionId, site, 
                query, sort.toArray(new String[0]), show.toArray(new String[0]), offset, max);
    }

}
