
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.srs.datacat.shared.DatacatObject.Builder;
import org.srs.datacat.shared.dataset.FlatDataset;
import org.srs.datacat.shared.dataset.FullDataset;
import org.srs.datacat.shared.dataset.VersionWithLocations;
import org.srs.rest.shared.metadata.MetadataEntry;

/**
 * The generalized base object for most of the database derived objects.
 * @author bvan
 */
@XmlRootElement
@XmlSeeAlso({LogicalFolder.class, DatasetGroup.class, 
    Dataset.class, FlatDataset.class, FullDataset.class,
    DatasetVersion.class, VersionWithLocations.class, DatasetLocation.class})
@XmlType(propOrder={"name","path","pk","parentPk","metadata"})
@JsonPropertyOrder({"name","path","pk","parentPk","metadata"})
// Use default implementation of DatacatObject
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="$type", defaultImpl=DatacatObject.class)
@JsonSubTypes(value = {
    @JsonSubTypes.Type(LogicalFolder.class), @JsonSubTypes.Type(DatasetGroup.class),
    @JsonSubTypes.Type(Dataset.class), 
    @JsonSubTypes.Type(FlatDataset.class),
    @JsonSubTypes.Type(FullDataset.class),
    @JsonSubTypes.Type(DatasetVersion.class),
    @JsonSubTypes.Type(VersionWithLocations.class),
    @JsonSubTypes.Type(DatasetLocation.class)
})
@JsonDeserialize(builder=Builder.class)
public class DatacatObject implements Serializable {

    private Long pk;
    private Long parentPk;
    private String name;
    private String path;
    private HashMap<String, Object> metadata = null;

    @XmlEnum(String.class)
    public enum Type {
        DATASET,
        DATASETLOCATION,
        DATASETVERSION,
        FOLDER,
        GROUP;
        
        public boolean isContainer(){
            return this == FOLDER || this == GROUP;
        }
        
        public static Type typeOf(DatacatObject object){
            if(object instanceof LogicalFolder)
                return FOLDER;
            if(object instanceof Dataset)
                return DATASET;
            if(object instanceof DatasetGroup)
                return GROUP;
            return null;
        }        
        
        public static Type fromJsonType(String jsonType){
            jsonType = jsonType == null ? "" : jsonType;
            switch(jsonType){
                case "folder":
                    return Type.FOLDER;
                case "group":
                    return Type.GROUP;
                case "dataset":
                case "dataset#flat":
                case "dataset#full":
                    return Type.DATASET;
                default:
                    return null;
            }
        }
    }
    
    // Default no-arg constructor needed for jaxb
    public DatacatObject(){}
    
    public DatacatObject(Long pk, Long parentPk, String name){
        this.pk = pk;
        this.parentPk = parentPk;
        this.name = name;
    }
    
    /**
     * Cloning constructor
     * @param object 
     */
    public DatacatObject(DatacatObject object){
        this(object.pk, object.parentPk, object.name);
        this.path = object.path;
        this.metadata = object.metadata != null ? object.metadata : new HashMap<String,Object>();
    }
    
    public DatacatObject(Builder builder){
        this(builder.pk, builder.parentPk, builder.name);
        this.path = builder.path;
        this.metadata = builder.metadata != null ? builder.metadata : new HashMap<String,Object>();
    }
    
    @XmlElement(required=false)
    public String getName(){
        return name;
    }
    
    /**
     * Get the primary key for the database object in question.
     * This will correspond to the object's primary key column.
     * @return The primary key for the given object in the database.
     */
    @XmlElement(required=false)
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
    @XmlElement(required=false)
    public Long getParentPk(){
        // TODO: Should we return -1 if the parent isn't set?
        return parentPk;
    }

    /**
     * If the object has it's path information included, we will return it.
     * If not, we will return null.
     * @return The path of the object in the datacatalog as a string.
     * @return null
     */
    @XmlElement(required=false)
    public String getPath(){
        return path;
    }

     /**
     * Metadata to return 
     * @return The meatadata with values of Number types
     */
    @XmlTransient
    public HashMap<String,Object> getMetadataMap() {
        return metadata;
    }
    
    /**
     * Convenience method to lump metadata into a list for easy serialization.
     * This makes the xml and json output a lot cleaner.
     * @return 
     */
    @XmlElementWrapper(name="metadata")
    @XmlElement(required=false, name="entry")
    @JsonProperty("metadata")
    public List<MetadataEntry> getMetadata() {
        ArrayList<MetadataEntry> entries = new ArrayList<>();
        for(Entry<String, Object> e: metadata.entrySet()){
            if(e.getValue() instanceof Number){
                entries.add( new MetadataEntry( e.getKey(), (Number) e.getValue()) );
            } else if (e.getValue() instanceof String){
                entries.add( new MetadataEntry(e.getKey(), (String) e.getValue()));
            }
        }
        return entries.size() > 0 ? entries : null;
    }
    
