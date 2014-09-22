
package org.srs.rest.shared.metadata;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;
import org.srs.rest.shared.LongSerializer;
import org.srs.rest.shared.metadata.MetadataInteger.Builder;

/**
 *
 * @author bvan
 */
@XmlRootElement(name="integer")
@JsonTypeName(value="integer")
@JsonDeserialize(builder=Builder.class)
public class MetadataInteger implements MetadataValue<Number> {
    private java.math.BigInteger value;

    public MetadataInteger(){}
    
    public MetadataInteger(java.math.BigInteger value){this.value = value;}
    
    public MetadataInteger(Number value){
        if(value instanceof java.math.BigInteger){
            this.value = (java.math.BigInteger) value;
        } else {
            this.value = java.math.BigInteger.valueOf( value.longValue() );
        }
    }
    
    @Override
    public String toString(){
        return String.format( "integer(\"%d\")", value);
    }
    
    @XmlValue
    @JsonValue
    // TODO: Fix this to be BigInteger if https://github.com/FasterXML/jackson-databind/issues/466 gets fixed.
    @JsonSerialize(using=LongSerializer.class)
    public Long getValue(){return value.longValue(); }
    
    @XmlTransient
    public static class Builder extends MetadataValue.Builder<MetadataInteger> {
        public Builder(){}
        
        public Builder(java.math.BigInteger value){ this.rawValue = value; }
        public Builder(Long value){ this.rawValue = java.math.BigInteger.valueOf( value ); }
        
        public MetadataInteger build(){ return new MetadataInteger( (Number) rawValue); }
    }
    
}
