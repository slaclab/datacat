package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.sql.Timestamp;
import java.util.List;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.srs.datacat.model.DatasetLocationModel;
import org.srs.datacat.model.DatasetVersionModel;
import org.srs.datacat.model.DatasetWithViewModel;
import org.srs.datacat.shared.adapters.RestDateAdapter;
import org.srs.datacat.shared.metadata.MetadataEntry;

/**
 *
 * @author bvan
 */
@JsonPropertyOrder({
    "_type", "name", "path", "pk", "parentPk",
    "metadata", "dataType", "fileFormat", "created",
    "versionId", "latest", "versionCreated",
    "versionMetadata", "datasetSource", "taskName", "processInstance"}
)
public class DatasetWithView extends Dataset implements DatasetWithViewModel {
    private DatasetViewInfo viewInfo;

    public DatasetWithView(){}

    public DatasetWithView(Dataset dataset, boolean flatten){
        super(dataset);
        DatasetVersionModel dsVersion;
        DatasetLocationModel dsLocation;
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
        DatasetVersionModel viewVer = builder.version;
        if(viewVer == null && builder.checkType(Dataset.Builder.VERSION)){
            viewVer = new DatasetVersion.Builder(builder).build();
        }
        if(flatten){
            DatasetLocationModel viewLoc = builder.location;
            if(viewLoc == null && builder.checkType(Dataset.Builder.LOCATION)){
                viewLoc = new DatasetLocation(builder);
            }
            this.viewInfo = new DatasetViewInfo(viewVer, viewLoc);
        } else {
            this.viewInfo = new DatasetViewInfo(viewVer, builder.locations);
        }
    }

    @JsonIgnore    
    public DatasetViewInfo getViewInfo(){
        return viewInfo;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getVersionPk(){
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getPk();
        }
        return null;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("versionMetadata")
    @JacksonXmlElementWrapper(localName="versionMetadata")
    @JacksonXmlProperty(localName="entry")
    public List<MetadataEntry> getVersionMetadata(){
        if(viewInfo.versionOpt().isPresent()){
            List<MetadataEntry> entries = MetadataEntry.toList(viewInfo.getVersion().getMetadataMap());
            return entries.size() > 0 ? entries : null;
        }
        return null;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDatasetSource(){
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getDatasetSource();
        }
        return null;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Boolean isLatest(){
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().isLatest();
        }
        return null;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getProcessInstance(){
        if(viewInfo.versionOpt().isPresent()){
            return ((DatasetVersion) viewInfo.getVersion()).getProcessInstance();
        }
        return null;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getTaskName(){
        if(viewInfo.versionOpt().isPresent()){
            return ((DatasetVersion) viewInfo.getVersion()).getTaskName();
        }
        return null;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Integer getVersionId(){
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getVersionId();
        }
        return null;
    }

    @XmlJavaTypeAdapter(RestDateAdapter.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("versionCreated")
    public Timestamp getDateVersionCreated(){
        if(viewInfo.versionOpt().isPresent()){
            return viewInfo.getVersion().getDateCreated();
        }
        return null;
    }

}
