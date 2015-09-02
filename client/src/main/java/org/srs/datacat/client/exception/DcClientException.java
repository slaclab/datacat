package org.srs.datacat.client.exception;

/**
 * The base exception class for all datacat exceptions returned from the datacat server.
 *
 * @author bvan
 */
public class DcClientException extends DcException {
    private String type;
    private String code;
    private int statusCode;

    /**
     * Construct DcClientException from standard fields.
     *
     * @param type Type of datacat error
     * @param message Message from the server
     * @param cause Root cause of the error
     * @param code Additional Error code
     * @param statusCode HTTP status code
     */
    public DcClientException(String type, String message, String cause, String code,
            int statusCode){
        super(message, new Throwable(cause));
        this.type = type;
        this.code = code;
        this.statusCode = statusCode;
    }

    public String getType(){
        return type;
    }

    public String getCode(){
        return code;
    }

    public int getStatusCode(){
        return statusCode;
    }

    @Override
    public String toString(){
        return "DcClientException{" + super.toString() + ", " + "type=" + 
                type + ", code=" + code + ", statusCode=" + statusCode + '}';
    }

}
