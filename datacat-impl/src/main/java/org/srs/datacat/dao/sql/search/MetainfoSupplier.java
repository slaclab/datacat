
package org.srs.datacat.dao.sql.search;

import com.google.common.base.Supplier;
import java.sql.Connection;

/**
 * Abstract class for memoization support in Datacat searching.
 * @author bvan
 */
public abstract class MetainfoSupplier implements Supplier<MetanameContext>{
    private Connection conn;
        
    public Connection getConnection(){
        return this.conn;
    }
    
    public void setCurrentConnection(Connection val){
        this.conn = val;
    }

}
