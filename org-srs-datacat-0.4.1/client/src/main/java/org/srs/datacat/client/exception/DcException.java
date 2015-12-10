package org.srs.datacat.client.exception;

/**
 * The base exception class for the datacat client (this module) when there is a general error
 * interacting with the API.
 *
 * @author bvan
 */
public class DcException extends RuntimeException {

    public DcException(String message, Throwable cause){
        super(message, cause);
    }
    
}
