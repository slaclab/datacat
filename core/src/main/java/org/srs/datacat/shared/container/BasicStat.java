package org.srs.datacat.shared.container;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Basic stat info for a group or a folder.
 *
 * @author bvan
 */
@XmlRootElement
@XmlType(name = "stat")
@XmlSeeAlso({DatasetStat.class})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "$type", defaultImpl = BasicStat.class)
@JsonSubTypes(value = {@JsonSubTypes.Type(DatasetStat.class)})
public class BasicStat {

    /**
     * Type of Stat.
     */
    public static enum StatType {
        NONE,
        LAZY,
        BASIC,
        DATASET;
    }

    private int datasetCount;
    private int groupCount;
    private int folderCount;

    public BasicStat(){}

    public BasicStat(int folderCount, int groupCount, int datasetCount){
        this.datasetCount = datasetCount;
        this.groupCount = groupCount;
        this.folderCount = folderCount;
    }

    public BasicStat(BasicStat stat){
        this(stat.folderCount, stat.groupCount, stat.datasetCount);
    }

    @XmlElement(required = true)
    public Integer getChildCount(){
        return folderCount + groupCount + datasetCount;
    }

    @XmlElement(required = false)
    public Integer getChildContainerCount(){
        return folderCount + groupCount;
    }

    @XmlElement(required = true)
    public Integer getDatasetCount(){
        return datasetCount;
    }

    @XmlElement(required = true)
    public Integer getGroupCount(){
        return groupCount;
    }

    @XmlElement(required = true)
    public Integer getFolderCount(){
        return folderCount;
    }

    public void setDatasetCount(int datasets){
        this.datasetCount = datasets;
    }

    public void setGroupCount(int groups){
        this.groupCount = groups;
    }

    public void setFolderCount(int folders){
        this.folderCount = folders;
    }

}
