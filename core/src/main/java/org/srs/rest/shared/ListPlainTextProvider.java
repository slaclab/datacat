
package org.srs.rest.shared;

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
import org.srs.datacat.shared.DatacatObject;

/**
 *
 * @author bvan
 */
@Produces("text/plain")
public class ListPlainTextProvider implements MessageBodyWriter<List> {
 
    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
                               Annotation[] annotations, MediaType mediaType) {
        return List.class.isAssignableFrom( type );
        /*if(List.class.isAssignableFrom( type )){
            Type arrayType = ( (ParameterizedType) genericType).getActualTypeArguments()[0];
            return DatacatObject.class.isAssignableFrom( (Class) arrayType );
        }
        return false;*/
    }
  
    @Override
    public void writeTo(List t,
            Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException{
        
        for(Object o: t){
            entityStream.write( o.toString().getBytes() );
            entityStream.write( "\n".getBytes() );
        }
    }


    @Override
    public long getSize(List t,
            Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType){
        throw new UnsupportedOperationException( "Not supported yet." );
    }

}