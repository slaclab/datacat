package org.srs.datacat.shared;

import org.srs.datacat.model.RecordType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.model.HasMetadata;
import org.srs.datacat.shared.DatacatObject.Builder;
import org.srs.datacat.shared.metadata.MetadataEntry;

/**
 * The generalized base object for most of the database derived objects.
 *
 * @author bvan
 */
@JsonPropertyOrder({"_type", "name", "path", "pk", "parentPk", "metadata"})
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="_type", defaultImpl=DatacatObject.class)
@JsonSubTypes(value = {
        @JsonSubTypes.Type(LogicalFolder.class),
        @JsonSubTypes.Type(DatasetGroup.class),
        @JsonSubTypes.Type(Dataset.class),
        @JsonSubTypes.Type(FlatDataset.class),
        @JsonSubTypes.Type(FullDataset.class),
        @JsonSubTypes.Type(DatasetVersion.class),
        @JsonSubTypes.Type(DatasetLocation.class)
        }
    )
@JsonDeserialize(builder = Builder.class)
public class DatacatObject implements DatacatNode, HasMetadata {

    private Long pk;
    private Long parentPk;
    private String name;
    private String path;
    private String acl;
    private final HashMap<String, Object> metadata = new HashMap<>();


    // Default no-arg constructor needed for jaxb
    public DatacatObject(){
    }

    public DatacatObject(Long pk, Long parentPk, String name){
        this.pk = pk;
        this.parentPk = parentPk;
        this.name = name;
    }

    /**
     * Cloning constructor.
     *
     * @param object
     */
    public DatacatObject(DatacatObject object){
        this(object.pk, object.parentPk, object.name);
        this.path = object.path;
        if(object.metadata != null){
            this.metadata.putAll(object.metadata);
        }
        this.acl = object.acl;
    }

