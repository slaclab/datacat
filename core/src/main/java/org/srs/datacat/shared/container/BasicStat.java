/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacat.shared.container;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
/**
 * Basic stat info for a group or a folder
 * 
 * @author bvan
 */
@XmlRootElement
@XmlType(name="stat")
@XmlSeeAlso({DatasetStat.class})
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="$type", defaultImpl=BasicStat.class)
@JsonSubTypes(value = {@JsonSubTypes.Type(DatasetStat.class)})
public class BasicStat {
    
    public static enum StatType {
        NONE,
        LAZY,
        BASIC,
        DATASET;
    }

    private long datasetCount;
    private long groupCount;
    private long folderCount;
    
    public BasicStat(){}
    
    public BasicStat(long folderCount, long groupCount, long datasetCount){
        this.datasetCount = datasetCount;
        this.groupCount = groupCount;
        this.folderCount = folderCount;
    }
    
    public BasicStat(BasicStat stat){
        this(stat.folderCount, stat.groupCount, stat.datasetCount);
    }
    
    @XmlElement(required=true)
    public Long getChildCount(){
        return folderCount + groupCount + datasetCount;
    }
    
    @XmlElement(required=false)
    public Long getChildContainerCount(){
        return folderCount + groupCount;
    }
    
    @XmlElement(required=true)
    public Long getDatasetCount(){
        return datasetCount;
    }
    
    @XmlElement(required=true)
    public Long getGroupCount(){
        return groupCount;
    }
    
    @XmlElement(required=true)
    public Long getFolderCount(){
        return folderCount;
    }
    
    public void setDatasetCount(long datasets){
        this.datasetCount = datasets;
    }
    
    public void setGroupCount(long groups){
        this.groupCount = groups;
    }
    
    public void setFolderCount(long folders){
        this.folderCount = folders;
    }
    
}
