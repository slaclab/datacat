
package org.srs.datacat.vfs;

import java.io.Serializable;
import org.srs.datacat.shared.DatacatObject;

/**
 * A limited class representing a datastore record, similar to DatacatObject.
 * Used for simplifying requests into the data store.
 * @author bvan
 */
public final class DcRecord implements Serializable {
    private long pk;
    private DatacatObject.Type type;
    private String path;
    
    public DcRecord(){}
    
    public DcRecord(DatacatObject object){
        this.pk = object.getPk();
        this.type = object.getType();
        this.path = object.getPath();
    }

    public DcRecord(long pk, long parentPk, DatacatObject.Type type, String path){
        this.pk = pk;
        this.type = type;
        this.path = path;
    }

    public long getPk(){
        return pk;
    }

    public DatacatObject.Type getType(){
        return type;
    }

    public String getPath(){
        return path;
    }

}
