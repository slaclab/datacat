
package org.srs.datacat.shared.dataset;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Timestamp;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.srs.datacat.model.DatasetVersionModel;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.rest.shared.RestDateAdapter;
import org.srs.rest.shared.metadata.MetadataEntry;

/**
 *
 * @author bvan
 */
public class DatasetWithView extends Dataset implements DatasetVersionModel {
    private DatasetViewInfo viewInfo;
    
    public DatasetWithView(){}
    
    public DatasetWithView(Dataset dataset, boolean flatten){
        super(dataset);
        DatasetVersion dsVersion = null;
        DatasetLocation dsLocation = null;
        if(dataset instanceof DatasetWithView){
            DatasetViewInfo info = ((DatasetWithView) dataset).getViewInfo();
            dsVersion = info.getVersion();
            if(flatten){
                if(dataset instanceof FullDataset){
                    dsLocation = info.canonicalLocationOpt().orNull();
                } else {
                    dsLocation = info.singularLocationOpt().orNull();
                }
                this.viewInfo = new DatasetViewInfo(dsVersion, dsLocation);
            } else {
                this.viewInfo = ((DatasetWithView) dataset).getViewInfo();
            }
            return;
        }
        this.viewInfo = null;
    }
    
    protected DatasetWithView(Dataset.Builder builder, boolean flatten){
        super(builder);
        if(flatten){
            this.viewInfo = new DatasetViewInfo(builder.version, builder.location);
        } else {
            this.viewInfo = new DatasetViewInfo(builder.version, builder.locations);
        }
    }
    
    @XmlTransient
    public DatasetViewInfo getViewInfo(){
        return viewInfo;
    }
    
    @XmlElement(required=false)
    public Long getVersionPk(){
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getPk(); 
        }
        return null;
    }
    
    @XmlElementWrapper(name="versionMetadata")
    @XmlElement(required=false, name="entry")
    @JsonProperty("versionMetadata")
    public List<MetadataEntry> getVersionMetadata(){ 
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getMetadata();
        }
        return null; 
    }
    
    @Override
    @XmlElement(required=false)
    public String getDatasetSource(){ 
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getDatasetSource();
        }
        return null; 
    }

    @Override
    @XmlElement(required=false)
    public Boolean isLatest(){ 
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().isLatest(); 
        }
        return null;
    }

    @Override
    @XmlElement(required=false)
    public Long getProcessInstance(){ 
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getProcessInstance();
        }
        return null; }

    @Override
    @XmlElement(required=false)
    public String getTaskName(){ 
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getTaskName();
        }
        return null; 
    }

    @Override
    @XmlElement(required=false)
    public Integer getVersionId(){ 
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getVersionId();
        }
        return null; 
    }

    @XmlElement(name="versionCreated", required=false)
    @XmlJavaTypeAdapter(RestDateAdapter.class) 
    public Timestamp getDateVersionCreated(){ 
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getDateCreated();
        }
        return null; 
    }


}
