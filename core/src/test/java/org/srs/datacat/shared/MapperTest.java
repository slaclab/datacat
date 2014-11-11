
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import org.srs.datacat.shared.LogicalFolder;
import org.srs.datacat.shared.DatacatObject;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import java.io.IOException;
import java.io.StringWriter;
import junit.framework.TestCase;
import org.srs.datacat.shared.dataset.FlatDataset;
import org.srs.rest.shared.metadata.MetadataEntry;
import org.srs.rest.shared.metadata.MetadataValue;

/**
 *
 * @author bvan
 */
public class MapperTest extends TestCase {
    String jsonText = "{\"$type\":\"folder\",\"name\":\"Flight\","
            + "\"path\":\"/Data\",\"pk\":4321,\"parentPk\":1234}";
    
    public MapperTest(){ }
    
    public void testDeserialization() throws IOException {
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        AnnotationIntrospector pair = new AnnotationIntrospectorPair(primary, secondary);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector( pair );
        JsonSchemaGenerator generator = new JsonSchemaGenerator(mapper);
        JsonSchema jsonSchema = generator.generateSchema(FlatDataset.class);
        
        DatacatObject o = mapper.readValue( jsonText, LogicalFolder.class );
        StringWriter writ = new StringWriter();
        mapper.writeValue( writ, o );
        String actual = writ.toString();
        assertEquals("JSON string representation mismatch",jsonText,actual);

        //System.out.println(mapper.writeValueAsString(generator.generateSchema(DatacatObject.class)));
        
        System.out.print(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(generator.generateSchema( DatacatObject.class)));
        
        System.out.println(",");
        for(JsonSubTypes.Type t:DatacatObject.class.getAnnotation(JsonSubTypes.class).value()){
            System.out.print(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(generator.generateSchema(t.value())));
            System.out.println(",");
        }
        System.out.print(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(generator.generateSchema( MetadataEntry.class)));
        System.out.print(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(generator.generateSchema( MetadataValue.class)));
        //System.out.println(mapper.writeValueAsString(generator.generateSchema(DatasetGroup.class)));
        //System.out.println(mapper.writeValueAsString(generator.generateSchema(LogicalFolder.class)));
        //System.out.println(mapper.writeValueAsString(generator.generateSchema(MetadataEntry.class)));
        //System.out.println(mapper.writeValueAsString(generator.generateSchema(MetadataValue.class)));

    }
}
