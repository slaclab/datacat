
package org.srs.datacat.dao.sql.mysql;

import java.io.IOException;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.srs.datacat.dao.sql.SqlBaseDAO;
import org.srs.datacat.dao.sql.SqlDAOFactory;

/**
 *
 * @author bvan
 */
public class DAOFactoryMySQL  extends SqlDAOFactory{

    public DAOFactoryMySQL(DataSource ds){
        super(ds);
    }

    @Override
    public SqlBaseDAO newBaseDAO() throws IOException{
        try {
            return new BaseDAOMySQL(dataSource.getConnection());
        } catch(SQLException ex) {
            throw new IOException("Error connecting to data source", ex);
        }
    }

}
