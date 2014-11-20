package org.srs.rest.shared;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.io.IOException;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Need this because jackson doesn't handle boxed Long values well.
 * @author bvan
 */
@XmlTransient
public final class LongSerializer extends StdScalarSerializer<Long> implements ContextualSerializer {
    private final JsonParser.NumberType numberType;
    private final String schemaType;

    public LongSerializer(){
        super(Long.class);
        this.numberType = JsonParser.NumberType.BIG_INTEGER;
        this.schemaType = "number";
    }

    public void serialize(Double value, JsonGenerator jgen, SerializerProvider provider) throws IOException{
        jgen.writeNumber(value.longValue());
    }

    // IMPORTANT: copied from `NonTypedScalarSerializerBase`
    public void serializeWithType(Double value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer) throws IOException{
        // no type info, just regular serialization
        serialize(value, jgen, provider);
    }

    @Override
    public void serialize(Long value, JsonGenerator jgen, 
            SerializerProvider provider) throws IOException, JsonGenerationException{
        jgen.writeNumber(value.longValue());
    }

    public JsonNode getSchema(SerializerProvider provider, JsonSubTypes.Type typeHint){
        return createSchemaNode(schemaType, true);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, 
            JavaType typeHint) throws JsonMappingException{
        JsonIntegerFormatVisitor v2 = visitor.expectIntegerFormat(typeHint);
        if(v2 != null){
            v2.numberType(numberType);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, 
            BeanProperty property) throws JsonMappingException{
        if(property != null){
            JsonFormat.Value format = prov.getAnnotationIntrospector().
                    findFormat(property.getMember());
            if(format != null){
                switch(format.getShape()){
                    case STRING:
                        return ToStringSerializer.instance;
                    default:
                }
            }
        }
        return this;
    }

}
