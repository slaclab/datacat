
package org.srs.datacat.shared;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import junit.framework.TestCase;
import org.srs.datacat.shared.metadata.MetadataDecimal;
import org.srs.datacat.shared.metadata.MetadataEntry;
import org.srs.datacat.shared.metadata.MetadataInteger;
import org.srs.datacat.shared.metadata.MetadataValue;

/**
 *
 * @author bvan
 */
public class MetadataTest extends TestCase {
    
    
    public void testMetadataNoTypesDeserialize() throws IOException {
        String jsonDecimal = "{\"key\":\"number1\",\"value\":1234.25}";
        String jsonInteger = "{\"key\":\"number1\",\"value\":1234}";
        String jsonString = "{\"key\":\"hello\",\"value\":\"world\"}";
        
        String jsonListStringNoType = "[ {\"key\":\"nRuns\", \"value\":\"hello\"} ]";
        
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        AnnotationIntrospector pair = new AnnotationIntrospectorPair(primary, secondary);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector( pair );
        TypeReference singleRef = new TypeReference<MetadataEntry>(){};
        TypeReference compoundRef = new TypeReference<List<MetadataEntry>>(){};
        
        MetadataEntry entry;
        
        entry = mapper.readValue( jsonDecimal, singleRef);
        assertEquals("MetadataDecimal", entry.getValue().getClass().getSimpleName());
        
        
        entry = mapper.readValue( jsonInteger, singleRef);
        assertEquals("MetadataInteger", entry.getValue().getClass().getSimpleName());
        
        
        entry = mapper.readValue( jsonString, singleRef);
        assertEquals("MetadataString", entry.getValue().getClass().getSimpleName());

        List<MetadataEntry> entries;
        entries = mapper.readValue( jsonListStringNoType, compoundRef);
        
    }
    
    public void testMetadataDeserialize() throws IOException{
        String jsonDecimal = "{\"key\":\"number1\",\"value\":1234.25}";
        //String jsonDecimalWithType = "{\"key\":\"number2\",\"value\":{\"decimal\":1234.25}}";
        String jsonDecimalWithType = "{\"key\":\"number2\",\"value\":1234.25, \"type\":\"decimal\"}";
        String jsonDecimalStringWithType = "{\"key\":\"number2\",\"value\":\"1234.25\", \"type\":\"decimal\"}";
        
        String jsonInteger = "{\"key\":\"number1\",\"value\":1234}";
        String jsonIntegerWithType = "{\"key\":\"number2\",\"value\":1234.25, \"type\":\"integer\"}";
        String jsonIntegerStringWithType = "{\"key\":\"number1\",\"value\":\"1234\", \"type\":\"integer\"}";
        String jsonIntegerWithBadType = "{\"key\":\"number2\",\"value\":1234.25, \"type\":\"integer\"}";
        
        String jsonString = "{\"key\":\"hello\",\"value\":\"world\"}";
        String jsonStringWithType = "{\"key\":\"hello\",\"value\":\"world\", \"type\":\"string\"}";
        
        String compoundJson = 
                "[{\"key\":\"numberDouble\",\"value\":1234.25},"
                + "{\"key\":\"numberInt\",\"value\":1234},"
                + "{\"key\":\"hello\",\"value\":\"world\"},"
                + "{\"key\":\"how\",\"value\":\"doyoudo\"}]";
        String compoundJsonWithTypes = 
                "[{\"key\":\"numberDouble\",\"value\":1234.25, \"type\":\"decimal\"},"
                + "{\"key\":\"numberInt\",\"value\":1234, \"type\":\"integer\"},"
                + "{\"key\":\"hello\",\"value\":\"world\", \"type\":\"string\"},"
                + "{\"key\":\"how\",\"value\":\"doyoudo\", \"type\":\"string\"}]";
        
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        AnnotationIntrospector pair = new AnnotationIntrospectorPair(primary, secondary);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector( pair );
        TypeReference singleRef = new TypeReference<MetadataEntry>(){};
        TypeReference compoundRef = new TypeReference<List<MetadataEntry>>(){};
        
        
        MetadataEntry entry;
        
        entry = mapper.readValue( jsonDecimal, singleRef);
        assertEquals("MetadataDecimal", entry.getValue().getClass().getSimpleName());
        
        entry = mapper.readValue( jsonDecimalWithType, singleRef);
        assertEquals("MetadataDecimal", entry.getValue().getClass().getSimpleName());
        
        entry = mapper.readValue( jsonDecimalStringWithType, singleRef);
        assertEquals("MetadataDecimal", entry.getValue().getClass().getSimpleName());
        
        entry = mapper.readValue( jsonInteger, singleRef);
        assertEquals("MetadataInteger", entry.getValue().getClass().getSimpleName());
                
        entry = mapper.readValue( jsonIntegerWithType, singleRef);
        assertEquals("MetadataInteger", entry.getValue().getClass().getSimpleName());
        
        entry = mapper.readValue( jsonIntegerStringWithType, singleRef);
        assertEquals("MetadataInteger", entry.getValue().getClass().getSimpleName());
        
        entry = mapper.readValue( jsonIntegerWithBadType, singleRef);
        assertEquals("MetadataInteger", entry.getValue().getClass().getSimpleName());
        
        entry = mapper.readValue( jsonString, singleRef);
        assertEquals("MetadataString", entry.getValue().getClass().getSimpleName());
        
        entry = mapper.readValue( jsonStringWithType, singleRef);
        assertEquals("MetadataString", entry.getValue().getClass().getSimpleName());
        
        List<MetadataEntry> entries;
        entries = mapper.readValue( compoundJson, compoundRef);
        for(MetadataEntry e: entries){
            System.out.println(e.getValue().getClass().getSimpleName());
        }
        entries = mapper.readValue( compoundJsonWithTypes, compoundRef);
        for(MetadataEntry e: entries){
            System.out.println(e.getValue().getClass().getSimpleName());
        }
    }
    