    @XmlTransient
    public Type getType(){
        return Type.typeOf( this );
    }
    
    @XmlTransient
    public String getXmlTypeName(){
        XmlType t = getClass().getAnnotation( XmlType.class );
        return t.name();
    }
    
    @XmlTransient
    public String getJsonTypeName(){
        JsonTypeName t = getClass().getAnnotation( JsonTypeName.class );
        return t.value();
    }
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append( omitNull("Name: ", name));
        sb.append( omitNull("Path: ", getPath()));
        List me = getMetadata();
        if(me != null && !me.isEmpty()){
            sb.append("Metadata: {");
            for(Iterator iter = me.iterator(); iter.hasNext();){
                sb.append(iter.next()).append( iter.hasNext()? ", " :"");
            }
            sb.append( "}");
        }
        return sb.toString();
    }

    protected String omitNull(String name, Object o){
        return o != null ? name + o + "\t": "";
    }

    /**
     * Checks to make sure any fields that are declared are equivalent
     * @param obj
     * @return 
     */
    public boolean weakEquals(Object obj){
        if(obj == null){
            return false;
        }
        if(getClass() != obj.getClass()){
            return false;
        }
        final DatacatObject other = (DatacatObject) obj;
        if(!weakEquivalence(this.pk, other.pk)){
            return false;
        }
        if(!weakEquivalence(this.parentPk, other.parentPk)){
            return false;
        }
        if(!weakEquivalence(this.name, other.name)){
            return false;
        }
        if(!weakEquivalence(this.path, other.path)){
            return false;
        }
        return true;
    }
    
    public static boolean weakEquivalence(Object source, Object target){
        if(target == null || Objects.equals(source, target)){
            return true;
        }
        return false;
    }
    
    /**
     *
     * @author bvan
     */
    @XmlTransient
    public static class Builder<U extends Builder> {
        public String name;
        public Long pk;
        public Long parentPk = null;
        public Type parentType = null;
        public String path;
        public Type type;
        public HashMap<String, Object> metadata;

        public Builder(){
            super();
        }

        public Builder(DatacatObject object){
            this();
            this.name = object.getName();
            this.pk = object.getPk();
            this.parentPk = object.getParentPk();
            this.path = object.getPath();
            this.type = object.getType();
            this.metadata = object.metadata;
        }

        public Builder(Builder builder){
            this();
            this.name = builder.name;
            this.pk = builder.pk;
            this.parentPk = builder.parentPk;
            this.path = builder.path;
            this.type = builder.type;
            this.metadata = builder.metadata;
        }

        public Builder(Type type){
            this();
            this.type = type;
        }

        public DatacatObject build(){
            if(type == null){
                return new DatacatObject( this );
            }
            switch(type){
                case FOLDER:
                    return new LogicalFolder( this );
                case GROUP:
                    return new DatasetGroup( this );
                case DATASET:
                    return new Dataset( this );
                default:
                    return new DatacatObject( this );
            }
        }

        @JsonSetter
        public U name(String val){
            this.name = val;
            return (U) this;
        }

        public U type(Type type){
            this.type = type;
            return (U) this;
        }

        @JsonSetter(value = "type")
        public U jsonType(String val){
            this.type = Type.fromJsonType( val );
            return (U) this;
        }

        @JsonSetter
        public U pk(Long val){
            this.pk = val;
            return (U) this;
        }

        @JsonSetter
        public U parentPk(Long val){
            this.parentPk = val;
            return (U) this;
        }

        @JsonSetter
        public U parentType(Type val){
            this.parentType = val;
            return (U) this;
        }

        @JsonSetter
        public U path(String val){
            this.path = val;
            return (U) this;
        }

        @JsonSetter
        public U metadata(List<MetadataEntry> val){
            this.metadata = new HashMap<>();
            for(MetadataEntry e: val){
                if(e.getRawValue() instanceof Number) {
                    metadata.put(e.getKey(), (Number)e.getRawValue());
                } else {
                    metadata.put(e.getKey(), (String)e.getRawValue());
                }
            }
            return (U) this;
        }

        public U metadata(Map<String, Object> val){
            this.metadata = new HashMap<>();
            metadata.putAll( val );
            return (U) this;
        }

    }
    
    
}
