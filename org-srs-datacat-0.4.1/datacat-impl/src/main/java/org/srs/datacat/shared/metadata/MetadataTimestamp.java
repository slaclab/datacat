package org.srs.datacat.shared.metadata;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.sql.Timestamp;
import org.srs.datacat.shared.adapters.RestDateAdapter;
import org.srs.datacat.shared.metadata.MetadataTimestamp.Builder;

/**
 *
 * @author bvan
 */
@JsonTypeName(value = "timestamp")
@JsonDeserialize(builder = Builder.class)
public class MetadataTimestamp implements MetadataValue<Timestamp> {
    private Timestamp value;

    public MetadataTimestamp(){}

    public MetadataTimestamp(Timestamp value){ this.value = value; }

    @Override
    @JsonValue
    public Timestamp getValue(){ return value; } 

    @Override
    public String toString(){
        return String.format("timestamp(\"%s\")", new RestDateAdapter().marshal(value));
    }

    /**
     * Builder.
     */
    public static class Builder extends MetadataValue.Builder<MetadataTimestamp> {
        public Builder(){}

        public Builder(String val){
            super.value(val);
        }

        @Override
        public MetadataTimestamp build(){
            return super.build();
        }

    }
}