    public void testMetadataDeserializeRawMap() throws IOException{
        String jsonDecimal = "{\"number1\":1234.25}";
        //String jsonDecimalWithType = "{\"key\":\"number2\",\"value\":{\"decimal\":1234.25}}";
        String jsonDecimalWithType = "{\"number2\":\"1234.25\", \"type\":\"decimal\"}";
        String jsonBigDecimalWithType = "{\"number2\":\"1234.22222222222222222223422225\", "
                + "\"type\":\"decimal\"}";
        String jsonDecimalStringWithType = "{\"number2\":\"1234.25\", \"type\":\"decimal\"}";
        
        String jsonInteger = "{\"number1\":1234}";
        String jsonIntegerWithType = "{\"number2\":1234.25, \"type\":\"integer\"}";
        String jsonIntegerStringWithType = "{\"number1\":\"1234\", \"type\":\"integer\"}";
        String jsonIntegerWithBadType = "{\"number2\":1234.25, \"type\":\"integer\"}";
        
        String jsonString = "{\"hello\":\"world\"}";
        String jsonStringWithType = "{\"hello\":\"world\", \"type\":\"string\"}";
        
        String jsonTimestamp = "{\"hello\":\"2014-01-02T12:02:01Z\", \"type\":\"timestamp\"}";
        String jsonTimestamp2 = "{\"hello\":\"2014-01-02T12:02:01-0000\", \"type\":\"timestamp\"}";
        
        String compoundJson = 
                "[{\"numberDouble\":1234.25},"
                + "{\"numberInt\":1234},"
                + "{\"hello\":\"world\"},"
                + "{\"how\":\"doyoudo\"}]";
        String compoundJsonWithTypes = 
                "[{\"numberDouble\":1234.25, \"type\":\"decimal\"},"
                + "{\"numberInt\":1234, \"type\":\"integer\"},"
                + "{\"hello\":\"world\", \"type\":\"string\"},"
                + "{\"how\":\"doyoudo\", \"type\":\"string\"}]";
        
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        AnnotationIntrospector pair = new AnnotationIntrospectorPair(primary, secondary);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector( pair );
        TypeReference singleRef = new TypeReference<MetadataEntry>(){};
        TypeReference compoundRef = new TypeReference<List<MetadataEntry>>(){};
        
        
        MetadataEntry entry;
        
        entry = mapper.readValue( jsonDecimal, singleRef);
        assertEquals("MetadataDecimal", entry.getValue().getClass().getSimpleName());
        
        entry = mapper.readValue( jsonDecimalWithType, singleRef);
        assertEquals("MetadataDecimal", entry.getValue().getClass().getSimpleName());
        
        // Check bigdecimal
        entry = mapper.readValue( jsonBigDecimalWithType, singleRef);
        assertEquals("MetadataDecimal", entry.getValue().getClass().getSimpleName());
        assertEquals("BigDecimal", entry.getValue().getValue().getClass().getSimpleName());
        
        entry = mapper.readValue( jsonDecimalStringWithType, singleRef);
        assertEquals("MetadataDecimal", entry.getValue().getClass().getSimpleName());
        
        entry = mapper.readValue( jsonInteger, singleRef);
        assertEquals("MetadataInteger", entry.getValue().getClass().getSimpleName());
                
        entry = mapper.readValue( jsonIntegerWithType, singleRef);
        assertEquals("MetadataInteger", entry.getValue().getClass().getSimpleName());
        
        entry = mapper.readValue( jsonIntegerStringWithType, singleRef);
        assertEquals("MetadataInteger", entry.getValue().getClass().getSimpleName());
        
        entry = mapper.readValue( jsonIntegerWithBadType, singleRef);
        assertEquals("MetadataInteger", entry.getValue().getClass().getSimpleName());
        
        entry = mapper.readValue( jsonString, singleRef);
        assertEquals("MetadataString", entry.getValue().getClass().getSimpleName());
        
        entry = mapper.readValue( jsonStringWithType, singleRef);
        assertEquals("MetadataString", entry.getValue().getClass().getSimpleName());
        
        entry = mapper.readValue( jsonTimestamp, singleRef);
        assertEquals("MetadataTimestamp", entry.getValue().getClass().getSimpleName());
        
        entry = mapper.readValue( jsonTimestamp2, singleRef);
        assertEquals("MetadataTimestamp", entry.getValue().getClass().getSimpleName());
        
        List<MetadataEntry> entries;
        entries = mapper.readValue( compoundJson, compoundRef);
        for(MetadataEntry e: entries){
            System.out.println(e.getValue().getClass().getSimpleName());
        }
        entries = mapper.readValue( compoundJsonWithTypes, compoundRef);
        for(MetadataEntry e: entries){
            System.out.println(e.getValue().getClass().getSimpleName());
        }
        //System.out.println\("(Me[a-zA-Z]*):.*\+ *
    }
    
