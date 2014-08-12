
package org.srs.datacat.shared.dataset;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.dataset.FullDataset.Builder;

/**
 * Dataset with a Version, which also must contain locations
 * @author bvan
 */
@XmlRootElement
@XmlType(name="fullDataset")
@JsonTypeName(value="dataset#full")
@JsonDeserialize(builder = Builder.class)
public class FullDataset extends Dataset {
    private DatasetVersion dsVersion;
    
    private FullDataset(){}
    
    /**
     * Copy constructor. Can convert other datasets, if needed.
     * @param dataset 
     */
    public FullDataset(Dataset dataset){
        super(dataset);
        if(dataset instanceof FullDataset){
            FullDataset ds = ((FullDataset) dataset);
            if(ds.dsVersion instanceof VersionWithLocations){
                this.dsVersion = new VersionWithLocations(ds.dsVersion);
            } else {
                this.dsVersion = new DatasetVersion(ds.dsVersion);
            }
        }
        else if(dataset instanceof FlatDataset){
            FlatDataset ds = ((FlatDataset) dataset);
            if(ds.getVersion() instanceof VersionWithLocations){
                this.dsVersion = new VersionWithLocations(ds.getVersion());
            } else {
                this.dsVersion = new DatasetVersion(ds.getVersion());
            }
        }
    }

    public FullDataset(DatasetBuilder builder){
        super(builder);
        dsVersion = builder.version;
    }
    
    @XmlElement(name="version", required=false)
    public DatasetVersion getVersion(){
        return dsVersion;
    }
    
    
    @XmlTransient
    public static class Builder extends DatasetBuilder<FullDataset>{
        
        public Builder(){}
        
        public Builder(DatasetBuilder builder){
            super(builder);
        }
        
        @Override
        public FullDataset build(){
            if(version == null){
                VersionWithLocations dv = null;
                if((dsType & VERSION) > 0){
                    dv = new VersionWithLocations( new VersionWithLocations.Builder(this));
                }
                version( dv );
            }
            return new FullDataset( this );
        }
    }
    
}
