
package org.srs.datacat.shared.metadata;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import org.srs.datacat.shared.adapters.RestDateAdapter;
import org.srs.datacat.shared.metadata.MetadataValue.Builder;

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
    public static class Builder<T extends MetadataValue> {
        protected Object rawValue;
        protected String type;

        public Builder(){}

        public Builder(Number val){ this.rawValue = val; }

        public Builder(Double val){ this.rawValue = val; }

        public Builder(java.math.BigDecimal val){ this.rawValue = val; }

        public Builder(Long val){ this.rawValue = val; }

        public Builder(java.math.BigInteger val){ this.rawValue = val; }
        
        public Builder(Timestamp val){ this.rawValue = val; }

        public Builder(String val){ this.rawValue = val; }

        public T build(){
            if(type != null){
                if(type.equalsIgnoreCase("decimal")){
                    if(rawValue instanceof Number){
                        return (T) new MetadataDecimal((Number) rawValue);
                    } else {
                        return (T) new MetadataDecimal(new BigDecimal((String) rawValue));
                    }
                } else if(type.equalsIgnoreCase("integer")){
                    if(rawValue instanceof Number){
                        return (T) new MetadataInteger((Number) rawValue);
                    } else {
                        return (T) new MetadataInteger(new BigInteger((String) rawValue));
                    }
                } else if(type.equalsIgnoreCase("timestamp")){
                    if(rawValue instanceof Timestamp){
                        return (T) new MetadataTimestamp((Timestamp) rawValue);
                    } else {
                        return (T) new MetadataTimestamp(new RestDateAdapter().unmarshal((String) rawValue));
                    }
                } else {
                    return (T) new MetadataString((String) rawValue);
                }
            }
            if(rawValue instanceof Long || rawValue instanceof BigInteger || rawValue instanceof Integer){
                return (T) new MetadataInteger((Number) rawValue);
            }
            if(rawValue instanceof Number){
                return (T) new MetadataDecimal((Number) rawValue);
            }
            return (T) new MetadataString((String) rawValue);
        }

        @JsonSetter
        public Builder value(Object val){ this.rawValue = val; return this; }

        @JsonSetter(value="type")
        public Builder type(String val){ this.type = val; return this; }

    }
}
