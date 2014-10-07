
package org.srs.datacat.dao.sql;

import java.io.IOException;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 *
 * @author Brian Van Klaveren<bvan@slac.stanford.edu>
 */
public class DAOFactory {
    private DataSource dataSource;
    
    public DAOFactory(DataSource ds){
        this.dataSource = ds;
    }
    
    public BaseDAO newBaseDAO() throws IOException{
        try {
            return new BaseDAO(dataSource.getConnection());
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }
    
    public ContainerDAO newContainerDAO() throws IOException{
        try {
            return new ContainerDAO(dataSource.getConnection());
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }
    
    public DatasetDAO newDatasetDAO() throws IOException{
        try {
            return new DatasetDAO(dataSource.getConnection());
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }

}
