
package org.srs.datacat.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

/**
 *
 * @author bvan
 */
@PreMatching
@Priority(Priorities.HEADER_DECORATOR)
public class RequestAcceptFilter implements ContainerRequestFilter {

    protected final Map<String, MediaType> mediaTypeMappings = new HashMap<String,MediaType>(){
        {
           put("xml",MediaType.APPLICATION_XML_TYPE);
           put("json",MediaType.APPLICATION_JSON_TYPE);
           put("txt",MediaType.TEXT_PLAIN_TYPE);
        }
    };
    
    /**
     * Create a filter with suffix to media type mappings 
     * for suffixes under the root resource
     */
    public RequestAcceptFilter() { }
        
    @Override
    public void filter(ContainerRequestContext rc) throws IOException {
        UriInfo uriInfo = rc.getUriInfo();
        
        // Quick check for a '.' character
        String path = uriInfo.getRequestUri().getRawPath();
        PathSegment segment = uriInfo.getPathSegments( false ).get( 0 );
        if (segment == null) {
            return;
        }
        if (segment.getPath().indexOf('.') == -1) {
            return;
        }

        final int length = path.length();

        // Get the suffixes
        final String[] suffixes = segment.getPath().split("\\.");

        for (int i = suffixes.length - 1; i >= 1; i--) {
            final String suffix = suffixes[i];
            if (suffix.length() == 0) {
                continue;
            }

            final MediaType accept = mediaTypeMappings.get(suffix);

            if (accept != null) {
                rc.getHeaders().remove(HttpHeaders.ACCEPT);
                rc.getHeaders().putSingle(HttpHeaders.ACCEPT, accept.toString());

                final int index = path.indexOf('.' + suffix);
                path = new StringBuilder(path).delete(index, index + suffix.length() + 1).toString();
                suffixes[i] = "";
                break;
            }
        }


        if (length != path.length()) {
            rc.setRequestUri(uriInfo.getRequestUriBuilder().replacePath(path).build());
        }
    }

}
