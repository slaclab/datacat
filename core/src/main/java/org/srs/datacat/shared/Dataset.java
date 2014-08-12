
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.srs.datacat.model.DatasetModel;
import java.sql.Timestamp;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlTransient;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.dataset.DatasetBuilder;
import org.srs.rest.shared.RestDateAdapter;

/**
 * Represents an entire dataset. May include information on all datasetversions, 
 * @author bvan
 */
@XmlRootElement
@XmlType(name="dataset")
@JsonTypeName(value="dataset")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="$type", defaultImpl=Dataset.class)
@JsonDeserialize(builder=DatasetBuilder.class)
public class Dataset extends DatacatObject implements DatasetModel {
    
    private String datasetFileFormat;
    private String datasetDataType;
    private Timestamp dateCreated;
    
    public Dataset(){ super(); }
    
    public Dataset(DatacatObject o){ super(o); }
    
    /**
     * Copy constructor
     * @param dataset 
     */
    public Dataset(Dataset dataset){
        super(dataset);
        this.datasetDataType = dataset.datasetDataType;
        this.datasetFileFormat = dataset.datasetFileFormat;
        this.dateCreated = dataset.dateCreated;
    }
    
    public Dataset(DatasetBuilder builder){
        super(builder);
        this.datasetDataType = builder.datasetDataType;
        this.datasetFileFormat = builder.fileFormat;
        this.dateCreated = builder.created;
    }
    
    public Dataset(DatacatObjectBuilder builder){
        super(builder);
    }
    
    @Override
    @XmlElement(required=false)
    public String getDataType() { return this.datasetDataType;}
        
    @Override
    @XmlElement(required=false)
    public String getFileFormat() { return this.datasetFileFormat;}
    
    @Override
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    @XmlElement(name="registered", required=false)
    public Timestamp getDateCreated(){ return this.dateCreated; }
    
    @Override
    @XmlTransient
    public Timestamp getDateModified(){
        return null;
    }
    
    @Override
    public String toString() {
        return super.toString() + "\tType: " + datasetDataType + "\tRegistered: " + dateCreated;
    }

    @Override
    public boolean weakEquals(Object obj){
       if(obj == null || getClass().isAssignableFrom(obj.getClass())){
            return false;
        }
        if(!super.weakEquals( obj )){
            return false;
        }
        Dataset other = (Dataset) obj;
        if(!weakEquivalence( this.datasetFileFormat, other.datasetFileFormat )){
            return false;
        }
        if(!weakEquivalence( this.datasetDataType, other.datasetDataType )){
            return false;
        }
        if(!weakEquivalence( this.dateCreated, other.dateCreated )){
            return false;
        }
        return true;
    }
    
    public void validateFields(){
        Objects.requireNonNull(datasetFileFormat, "Dataset file format is required");
        Objects.requireNonNull(datasetDataType, "Dataset data type is required");
    }
    
    @XmlTransient
    public List<DatasetView> getDatasetViews(){
        return Collections.singletonList(DatasetView.EMPTY);
    }
}
