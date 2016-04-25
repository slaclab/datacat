
package org.srs.datacat.client.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.srs.datacat.client.Client;
import org.srs.datacat.model.DatasetResultSetModel;

/**
 * Helper class for programmatically building searches.
 * @author bvan
 */
public class SearchBuilder {
    
    private final Client client;
    private String target;
    private String versionId;
    private String site;
    private String query;
    private String folderQuery;
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
     */
    public void setSite(String site){
        this.site = site;
    }

    /**
     * Query String.
     */
    public void setQuery(String query){
        this.query = query;
    }
    
    /**
     * Folder Query String.
     * Same semantics as the regular query string, but for folders.
     */
    public void setFolderQuery(String folderQuery){
        this.folderQuery = folderQuery;
    }
    
    /**
     * Add metadata field to sort by.
     */
    public void addSort(String sortItem){
        this.sort.add(sortItem);
    }

    /**
     * Set the sort list. This makes  a copy of the list.
     */
    public void setSort(List<String> sort){
        Objects.requireNonNull(sort, "Sort list must not be null");
        this.sort = new ArrayList(sort);
    }
    
    /**
     * Add a metadata field to return.
     */
    public void addShow(String showItem){
        this.show.add(showItem);
    }

    /**
     * Set the list of fields to return. This method makes a copy of the list.
     */
    public void setShow(List<String> show){
        Objects.requireNonNull(show, "Sort list must not be null");
        this.show = new ArrayList(show);
    }

    /**
     * Offset of first returned record.
     */
    public void setOffset(Integer offset){
        this.offset = offset;
    }

    /**
     * Max number of records to return.
     * @param max 
     */
    public void setMax(Integer max){
        this.max = max;
    }
    
    /**
     * Execute the search and retrieve the results.
     */
    public DatasetResultSetModel search(){
        return client.searchForDatasets(target, versionId, site, 
                query, folderQuery, sort.toArray(new String[0]), show.toArray(new String[0]), 
                offset, max);
    }

}
