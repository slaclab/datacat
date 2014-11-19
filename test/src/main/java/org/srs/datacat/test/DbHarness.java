
package org.srs.datacat.test;

import java.sql.SQLException;
import javax.sql.DataSource;

/**
 *
 * @author bvan
 */
public abstract class DbHarness {
    
    public abstract DataSource getDataSource();
    
    public static DbHarness getDbHarness() throws SQLException{
        String harness = System.getProperty( "db.test.harness");
        if("mysql".equals( harness )){
            return new MySQLDbHarness();
        } else if ("hsqldb".equals( harness )){
            return new HSqlDbHarness();
        }
        throw new IllegalArgumentException("unable to intiate database test harness");
    }

}
