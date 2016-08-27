package org.srs.webapps.datacat.model;

import java.util.List;
import org.srs.datacat.model.DatacatNode;

/**
 *
 * @author bvan
 */
public class NodeTargetModel {

    private String applicationBase;
    private String contextPath;
    private String endPoint;
    private String parentURL;
    private String path;
    
    private List<String> queryParams;
    
    private DatacatNode target;
    private DatacatNode parent;
    private List<DatacatNode> containers;
    private List<DatacatNode> datasets;
    private DatacatNode selected;
    private int datasetCount;
    
    private boolean isDeletable;
    private boolean isWritable;
    private boolean isInsertable;

    public NodeTargetModel(){ }

    public String getApplicationBase(){
        return applicationBase;
    }

    public void setApplicationBase(String applicationBase){
        this.applicationBase = applicationBase;
    }

    public String getContextPath(){
        return contextPath;
    }

    public void setContextPath(String contextPath){
        this.contextPath = contextPath;
    }

    public String getEndPoint(){
        return endPoint;
    }

    public void setEndPoint(String endPoint){
        this.endPoint = endPoint;
    }

    public String getParentURL(){
        return parentURL;
    }

    public void setParentURL(String parentURL){
        this.parentURL = parentURL;
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

    public List<DatacatNode> getContainers(){
        return containers;
    }

    public void setContainers(List<DatacatNode> containers){
        this.containers = containers;
    }

    public List<DatacatNode> getDatasets(){
        return datasets;
    }

    public void setDatasets(List<DatacatNode> datasets){
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