    public void testMetadataSerialize() throws IOException{
        MetadataEntry jsonString = new MetadataEntry("hello", "world");
        MetadataEntry jsonDecimal = new MetadataEntry("decimal", 1234.4321);
        MetadataEntry jsonInteger = new MetadataEntry("int", 1234);
        
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        AnnotationIntrospector pair = new AnnotationIntrospectorPair(primary, secondary);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector( pair );
        TypeReference singleRef = new TypeReference<MetadataEntry>(){};
        TypeReference compoundRef = new TypeReference<List<MetadataEntry>>(){};

        MetadataEntry entry;
        
        StringWriter output = new StringWriter();
        mapper.writeValue(output, jsonString);
        entry = mapper.readValue( output.toString(), singleRef);
        System.out.println(entry.getValue().getValue());
        System.out.println(jsonString.getValue().getClass().getSimpleName() + ":" + entry.getValue().getClass().getSimpleName());
        
        output.close();
        
        output = new StringWriter();
        mapper.writeValue(output, jsonDecimal);
        entry = mapper.readValue( output.toString(), singleRef);
        System.out.println(entry.getValue().getValue());
        System.out.println(jsonDecimal.getValue().getClass().getSimpleName() + ":" + entry.getValue().getClass().getSimpleName());
        output.close();
        
        output = new StringWriter();
        mapper.writeValue(output, jsonInteger);
        entry = mapper.readValue( output.toString(), singleRef);
        System.out.println(entry.getValue().getValue());
        System.out.println(jsonInteger.getValue().getClass().getSimpleName() + ":" + entry.getValue().getClass().getSimpleName());
        output.close();
        
    }

    public void testLong(){
        MetadataEntry e;
        Number n1 = 4294967296L;
        Number n2 = 4294967297L;
        Number n3 = 4294967295L;
        MetadataValue o;
        o = getEntry(n1);
        assertEquals(n1, o.getValue());
        o = getEntry(n2);
        assertEquals(n2, o.getValue());
        o = getEntry(n3);
        assertEquals(n3, o.getValue());

    }
    
    private MetadataValue getEntry(Number v){
        if(v instanceof Double || v instanceof java.math.BigDecimal || v instanceof Float){
            return new MetadataDecimal( v.doubleValue() );
        } else if(v instanceof Long || v instanceof java.math.BigInteger || v instanceof Integer){
            return new MetadataInteger( v.longValue() );
        }
        return null;
    }

}
