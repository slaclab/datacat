
package org.srs.rest.shared;

import java.util.logging.Logger;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author bvan
 */
public class RestException extends WebApplicationException {
    private Response response;
    
    public RestException(String reason, int status) {
        super(Response.status(status).type(MediaType.APPLICATION_JSON).build());
        JSONObject r = new JSONObject();
        try {
            r.put("exception", "RestException");
            r.put("reason", reason);
        } catch (JSONException ex) {
            Logger.getLogger(RestException.class.getName()).severe("Unable to create JSON exception");
        }
        this.response = Response.status(status).entity(r.toString()).type(MediaType.APPLICATION_JSON).build();
    }
    
    public RestException(Exception e, int status) {
        super(Response.status(status).type(MediaType.APPLICATION_JSON).build());
        JSONObject r = new JSONObject();
        try {
            r.put("exception", e.getClass().getSimpleName());
            r.put("reason", e.getMessage());
        } catch (JSONException ex) {
            Logger.getLogger(RestException.class.getName()).severe("Unable to create JSON exception");
        }
        this.response = Response.status(status).entity(r.toString()).type(MediaType.APPLICATION_JSON).build();
    }
    
    @Override
    public Response getResponse(){
        return response;
    }
}