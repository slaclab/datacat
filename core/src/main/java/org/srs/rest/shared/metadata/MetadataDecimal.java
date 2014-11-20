
package org.srs.rest.shared.metadata;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;
import org.srs.rest.shared.metadata.MetadataDecimal.Builder;

/**
 *
 * @author bvan
 */
@XmlRootElement(name = "decimal")
@JsonTypeName(value = "decimal")
@JsonDeserialize(builder=Builder.class)
public class MetadataDecimal implements MetadataValue<Number> {
    private java.math.BigDecimal value;

    public MetadataDecimal(){ }

    public MetadataDecimal(java.math.BigDecimal value){ this.value = value; }

    public MetadataDecimal(Number value){ 
        if(value instanceof java.math.BigDecimal){
            this.value = (java.math.BigDecimal) value;
        } else {
            this.value = java.math.BigDecimal.valueOf( value.doubleValue() );
        }
    }

    @Override
    public String toString(){
        return String.format( "decimal(\"%s\")", value.toString());
    }

    @XmlValue
    @JsonValue
    // TODO: Fix this to be BigDecimal if https://github.com/FasterXML/jackson-databind/issues/466 gets fixed.
    public Double getValue(){ return value.doubleValue(); }
    
    /**
     * Builder.
     */
    @XmlTransient
    public static class Builder extends MetadataValue.Builder<MetadataDecimal> {
        public Builder(){}
        
        public Builder(java.math.BigDecimal value){ this.rawValue = value; }
        public Builder(Double value){ this.rawValue = java.math.BigDecimal.valueOf( value ); }
        
        public MetadataDecimal build(){ return new MetadataDecimal( (Number) rawValue); }
    }
}
