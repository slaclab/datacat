
package org.srs.datacat.dao.sql;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.sql.Connection;
import java.text.ParseException;
import java.util.LinkedList;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.dao.sql.search.DatasetSearch;
import org.srs.datacat.dao.sql.search.plugins.EXODatacatSearchPlugin;
import org.srs.datacat.dao.sql.search.plugins.LsstFilesSearchPlugin;
import org.srs.datacat.dao.sql.search.plugins.LsstKVSearchPlugin;
import org.srs.datacat.dao.sql.search.plugins.LsstPositionsSearchPlugin;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.shared.Provider;

/**
 *
 * @author bvan
 */
public class SqlSearchDAO extends SqlBaseDAO implements org.srs.datacat.dao.SearchDAO {

    private DatasetSearch search;
    
    public SqlSearchDAO(Connection conn, Object... plugins) throws IOException{
        super(conn);
        search = new DatasetSearch(conn,
                new Provider(),
                EXODatacatSearchPlugin.class,
                LsstFilesSearchPlugin.class,
                LsstKVSearchPlugin.class,
                LsstPositionsSearchPlugin.class);
    }

    @Override
    public DirectoryStream<DatasetModel> search(LinkedList<DatacatNode> containers,
            DatasetView datasetView, String query, String[] retrieveFields,
            String[] sortFields) throws ParseException, IOException{
        return search.search(containers, datasetView, query, retrieveFields, sortFields);
    }
 
}
