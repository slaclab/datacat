package org.srs.webapps.datacat.model;

import java.util.List;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.DatasetModel;

/**
 *
 * @author bvan
 */
public class NodeTargetModel {

    private String applicationBase;
    private String contextPath;
    private String endPoint;
    private String path;
    
    private List<String> queryParams;
    
    private DatacatNode target;
    private DatacatNode parent;
    private List<DatasetModel> datasets;
    private List<DatasetContainer> containers;
    private boolean containersOverflow = false;
    private DatacatNode selected;
    private int datasetCount;
    
    private boolean isDeletable;
    private boolean isWritable;
    private boolean isInsertable;

    public NodeTargetModel(ApplicationUriInfo uriInfo){ 
        this.contextPath = uriInfo.getApplicationRoot();
        this.applicationBase = uriInfo.getDisplayRoot();
        this.endPoint = uriInfo.getResourceRoot();
    }

    public String getApplicationBase(){
        return applicationBase;
    }

    public String getContextPath(){
        return contextPath;
    }

    public String getEndPoint(){
        return endPoint;
    }

    public String getPath(){
        return path;
    }

    public void setPath(String path){
        this.path = path;
    }

    public List<String> getQueryParams(){
        return queryParams;
    }

    public void setQueryParams(List<String> queryParams){
        this.queryParams = queryParams;
    }

    public DatacatNode getTarget(){
        return target;
    }

    public void setTarget(DatacatNode target){
        this.target = target;
    }

    public DatacatNode getParent(){
        return parent;
    }

    public void setParent(DatacatNode parent){
        this.parent = parent;
    }

    public List<DatasetContainer> getContainers(){
        return containers;
    }

    public void setContainers(List<DatasetContainer> containers){
        this.containers = containers;
    }
    
    public boolean getContainersOverflow(){
        return containersOverflow;
    }
    
    public void setContainersOverflow(boolean overflow){
        this.containersOverflow = overflow;
    }

    public List<DatasetModel> getDatasets(){
        return datasets;
    }

    public void setDatasets(List<DatasetModel> datasets){
        this.datasets = datasets;
    }

    public DatacatNode getSelected(){
        return selected;
    }

    public void setSelected(DatacatNode selected){
        this.selected = selected;
    }

    public int getDatasetCount(){
        return datasetCount;
    }

    public void setDatasetCount(int datasetCount){
        this.datasetCount = datasetCount;
    }

    public boolean isDeletable(){
        return isDeletable;
    }

    public void setDeletable(boolean isDeletable){
        this.isDeletable = isDeletable;
    }

    public boolean isWritable(){
        return isWritable;
    }

    public void setWritable(boolean isWritable){
        this.isWritable = isWritable;
    }

    public boolean isInsertable(){
        return isInsertable;
    }

    public void setInsertable(boolean isInsertable){
        this.isInsertable = isInsertable;
    }
    
    
    
}
