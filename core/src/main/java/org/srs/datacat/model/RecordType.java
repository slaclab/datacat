
package org.srs.datacat.model;

import org.srs.datacat.model.DatasetContainer.Folder;
import org.srs.datacat.model.DatasetContainer.Group;

/**
 * The basic type of this DatacatObject.
 */
public enum RecordType {
    DATASET, DATASETLOCATION, DATASETVERSION, FOLDER, GROUP;

    public boolean isContainer(){
        return this == FOLDER || this == GROUP;
    }

    public static RecordType typeOf(DatacatNode object){
        if(object instanceof Folder){
            return FOLDER;
        }
        if(object instanceof DatasetModel){
            return DATASET;
        }
        if(object instanceof Group){
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
