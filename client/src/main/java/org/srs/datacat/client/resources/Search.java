
package org.srs.datacat.client.resources;

import com.google.common.base.Optional;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 *
 * @author bvan
 */
public class Search {
    
    WebTarget webTarget;

    public Search(WebTarget baseTarget){
        this.webTarget = baseTarget.path("search.json");
    }
    
    /**
     * Search a target. A target is a Container of some sort. It may also be specified as a glob, as in:
     *   1. /path/to - target /path/to _only_
     *   2. /path/to/* - target is all containers directly in /path/to/
     *   3. /path/to/** - target is all containers, recursively, under /path/to/
     *   4. /path/to/*$ - target is only folders directly under /path/to/
     *   5. /path/to/**^ - target is only groups, recursively, under /path/to/
     * @param target The path (or glob-like path) of which to search
     * @param versionId Version Id to return
     * @param site Site to query
     * @param query Query String
     * @param sort Fields and Metadata fields to sort on.
     * @param show Metadata fields to optionally return
     * @param offset Offset at which to start returning objects.
     * @param max Maximum number of datasets to return
     * @return Response object of the search
     */
    public Response searchForDatasets(String target, Optional<String> versionId, Optional<String> site,
            Optional<String> query, Optional<String[]> sort, Optional<String> show, Optional<Integer> offset,
            Optional<Integer> max){
        return Path.getTarget(webTarget.path(target), versionId, site)
                .queryParam("filter", query.orNull())
                .queryParam("sort", sort.orNull())
                .queryParam("show", show.orNull())
                .queryParam("offset", offset.orNull())
                .queryParam("max", max.orNull())
                .request()
                .get();
    }

}
