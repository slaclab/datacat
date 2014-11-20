/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.rest.shared.metadata;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.bind.annotation.XmlTransient;
import org.srs.rest.shared.metadata.MetadataValue.Builder;

/**
 * Generic class to help with encapsulation of metadata values.
 * @author bvan
 */
@JsonDeserialize(builder = Builder.class)
public interface MetadataValue<T> {
    T getValue();

    /**
     * Builder.
     */
    @XmlTransient
    public static class Builder<T extends MetadataValue> {
        protected Object rawValue;
        protected String type;

        public Builder(){}

        public Builder(Number val){ this.rawValue = val; }

        public Builder(Double val){ this.rawValue = val; }

        public Builder(java.math.BigDecimal val){ this.rawValue = val; }

        public Builder(Long val){ this.rawValue = val; }

        public Builder(java.math.BigInteger val){ this.rawValue = val; }

        public Builder(String val){ this.rawValue = val; }

        public T build(){
            if(type != null){
                if(type.equalsIgnoreCase("decimal")){
                    if(rawValue instanceof Number){
                        return (T) new MetadataDecimal((Number) rawValue);
                    } else {
                        return (T) new MetadataDecimal(new BigDecimal(rawValue.toString()));
                    }
                } else if(type.equalsIgnoreCase("integer")){
                    if(rawValue instanceof Number){
                        return (T) new MetadataInteger((Number) rawValue);
                    } else {
                        return (T) new MetadataInteger(new BigInteger(rawValue.toString()));
                    }
                } else {
                    return (T) new MetadataString(rawValue.toString());
                }
            }
            if(rawValue instanceof Long || rawValue instanceof BigInteger || rawValue instanceof Integer){
                return (T) new MetadataInteger((Number) rawValue);
            }
            if(rawValue instanceof Number){
                return (T) new MetadataDecimal((Number) rawValue);
            }
            return (T) new MetadataString(rawValue.toString());
        }

        @JsonSetter
        public Builder value(Object val){ this.rawValue = val; return this; }

        @JsonSetter(value="$type")
        public Builder type(String val){ this.type = val; return this; }

    }
}
