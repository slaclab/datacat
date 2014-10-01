
package org.srs.datacat.vfs.attribute;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.RequestView;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.DatasetVersion;
import org.srs.datacat.shared.dataset.FullDataset;
import org.srs.datacat.shared.dataset.VersionWithLocations;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcFileSystemProvider;

/**
 *
 * @author bvan
 */
public class DatasetViewProvider implements DcViewProvider<RequestView> {

    private final DcFile file;
    private final DcFileSystemProvider provider;
    private boolean allVersionsLoaded = false;
    
    private final HashMap<Integer, VersionWithLocations> versionCache = new HashMap<>(4);
    //private final HashMap<Integer, HashMap<String,DatasetLocation>> locationCache = new HashMap<>(4);

    public DatasetViewProvider(DcFile file, Dataset object){
        this.file = file;
        this.provider = file.getPath().getFileSystem().provider();
        if(object instanceof FullDataset){
            VersionWithLocations dsv = (VersionWithLocations) ((FullDataset) object).getVersion();
            if(dsv.isLatest()){
                versionCache.put( DatasetView.CURRENT_VER, dsv );
            }
            versionCache.put( dsv.getVersionId(), dsv );
        }
    }
    
    public void clear(){
        synchronized(this){
            versionCache.clear();
            //locationCache.clear();
        }
    }

    @Override
    public Dataset withView(RequestView requestView) throws FileNotFoundException, IOException {
        return withView(requestView.getDatasetView(), requestView.includeMetadata());
    }
        
    public Dataset withView(DatasetView view, boolean withMetadata) throws FileNotFoundException, IOException {
        if(view == DatasetView.EMPTY){
            return (Dataset) file.getObject();
        }
        VersionWithLocations dsv;
        boolean noSites = DatasetView.EMPTY_SITES.equals(view.getSite());
        boolean anySites = DatasetView.ANY_SITES.equals(view.getSite());
        DatasetVersion retDsv;
        Set<DatasetLocation> retLocations;
        synchronized(this) {
            if(!versionCache.containsKey(view.getVersionId())){
                dsv = provider.getVersionWithLocations(file, view);
                if(dsv.isLatest()){
                    versionCache.put( DatasetView.CURRENT_VER, dsv);
                }
                versionCache.put(dsv.getVersionId(), dsv);
            }
            dsv = versionCache.get(view.getVersionId());
        }
        if(dsv == null){
            String msg = "Invalid View. Version %d not found";
            throw new FileNotFoundException( String.format( msg, view.getVersionId() ) );
        }
        retDsv = new DatasetVersion(dsv);
        retLocations = dsv.getLocations();
        if(retLocations == null && !(noSites || anySites)){
            String msg = "No locations found for dataset version %d";
            throw new FileNotFoundException(String.format( msg, view.getVersionId()));
        }
        Dataset.Builder b = new Dataset.Builder((Dataset) file.getObject());
        if(!withMetadata){ // mask metadata
            retDsv = new DatasetVersion.Builder(retDsv).metadata((List)null).build();
        }
        b.version(retDsv);
        if(!noSites){
            if(view.isAll()){
                b.locations(retLocations);
            } else {
                if(dsv.getLocation(view.getSite()) != null){
                    b.location(dsv.getLocation( view.getSite()));
                } else if(!anySites){
                    String msg = "Location %s not found";
                    throw new FileNotFoundException(String.format( msg, view.getSite()));
                }
            }
        }
        return b.buildDataset();
    }

    @Override
    public String name(){
        return "dsviews";
    }

}
