
package org.srs.datacat.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
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


/**
 *
 * @author bvan
 */
@Produces("text/plain")
public class DatacatObjectTextProvider implements MessageBodyWriter<DatacatObject> {
 
    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
                               Annotation[] annotations, MediaType mediaType) {
        return DatacatObject.class.isAssignableFrom( type );
    }
  
    @Override
    public void writeTo(DatacatObject t,
            Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException{
        entityStream.write( t.toString().getBytes() );
        entityStream.write( "\n".getBytes() );
    }

    @Override
    public long getSize(DatacatObject t,
            Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType){
        throw new UnsupportedOperationException( "Not supported yet." );
    }

}
