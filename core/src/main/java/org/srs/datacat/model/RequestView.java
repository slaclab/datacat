
package org.srs.datacat.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlTransient;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.dataset.FlatDataset;

/**
 * Class representing a view of a container or dataset.
 * @author bvan
 */
@XmlTransient
public class RequestView extends HashMap<String,String>{
    
    DatacatObject.Type type;
    DatasetView datasetView;
    boolean includeMetadata = true;
    
    public static final int OBJECT = 1<<1;
    public static final int CHILDREN = 1<<2;
    public static final int METADATA = 1<<3;
    
    private static final String PRESENT = new String();
    
    private static final HashSet<String> allowableAttributes = new HashSet<String>(){
        {
            for(Method m: FlatDataset.Builder.class.getMethods()){
                if(m.isAnnotationPresent(JsonSetter.class)){
                    add(m.getName());
                }
            }
            for(Method m: DatasetContainer.Builder.class.getMethods()){
                if(m.isAnnotationPresent(JsonSetter.class)){
                    add(m.getName());
                }
            }
        }
    };
    
    public RequestView(DatacatObject.Type type, Map<String,List<String>> params){
        this.type = type;
        validateView(params != null? params : new HashMap<String,List<String>>(0));
    }
    
    public boolean includeMetadata(){
        return includeMetadata;
    }
    
    public DatasetView getDatasetView(){
        return this.datasetView;
    }
    
    /**
     * Gets a DatasetView. If no version ID specified, use defaultView instead.
     * @param defaultView 
     * @return 
     */
    public DatasetView getDatasetView(DatasetView defaultView){
        if(datasetView.getVersionId() != DatasetView.EMPTY_VER){
            return datasetView;
        }
        return defaultView;
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
    
    private void validateView(Map<String,List<String>> params){
        String site = DatasetView.ANY_SITES;
        int vid = DatasetView.EMPTY_VER;
        HashMap<String,String> m = new HashMap<>();
        if(type == null){
            type = DatacatObject.Type.FOLDER; // Assume to be folder in this case.
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
                    m.put( "children", PRESENT);
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
                    if(sites.size() > 1)
                        throw new IllegalArgumentException( "Only one site arguments is allowed" );
                    site = sites.get(0);
                    switch(site.toLowerCase()){
                        case "all":
                            site = DatasetView.ALL_SITES;
                            break;
                        case "master":
                        case "canonical":
                            site = DatasetView.CANONICAL_SITE;
                    }
                }
        }
        for(String attr: allowableAttributes){
            if(params.containsKey(attr)){
                m.put(attr, params.get(attr).get(0));
            }
        }
        datasetView = new DatasetView(vid, site);
        putAll( m );
    }
    
}
