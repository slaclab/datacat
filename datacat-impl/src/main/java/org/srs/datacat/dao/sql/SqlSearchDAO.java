
package org.srs.datacat.dao.sql;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.sql.Connection;
import java.text.ParseException;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.dao.sql.search.ContainerSearch;
import org.srs.datacat.dao.sql.search.DatasetSearch;
import org.srs.datacat.dao.sql.search.plugins.EXODatacatSearchPlugin;
import org.srs.datacat.dao.sql.search.plugins.LsstFilesSearchPlugin;
import org.srs.datacat.dao.sql.search.plugins.LsstKVSearchPlugin;
import org.srs.datacat.dao.sql.search.plugins.LsstPositionsSearchPlugin;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.shared.Provider;

/**
 *
 * @author bvan
 */
public class SqlSearchDAO extends SqlBaseDAO implements org.srs.datacat.dao.SearchDAO {
    
    public SqlSearchDAO(Connection conn, SqlDAOFactory.Locker locker, Object... plugins) throws IOException{
        super(conn, locker);
    }

    @Override
    public DirectoryStream<DatasetModel> search(DirectoryStream<DatacatNode> containers,
            DatasetView datasetView, String query, String[] retrieveFields,
            
            String[] sortFields) throws ParseException, IOException{
        DatasetSearch search = new DatasetSearch(super.getConnection(),
                new Provider(),
                EXODatacatSearchPlugin.class,
                LsstFilesSearchPlugin.class,
                LsstKVSearchPlugin.class,
                LsstPositionsSearchPlugin.class);
        return search.search(containers, datasetView, query, retrieveFields, sortFields);
    }
    
    @Override
    public DirectoryStream<DatasetContainer> searchContainers(DirectoryStream<DatacatNode> containers,
            String query, String[] retrieveFields,
            String[] sortFields) throws ParseException, IOException{
        ContainerSearch search = new ContainerSearch(super.getConnection(), new Provider());
        return search.search(containers, query, retrieveFields, sortFields);
    }

}