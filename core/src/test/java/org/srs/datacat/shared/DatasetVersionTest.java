
package org.srs.datacat.shared;


import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import java.io.IOException;
import java.io.StringWriter;
import junit.framework.TestCase;
import static junit.framework.TestCase.assertEquals;

/**
 *
 * @author bvan
 */
public class DatasetVersionTest extends TestCase {
    String expectedJsonText = "{\"$type\":\"version\",\"versionId\":-2}";
    String jsonText = "{\"versionId\":\"next\"}";
    
    public DatasetVersionTest(){ }
    
    public void testDeserialization() throws IOException{
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        AnnotationIntrospector pair = new AnnotationIntrospectorPair(primary, secondary);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector( pair );
        
        DatasetVersion o = mapper.readValue( jsonText, DatasetVersion.class );
        StringWriter writ = new StringWriter();
        mapper.writeValue( writ, o );
        
        String actual = writ.toString();
        
        
        assertEquals("JSON string representation mismatch",expectedJsonText,actual);
    }
        
}
