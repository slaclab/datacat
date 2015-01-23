
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.base.Optional;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.RecordType;

/**
 * Class representing a view of a container or dataset.
 * @author bvan
 */
public class RequestView extends HashMap<String, String>{
    
    RecordType type;
    Optional<DatasetView> datasetView = Optional.absent();
    boolean includeMetadata = true;
    
    public static final int OBJECT = 1<<1;
    public static final int CHILDREN = 1<<2;
    public static final int METADATA = 1<<3;
    
    private static final String PRESENT = "";
    
    private static final HashSet<String> ALLOWABLE_ATTRIBUTES = new HashSet<String>(){
        {
            for(Method m: FlatDataset.Builder.class.getMethods()){
                if(m.isAnnotationPresent(JsonSetter.class)){
                    add(m.getName());
                }
            }
            for(Method m: DatasetContainerBuilder.class.getMethods()){
                if(m.isAnnotationPresent(JsonSetter.class)){
                    add(m.getName());
                }
            }
        }
    };
    
    public RequestView(RecordType type, Map<String, List<String>> params){
        this.type = type;
        validateView(params != null? params : new HashMap<String, List<String>>(0));
    }
    
    public boolean includeMetadata(){
        return includeMetadata;
    }
    
    public DatasetView getDatasetView(){
        return datasetView.or(DatasetView.EMPTY);
    }
    
    /**
     * Gets a DatasetView. If no version ID specified, use defaultView instead.
     * @param defaultView 
     * @return 
     */
    public DatasetView getDatasetView(DatasetView defaultView){
        return datasetView.or(defaultView);
    }
    
    public int getPrimaryView(){
        if(containsKey("children")){ // Highest priority
            return CHILDREN;
        } else if(containsKey("metadata")){
            return METADATA;
        } else if(containsKey("versionMetadata")){
            return METADATA;
        }
        return OBJECT;
    }
    
    private void validateView(Map<String, List<String>> params){
        Integer vid = null;
        String site = null;
        
        HashMap<String, String> m = new HashMap<>();
        if(type == null){
            type = RecordType.FOLDER; // Assume to be folder in this case.
        }
        switch(type){
            case GROUP:
                if(params.containsKey("datasets") && params.containsKey("children")){
                    throw new IllegalArgumentException("Groups can only contain datasets as children.");
                }
            case FOLDER:
                if(params.containsKey("stat") && params.containsKey("v")){
                    throw new IllegalArgumentException("Stat view not compatible with version selector");
                }
                if(params.containsKey("stat")){
                    m.put( "stat", params.get("stat").get(0));
                }
                if(params.containsKey( "versionMetadata")){
                    throw new IllegalArgumentException("versionMetadata not compatible with container");
                }
                if(params.containsKey("children")){
                    if(params.containsKey("metadata")){
                        throw new IllegalArgumentException("Metadata view not compatible with children");
                    }
                    List<String> children = params.get("children");
                    String childrenType = "";
                    if(children != null){
                        if(children.size() > 1){
                            throw new IllegalArgumentException("Only one version argument is allowed");
                        }
                        childrenType = children.get(0) == null || children.get(0).isEmpty() ? "all" : children.get(0);
                    }
                    switch(childrenType.toLowerCase()){
                        case "all":
                            m.put("children", PRESENT);
                            break;
                        case "containers":
                            m.put("children", "containers");
                            break;
                        default:
                            throw new IllegalArgumentException("Unable to return children of type " + childrenType);
                    }
                }
            case DATASET:
                List<String> version = params.get("v");
                List<String> sites = params.get("s");
                
                if(version != null){
                    if(version.size() > 1){
                        throw new IllegalArgumentException("Only one version argument is allowed");
                    }
                    vid = DatasetView.VersionId.valueOf(version.get(0)).getId();
                }
                
                if(sites != null){
                    if(sites.size() > 1) {
                        throw new IllegalArgumentException("Only one site arguments is allowed");
                    }
                    site = sites.get(0);
                    switch(site.toLowerCase()){
                        case "all":
                            site = DatasetView.ALL_SITES;
                            break;
                        case "master":
                        case "canonical":
                            site = DatasetView.CANONICAL_SITE;
                        default:
                            break;
                    }
                }
            default:
                break;
        }
        for(String attr: ALLOWABLE_ATTRIBUTES){
            if(params.containsKey(attr)){
                m.put(attr, params.get(attr).get(0));
            }
        }
        if(vid != null){
            datasetView = Optional.of(new DatasetView(vid, site != null ? site : DatasetView.ANY_SITES));
        } else if(site != null){
            datasetView = Optional.of(new DatasetView(DatasetView.CURRENT_VER, site));
        }
        putAll(m);
    }
    
}
