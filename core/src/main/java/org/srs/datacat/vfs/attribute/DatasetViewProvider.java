
package org.srs.datacat.vfs.attribute;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.RequestView;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.dataset.DatasetBuilder;
import org.srs.datacat.sql.DatasetDAO;
import org.srs.datacat.sql.Utils;
import org.srs.datacat.vfs.DcFile;

/**
 *
 * @author bvan
 */
public class DatasetViewProvider implements DcViewProvider<RequestView> {

    private final DcFile file;
    
    private HashMap<Integer, DatasetVersion> versionCache = new HashMap<>(4);
    private HashMap<Integer, HashMap<String,DatasetLocation>> locationCache = new HashMap<>(4);

    public DatasetViewProvider(DcFile file){
        this.file = file;
    }

    @Override
    public Dataset withView(RequestView requestView) throws FileNotFoundException, IOException {
        DatasetView view = requestView.getDatasetView();
        if(view == DatasetView.EMPTY){
            return (Dataset) file.getDatacatObject();
        }
        try (DatasetDAO dsdao = new DatasetDAO(Utils.getConnection())){
            DatasetVersion dsv;
            HashMap<String, DatasetLocation> locations;
            if(!versionCache.containsKey(view.getVersionId())){
                for(DatasetVersion v: dsdao.getDatasetVersions(file.fileKey())){
                    if(v.isLatest()){
                        versionCache.put(DatasetView.CURRENT_VER, v);
                    }
                    versionCache.put( v.getVersionId(), v);
                }
            }
            dsv = versionCache.get(view.getVersionId());
            if(dsv == null){
                String msg = "Invalid View. Version %d not found";
                throw new FileNotFoundException(String.format( msg, view.getVersionId()));
            }
            if(!locationCache.containsKey(view.getVersionId())) {
                locations = new HashMap<>(4);
                for(DatasetLocation l: dsdao.getDatasetLocations(dsv.getPk())){
                    if(l.isMaster()){
                        locations.put( DatasetView.CANONICAL_SITE, l);
                    }
                    locations.put( l.getSite(), l);
                }
                if(dsv.isLatest()){
                    locationCache.put( DatasetView.CURRENT_VER, locations);
                }
                locationCache.put( dsv.getVersionId(), locations );
            }
        } catch(SQLException ex) {
            throw new IOException("Error talking to the database", ex);
        }
        
        DatasetVersion retDsv = versionCache.get(view.getVersionId());
        HashMap<String, DatasetLocation> retLocations = locationCache.get(view.getVersionId());
        
        if(retLocations == null){
            String msg = "No locations found for dataset version %d";
            throw new FileNotFoundException(String.format( msg, view.getVersionId()));
        }
        DatasetBuilder b = DatasetBuilder.create( (Dataset) file.getDatacatObject());
        if(!requestView.includeMetadata()){ // mask metadata
            retDsv = new DatasetVersion.Builder(retDsv).metadata((List)null).build();
        }
        b.version(retDsv);
        if(view.isAll()){
            b.locations(new ArrayList<>(retLocations.values()));
        } else {
            if(retLocations.containsKey(view.getSite())){
                b.location(retLocations.get(view.getSite()));
            } else {
                String msg = "Location %s not found";
                throw new FileNotFoundException(String.format( msg, view.getSite()));
            }
        }
        return b.buildDataset();
    }

    @Override
    public String name(){
        return "dsviews";
    }

}
