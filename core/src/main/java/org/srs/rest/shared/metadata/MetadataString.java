package org.srs.rest.shared.metadata;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.srs.rest.shared.metadata.MetadataString.Builder;

/**
 *
 * @author bvan
 */
@JsonTypeName(value = "string")
@JsonDeserialize(builder = Builder.class)
public class MetadataString implements MetadataValue<String> {
    private String value;

    public MetadataString(){}

    public MetadataString(String value){ this.value = value; }

    @Override
    @JsonValue
    public String getValue(){ return value; } 

    @Override
    public String toString(){
        return String.format("string(\"%s\")", value);
    }

    /**
     * Builder.
     */
    public static class Builder extends MetadataValue.Builder<MetadataString> {
        public Builder(){}

        public Builder(String val){
            super.value(val);
        }

        @Override
        public MetadataString build(){
            return super.build();
        }

    }
}
