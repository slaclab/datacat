package org.srs.datacat.client.exception;

import java.util.List;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Base exception class for all http exceptions not returned from the datacat server.
 *
 * @author bvan
 */
public class DcRequestException extends DcException {
    private String url;
    private int statusCode;
    private Map<String, List<Object>> headers;
    private String entity;
    
    public DcRequestException(WebApplicationException ex){
        super(ex.getMessage(), ex);
    }

    public DcRequestException(String message, String url, int statusCode, Map<String, List<Object>> headers,
            String entity){
        super(message, null);
        this.url = url;
        this.statusCode = statusCode;
        this.headers = headers;
        this.entity = entity;
    }
    
    public DcRequestException(String message, Response resp){
        this(message, 
                resp.getLocation() != null ? resp.getLocation().toString() : "",
                resp.getStatus(), 
                resp.getHeaders(), 
                resp.readEntity(String.class));
    }

    public String getUrl(){
        return url;
    }

    public int getStatusCode(){
        return statusCode;
    }

    public Map<String, List<Object>> getHeaders(){
        return headers;
    }

    public String getEntity(){
        return entity;
    }

    @Override
    public String toString(){
        return "DcRequestException{" + "url=" + url + ", statusCode=" + 
                statusCode + ", headers=" + headers + ", entity=" + entity + '}';
    }

}
