
package org.srs.rest.shared;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.srs.datacat.shared.DatacatObject;
import org.srs.rest.shared.metadata.MetadataEntry;

/**
 *
 * @author Brian Van Klaveren<bvan@slac.stanford.edu>
 */
@XmlRootElement(name="error")
@XmlType(propOrder={"message","type", "code", "cause"})
@JsonPropertyOrder({"message","type", "code", "cause"})
@JsonDeserialize(builder=MetadataEntry.Builder.class)
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
    
    @XmlElement
    public String getMessage(){
        return message;
    }

    @XmlElement(required=false)
    public String getType(){
        return type;
    }

    @XmlElement(required=false)
    public String getCode(){
        return code;
    }

    @XmlElement(required=false)
    public String getCause(){
        return cause;
    }
  
    @XmlTransient
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
            return new ErrorResponse( message, type, code, cause );
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
                                   Annotation[] annotations, MediaType mediaType) {
            return type.isAssignableFrom( ErrorResponse.class );
        }

        @Override
        public void writeTo(ErrorResponse t,
                Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException{
            System.out.println("writing");
            entityStream.write( t.toString().getBytes());
            entityStream.write( "\n".getBytes() );
        }


        @Override
        public long getSize(ErrorResponse t,
                Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType){
            throw new UnsupportedOperationException( "Not supported yet." );
        }
    }
    
}
