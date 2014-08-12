
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.shared.DatacatObject.Type;
import org.srs.datacat.shared.container.BasicStat;
import org.srs.rest.shared.metadata.MetadataEntry;

/**
 *
 * @author bvan
 */
public class DatacatObjectBuilder<T extends DatacatObject, U extends DatacatObjectBuilder> {
    public String name;
    public Long pk;
    public Long parentPk = null;
    public Type parentType = null;
    public String path;
    public String type;
    public List<MetadataEntry> metadata;
    public Map<String,Number> numberMetadata;
    public Map<String,String> stringMetadata;
    
    public DatacatObjectBuilder(){ }
    
    public DatacatObjectBuilder(DatacatObject object){
        this();
        this.name = object.getName();
        this.pk = object.getPk();
        this.parentPk = object.getParentPk();
        this.path = object.getPath();
        this.type = object.getType();
        this.stringMetadata = object.getStringMetadata();
        this.numberMetadata = object.getNumberMetadata();
    }
    
    public DatacatObjectBuilder(DatacatObjectBuilder builder){
        this();
        this.name = builder.name;
        this.pk = builder.pk;
        this.parentPk = builder.parentPk;
        this.path = builder.path;
        this.type = builder.type;
        this.metadata = builder.metadata;
        this.numberMetadata = builder.numberMetadata;
        this.stringMetadata = builder.stringMetadata;
    }
    
    public DatacatObjectBuilder(Type type){
        this();
        this.type = type != null ? type.toString() : null;
    }

    public static DatacatObjectBuilder create(DatacatObject object){
        return new DatacatObjectBuilder()
                .pk( object.getPk() )
                .parentPk( object.getParentPk() )
                .name( object.getName() )
                .path( object.getPath() )
                .stringMetadata( object.getStringMetadata())
                .numberMetadata( object.getNumberMetadata() )
                .type( DatacatObject.Type.typeOf(object).toString() );
    }

    public T build(){
        return (T) buildType(type);
    }
    
    public DatacatObject buildType(String type){
        switch (Type.valueOf(type)){
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
    
    @JsonSetter
    public U name(String val){
        this.name = val;
        return (U) this;
    }

    @JsonSetter
    public U type(String val){
        this.type = val;
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
        this.metadata = val;
        return (U) this;
    }
    
    public U metadata(Map<String,Object> val){
        this.metadata = new ArrayList<>();
        for(Map.Entry<String,Object> e: val.entrySet()){
            if(e.getValue() instanceof Number){
                metadata.add( new MetadataEntry(e.getKey(), (Number) e.getValue()));
            } else {
                metadata.add( new MetadataEntry(e.getKey(), e.getValue().toString()));
            }
        }
        return (U) this;
    }
    
    public U numberMetadata(Map<String, Number> val){
        this.numberMetadata = val;
        return (U) this;
    }
    
    public U stringMetadata(Map<String, String> val){
        this.stringMetadata = val;
        return (U) this;
    }

    public static class DatasetContainerBuilder<T extends DatacatObject> extends DatacatObjectBuilder<T, DatasetContainerBuilder>{
        public BasicStat stat = null;
        public String description = null;
        public DatasetContainerBuilder(){super();}
        public DatasetContainerBuilder(DatacatObject object){
            super(create(object));
            if(object instanceof DatasetContainer){
                this.description = ((DatasetContainer) object).getDescription();
            }
        }
        
        @JsonSetter
        public DatasetContainerBuilder stat(BasicStat val) {this.stat = val; return this; }
        @JsonSetter
        public DatasetContainerBuilder description(String val) {this.description = val; return this; }

        @Override
        public DatacatObject buildType(String type){
            switch(Type.valueOf( type )){
                case FOLDER:
                    return new LogicalFolder( this );
                case GROUP:
                    return new DatasetGroup( this );
                default:
                    return new DatacatObject( this );
            }
        }
    }

}
