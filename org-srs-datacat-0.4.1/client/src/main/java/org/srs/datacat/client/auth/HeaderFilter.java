
package org.srs.datacat.client.auth;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

/**
 *
 * @author bvan
 */
public class HeaderFilter implements ClientRequestFilter {

    private Map<String, Object> headers;
    
    public HeaderFilter(){
        this.headers = new HashMap<>();
    }
    
    public HeaderFilter(Map<String, Object> headers){
        this.headers = headers;
    }
    
    public void setHeaders(Map<String, Object> headers){
        this.headers = headers;
    }
    
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException{
        if(this.headers != null){
            for(Map.Entry<String, Object> e: headers.entrySet()){
                requestContext.getHeaders().add(e.getKey(), e.getValue());
            }
        }
    }

}
