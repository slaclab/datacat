
package org.srs.datacat.rest;

import com.fasterxml.jackson.databind.JsonMappingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author bvan
 */
@Provider
public class JacksonExceptionMapper implements ExceptionMapper<JsonMappingException> {
    public Response toResponse(JsonMappingException ex) {
        RestException exc =  new RestException((Exception) ex.getCause(), 400 , "Unable to validate input", 
                ex.getCause().getMessage());
        return exc.getResponse();
    }
}