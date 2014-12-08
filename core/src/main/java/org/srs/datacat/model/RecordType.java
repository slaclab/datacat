/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacat.model;

import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetGroup;
import org.srs.datacat.shared.LogicalFolder;

/**
 * The basic type of this DatacatObject.
 */
//@XmlEnum(String.class)
public enum RecordType {
    DATASET, DATASETLOCATION, DATASETVERSION, FOLDER, GROUP;

    public boolean isContainer(){
        return this == FOLDER || this == GROUP;
    }

    public static RecordType typeOf(DatacatNode object){
        if(object instanceof LogicalFolder){
            return FOLDER;
        }
        if(object instanceof Dataset){
            return DATASET;
        }
        if(object instanceof DatasetGroup){
            return GROUP;
        }
        return null;
    }

    public static RecordType fromJsonType(String jsonType){
        jsonType = jsonType == null ? "" : jsonType;
        switch(jsonType){
            case "folder":
                return RecordType.FOLDER;
            case "group":
                return RecordType.GROUP;
            case "dataset":
            case "dataset#flat":
            case "dataset#full":
                return RecordType.DATASET;
            default:
                return null;
        }
    }
    
}
