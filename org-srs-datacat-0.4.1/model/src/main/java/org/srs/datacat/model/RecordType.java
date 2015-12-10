
package org.srs.datacat.model;

import org.srs.datacat.model.DatasetContainer.Folder;
import org.srs.datacat.model.DatasetContainer.Group;
import org.srs.datacat.model.dataset.DatasetLocationModel;
import org.srs.datacat.model.dataset.DatasetVersionModel;

/**
 * The basic type of this DatacatObject.
 */
public enum RecordType {
    DATASET, DATASETLOCATION, DATASETVERSION, FOLDER, GROUP;

    public boolean isContainer(){
        return this == FOLDER || this == GROUP;
    }

    public static RecordType typeOf(DatacatRecord object){
        if(object instanceof Folder){
            return FOLDER;
        }
        if(object instanceof DatasetModel){
            return DATASET;
        }
        if(object instanceof Group){
            return GROUP;
        }
        if(object instanceof DatasetVersionModel){
            return DATASETVERSION;
        }
        if(object instanceof DatasetLocationModel){
            return DATASETLOCATION;
        }
        return null;
    }

    public static RecordType fromJsonType(String jsonType){
        jsonType = jsonType == null ? "" : jsonType;
        switch(jsonType){
            case "folder":
                return FOLDER;
            case "group":
                return GROUP;
            case "dataset":
            case "dataset#flat":
            case "dataset#full":
                return DATASET;
            case "version":
                return DATASETVERSION;
            case "location":
                return DATASETLOCATION;
            default:
                return null;
        }
    }
    
}
