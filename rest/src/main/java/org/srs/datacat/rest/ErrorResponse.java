package org.srs.datacat.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import org.srs.datacat.rest.ErrorResponse.Builder;

/**
 *
 * @author bvan
 */
@JsonPropertyOrder({"message", "type", "code", "cause"})
@JsonDeserialize(builder = Builder.class)
public class ErrorResponse {
    private String message;
    private String type;
    private String code;
    private String cause;

    public ErrorResponse(){
    }

    public ErrorResponse(String message, String type, String code, String cause){
        this.message = message;
        this.type = type;
        this.code = code;
        this.cause = cause;
    }

    public String getMessage(){
        return message;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getType(){
        return type;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCode(){
        return code;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCause(){
        return cause;
    }

    public static class Builder {
        private String message;
        private String type;
        private String code;
        private String cause;

        public Builder(){
        }

        public Builder setMessage(String message){
            this.message = message;
            return this;
        }

        public Builder setType(String type){
            this.type = type;
            return this;
        }

        public Builder setCode(String code){
            this.code = code;
            return this;
        }

        public Builder setCause(String cause){
            this.cause = cause;
            return this;
        }

        public ErrorResponse build(){
            return new ErrorResponse(message, type, code, cause);
        }
    }

    @Override
    public String toString(){
        return "ErrorResponse{" + "message=" + message + ", type=" + type + ", code=" + code + ", cause=" + cause + '}';
    }

    // TODO: Hack because grizzly doesn't handle text/plain so great
    @Produces("text/plain")
    public static class ErrorTextProvider implements MessageBodyWriter<ErrorResponse> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType,
                Annotation[] annotations, MediaType mediaType){
            return type.isAssignableFrom(ErrorResponse.class);
        }

        @Override
        public void writeTo(ErrorResponse t,
                Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException{
            System.out.println("writing");
            entityStream.write(t.toString().getBytes());
            entityStream.write("\n".getBytes());
        }

        @Override
        public long getSize(ErrorResponse t,
                Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType){
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

}
