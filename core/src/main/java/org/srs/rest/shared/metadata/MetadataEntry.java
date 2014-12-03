package org.srs.rest.shared.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import org.srs.rest.shared.metadata.MetadataEntry.Builder;

/**
 *
 * @author bvan
 */
@JsonTypeName("metadata")
@JsonPropertyOrder({"key", "value"})
@JsonDeserialize(builder = Builder.class)
public class MetadataEntry {
    private String key;
    private MetadataValue value;

    public MetadataEntry(){}

    public MetadataEntry(String k, MetadataValue v){
        key = k;
        value = v;
    }

    public static List<MetadataEntry> toList(HashMap<String, Object> map){
        ArrayList<MetadataEntry> list = new ArrayList<>();
        for(Entry<String, Object> e: map.entrySet()){
            if(e.getValue() instanceof Number){
                list.add(new MetadataEntry(e.getKey(), (Number) e.getValue()));
            } else {
                list.add(new MetadataEntry(e.getKey(), e.getValue().toString()));
            }
        }
        return list;
    }

    public MetadataEntry(String k, Number v){
        key = k;
        if(v instanceof Double || v instanceof java.math.BigDecimal || v instanceof Float){
            value = new MetadataDecimal(v.doubleValue());
        } else if(v instanceof Long || v instanceof java.math.BigInteger || v instanceof Integer){
            value = new MetadataInteger(v.longValue());
        }
    }

    public MetadataEntry(String k, String v){
        key = k;
        value = new MetadataString(v);
    }

    @JsonProperty
    public String getKey(){
        return key;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
    public MetadataValue getValue(){
        return value;
    }

    @JsonIgnore
    public Object getRawValue(){
        return value.getValue();
    }

    @Override
    public String toString(){
        return String.format("\"%s\":%s", key, value);
    }
    
    /**
     * Builder.
     */
    public static class Builder {
        protected String key;
        protected Object rawValue;
        protected String type;

        public Builder(){}

        public static Builder create(){
            return new Builder();
        }

        @JsonSetter
        public Builder key(String val){
            this.key = val;
            return this;
        }

        @JsonSetter
        public Builder value(Object val){
            if(val instanceof MetadataValue){
                rawValue = ((MetadataValue) val).getValue();
            } else {
                rawValue = val;
            }
            return this;
        }

        @JsonSetter
        public Builder type(String val){
            this.type = val;
            return this;
        }

        public MetadataEntry build(){
            MetadataValue.Builder builder = new MetadataValue.Builder();
            builder.type(type);
            builder.value(rawValue);
            return new MetadataEntry(key, builder.build());
        }
    }

}
