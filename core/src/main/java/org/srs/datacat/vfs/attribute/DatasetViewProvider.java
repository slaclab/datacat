
package org.srs.datacat.vfs.attribute;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.dataset.DatasetLocationModel;
import org.srs.datacat.model.dataset.DatasetVersionModel;
import org.srs.datacat.model.dataset.DatasetViewInfoModel;
import org.srs.datacat.model.dataset.DatasetWithViewModel;
import org.srs.datacat.vfs.DcFile;
import org.srs.datacat.vfs.DcFileSystemProvider;

/**
 *
 * @author bvan
 */
public class DatasetViewProvider implements DcViewProvider<DatasetView> {

    private final DcFile file;
    private final DcFileSystemProvider provider;
    
    private final HashMap<Integer, DatasetViewInfoModel> versionCache = new HashMap<>(4);
    
    public DatasetViewProvider(DcFile file, DcFileSystemProvider provider, DatasetModel object){
        this.file = file;
        this.provider = provider;
        if(object instanceof DatasetWithViewModel){
            DatasetWithViewModel objectWithView = ((DatasetWithViewModel) object);
            if(objectWithView.getViewInfo().locationsOpt().isPresent()){
                DatasetViewInfoModel viewInfo = objectWithView.getViewInfo();
                if(viewInfo.getVersion().isLatest()){
                    versionCache.put(DatasetView.CURRENT_VER, viewInfo);
                }
                versionCache.put(viewInfo.getVersion().getVersionId(), viewInfo);
            }
        }
    }
    
    public void clear(){
        synchronized(this){
            versionCache.clear();
            //locationCache.clear();
        }
    }

    @Override
    public DatasetModel withView(DatasetView requestView) throws NoSuchFileException, IOException {
        return withView(requestView, false);
    }
   
    public DatasetModel withView(DatasetView view, boolean withMetadata) throws NoSuchFileException, IOException {
        if(view == DatasetView.EMPTY){
            return (DatasetModel) file.getObject();
        }
        DatasetViewInfoModel dsv;
        DatasetVersionModel retDsv;
        Set<DatasetLocationModel> retLocations;
        synchronized(this) {
            if(!versionCache.containsKey(view.getVersionId())){
                dsv = provider.getDatasetViewInfo(file, view);
                if(dsv.getVersion().isLatest()){
                    versionCache.put(DatasetView.CURRENT_VER, dsv);
                }
                versionCache.put(dsv.getVersion().getVersionId(), dsv);
            }
            dsv = versionCache.get(view.getVersionId());
        }
        if(dsv == null){
            String msg = "Unable to process view. Version %d not found";
            throw new NoSuchFileException( String.format( msg, view.getVersionId() ) );
        }
        retDsv = dsv.getVersion();
        retLocations = dsv.getLocations();
        if(retLocations == null && !(view.zeroSites() || view.zeroOrMoreSites())){
            String msg = "No locations found for dataset version %d";
            throw new NoSuchFileException(String.format( msg, view.getVersionId()));
        }
        DatasetModel.Builder b = provider.getModelProvider().getDatasetBuilder().create(file.getObject());
        if(!withMetadata){ // mask metadata
            retDsv = provider.getModelProvider().getVersionBuilder().create(retDsv).metadata((Map)null).build();
        }
        b.version(retDsv);
        
        if(!view.zeroSites()){                                 // Don't bother if zeroSites is true
            boolean multipleSites = view.allSites() || view.zeroOrMoreSites();
            if(retLocations != null && !retLocations.isEmpty() && multipleSites){    // We want all/any sites 
                // retLocations is not null/empty
                if(retLocations.size() == 1){
                    b.location(retLocations.iterator().next());       
                } else {
                    b.locations(retLocations);
                }
            } else {
                if(dsv.getLocation(view.getSite()) != null){     // If we find a site, use it
                    b.location(dsv.getLocation(view.getSite()));
                } else if(!view.zeroOrMoreSites()){              // No site, is zero acceptable?
                    String msg = "Location %s not found";
                    throw new NoSuchFileException(String.format(msg, view.getSite()));
                }
            }
        }
        return b.build();
    }

    @Override
    public String name(){
        return "dsviews";
    }

}
