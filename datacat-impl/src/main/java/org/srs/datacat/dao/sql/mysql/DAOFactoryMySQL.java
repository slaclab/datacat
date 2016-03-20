
package org.srs.datacat.dao.sql.mysql;

import java.io.IOException;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.srs.datacat.dao.SearchDAO;
import org.srs.datacat.dao.sql.SqlDAOFactory.Locker;
import org.srs.datacat.dao.sql.SqlSearchDAO;

/**
 *
 * @author bvan
 */
public class DAOFactoryMySQL implements org.srs.datacat.dao.DAOFactory {
    protected final DataSource dataSource;
    private final Locker locker = new Locker();
          
    public DAOFactoryMySQL(DataSource ds){
        this.dataSource = ds;
    }
    
    @Override
    public BaseDAOMySQL newBaseDAO() throws IOException{
        try {
            return new BaseDAOMySQL(dataSource.getConnection(), locker);
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }
    
    @Override
    public ContainerDAOMySQL newContainerDAO() throws IOException{
        try {
            return new ContainerDAOMySQL(dataSource.getConnection(), locker);
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }

    /**
     * Get a new DatasetDAO.
     * 
     * @return
     * @throws IOException 
     */
    @Override
    public DatasetDAOMySQL newDatasetDAO() throws IOException{
        try {
            return new DatasetDAOMySQL(dataSource.getConnection(), locker);
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }
    
    @Override
    public SearchDAO newSearchDAO(Object... plugins) throws IOException{
        try {
            return new SqlSearchDAO(dataSource.getConnection(), null);
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }

}