    public DatacatObject(Builder builder){
        this(builder.pk, builder.parentPk, builder.name);
        this.path = builder.path;
        if(builder.metadata != null){
            this.metadata.putAll(builder.metadata);
        }
        this.acl = builder.acl;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getName(){
        return name;
    }

    /**
     * Get the primary key for the database object in question. 
     * This will correspond to the object's primary key column.
     *
     * @return The primary key for the given object in the database.
     */
    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getPk(){
        return pk;
    }

    /**
     * Get the primary key for the database objects parent object.
     * Every object has a parent object.
     * Child : Parent
     * Folder : Folder
     * Group : Folder
     * Dataset : Group || Folder
     * DatasetVersion : Dataset
     * DatasetLocation : DatasetVersion
     *
     * @return The primary key for the given object's parent
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getParentPk(){
        // TODO: Should we return -1 if the parent isn't set?
        return parentPk;
    }

    /**
     * If the object has it's path information included, we will return it.
     * If not, we will return null.
     * 
     * @return The path of the object in the datacatalog as a string.
     * @return null
     */
    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getPath(){
        return path;
    }

    /**
     * Metadata to return.
     *
     * @return The metadata, each value should be the proper type (String,Number,etc..)
     */
    @JsonIgnore
    @Patchable
    public HashMap<String, Object> getMetadataMap(){
        return metadata;
    }

    /**
     * Convenience method to lump metadata into a list for easy serialization. 
     * This makes the xml and json output a lot cleaner.
     *
     * @return
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<MetadataEntry> getMetadata(){
        List<MetadataEntry> entries = MetadataEntry.toList(metadata);
        return entries.size() > 0 ? entries : null;
    }

    @JsonIgnore
    public RecordType getType(){
        return RecordType.typeOf(this);
    }

    @JsonIgnore
    public String getJsonTypeName(){
        JsonTypeName t = getClass().getAnnotation(JsonTypeName.class);
        return t.value();
    }
    
    @Override
    @JsonIgnore
    @Patchable(column="ACL")
    public String getAcl(){
        return this.acl;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(omitNull("Name: ", name));
        sb.append(omitNull("Path: ", getPath()));
        List me = getMetadata();
        if(me != null && !me.isEmpty()){
            sb.append("Metadata: {");
            for(Iterator iter = me.iterator(); iter.hasNext();){
                sb.append(iter.next()).append(iter.hasNext() ? ", " : "");
            }
            sb.append("}");
        }
        return sb.toString();
    }

    protected String omitNull(String key, Object o){
        return o != null ? key + o + "\t" : "";
    }

    /**
     *
     * @author bvan
     */
    public static class Builder<U extends DatacatNodeBuilder> implements DatacatNodeBuilder<U> {
        public String name;
        public Long pk;
        public Long parentPk;
        public RecordType parentType = null;
        public String path;
        public RecordType type;
        public String acl;
        public HashMap<String, Object> metadata = new HashMap<>();

        public Builder(){
            super();
        }
        
        public Builder(DatacatRecord object){
            this();
            
            this.pk = object.getPk();
            
            this.path = object.getPath();
            this.type = object.getType();
            if(object instanceof DatacatNode){
                this.name = ((DatacatNode) object).getName();
                this.parentPk = ((DatacatNode) object).getParentPk();
                this.acl = ((DatacatNode) object).getAcl();
            }
            if(object instanceof DatacatObject){
                this.metadata = ((DatacatObject) object).getMetadataMap();
            }
        }

        public Builder(DatacatObject object){
            this();
            this.name = object.getName();
            this.pk = object.getPk();
            this.parentPk = object.getParentPk();
            this.path = object.getPath();
            this.type = object.getType();
            this.metadata = object.metadata;
            this.acl = object.acl;
        }

        public Builder(Builder builder){
            this();
            this.name = builder.name;
            this.pk = builder.pk;
            this.parentPk = builder.parentPk;
            this.path = builder.path;
            this.type = builder.type;
            this.metadata = builder.metadata;
            this.acl = builder.acl;
        }

        public Builder(RecordType type){
            this();
            this.type = type;
        }
        
        @Override
        public DatacatObject build(){
            if(type == null){
                return new DatacatObject(this);
            }
            switch(type){
                case FOLDER:
                    return new LogicalFolder(this);
                case GROUP:
                    return new DatasetGroup(this);
                case DATASET:
                    return new Dataset(this);
                default:
                    return new DatacatObject(this);
            }
        }
        
        public U create(DatacatNode val){
            return (U) new Builder(val);
        }

        @JsonSetter
        @Override
        public U name(String val){
            this.name = val;
            return (U) this;
        }

        @Override
        public U type(RecordType dType){
            this.type = dType;  
            return (U) this;
        }

        @JsonSetter(value = "type")
        public U jsonType(String val){
            this.type = RecordType.fromJsonType(val);
            return (U) this;
        }

        @JsonSetter
        @Override
        public U pk(Long val){
            this.pk = val;
            return (U) this;
        }

        @JsonSetter
        @Override
        public U parentPk(Long val){
            this.parentPk = val;
            return (U) this;
        }

        @JsonSetter
        public U parentType(RecordType val){
            this.parentType = val;
            return (U) this;
        }

        @JsonSetter
        @Override
        public U path(String val){
            this.path = val;
            return (U) this;
        }

        @JsonSetter
        public U metadata(List<MetadataEntry> val){
            this.metadata = new HashMap<>();
            if(val != null){
                for(MetadataEntry e: val){
                    if(e.getRawValue() instanceof Number){
                        metadata.put(e.getKey(), (Number) e.getRawValue());
                    } else if(e.getRawValue() instanceof Timestamp) {
                        metadata.put(e.getKey(), (Timestamp) e.getRawValue());
                    } else {
                        metadata.put(e.getKey(), (String) e.getRawValue());
                    }
                }
            }
            return (U) this;
        }

        @Override
        public U metadata(Map<String, Object> val){
            this.metadata = new HashMap<>();
            if(val != null){
                metadata.putAll(val);
            }
            return (U) this;
        }

        @Override
        public U acl(String val){
            this.acl = val;
            return (U) this;
        }
        
        protected Timestamp checkTime(Timestamp ts){
            return ts != null ? new Timestamp(ts.getTime()) : null;
        }
    }
    
}
