/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.rest.shared.metadata;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.srs.rest.shared.metadata.MetadataEntry.Builder;

/**
 *
 * @author bvan
 */
@XmlRootElement(name="entry")
@JsonTypeName("metadata")
@JsonPropertyOrder({"key","value"})
@JsonDeserialize(builder=Builder.class)
public class MetadataEntry {
    private String key; 
    private MetadataValue value;
    
    public MetadataEntry(){}
       
    public MetadataEntry(String k, MetadataValue v) {
       key = k;
       value = v;
    }
    
    public MetadataEntry(String k, Number v) {
       key = k;
       if(v instanceof Double || v instanceof java.math.BigDecimal || v instanceof Float){
          value = new MetadataDecimal(v.doubleValue());
       } else if(v instanceof Long || v instanceof java.math.BigInteger || v instanceof Integer){
           value = new MetadataInteger(v.longValue());
       }
    }
    
    public MetadataEntry(String k, String v) {
       key = k;
       value = new MetadataString(v);
    }
    
    @XmlElement
    public String getKey(){
        return key;
    }
    
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="$type", include=JsonTypeInfo.As.EXTERNAL_PROPERTY)
    @XmlElementRefs({
        @XmlElementRef(name = "decimal", type = MetadataDecimal.class),
        @XmlElementRef(name = "integer", type = MetadataInteger.class),
        @XmlElementRef(name = "string", type = MetadataString.class)})
    public MetadataValue getValue(){
        return value;
    }
    
    @XmlTransient
    public Object getRawValue(){
        return value.getValue();
    }
    
    @Override
    public String toString(){
        return String.format("\"%s\":%s", key, value);
    }
    
    @XmlTransient
    public static class Builder {
        protected String key;
        protected Object rawValue;
        protected String type;
        
        public Builder(){}
        public static Builder create(){return new Builder();}
        
        @JsonSetter
        public Builder key(String val){ this.key = val; return this; }
        
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
        public Builder $type(String val){
            this.type = val;
            return this;
        }
        
        public MetadataEntry build(){
            MetadataValue.Builder builder = new MetadataValue.Builder();
            builder.$type(type);
            builder.value( rawValue );
            return new MetadataEntry(key, builder.build());
        }
    }

}