
package org.srs.datacat.rest;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import org.srs.datacat.model.dataset.DatasetLocationModel;
import org.srs.datacat.model.RecordType;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetContainerBuilder;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.metadata.MetadataEntry;

/**
 *
 * @author bvan
 */
public class FormParamConverter {

    final static HashMap<String, Method> containerSetters = new HashMap<>();
    final static HashMap<String, Method> datasetSetters = new HashMap<>();
    final static HashMap<String, Method> locationSetters = new HashMap<>();
    final static ObjectMapper mdMapper = new ObjectMapper();
    
    static {
        
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        AnnotationIntrospector pair = new AnnotationIntrospectorPair(primary, secondary);
        mdMapper.setAnnotationIntrospector( pair );
        
        Dataset.Builder dsBuilder = new Dataset.Builder();
        for(Method m: dsBuilder.getClass().getMethods()){
            if(m.getAnnotation( JsonSetter.class ) != null){
                datasetSetters.put( m.getName(), m );
            }
        }
        
        DatasetLocation.Builder dslBuilder = new DatasetLocation.Builder();
        for(Method m: dslBuilder.getClass().getMethods()){
            if(m.getAnnotation( JsonSetter.class ) != null){
                locationSetters.put( m.getName(), m );
            }
        }
        
        DatasetContainerBuilder builder = new DatasetContainerBuilder();
        for(Method m: builder.getClass().getMethods()){
            if(m.getAnnotation( JsonSetter.class ) != null){
                containerSetters.put( m.getName(), m );
            }
        }

    }
    
    public static DatasetContainerBuilder getContainerBuilder(RecordType type, MultivaluedMap<String,String> formParams){
        DatasetContainerBuilder builder = new DatasetContainerBuilder();
        builder.jsonType(type.toString());
        ObjectMapper mapper = new ObjectMapper();
        buildFromParams(mapper, builder, containerSetters, formParams);
        return builder;
    }
    
    public static Dataset.Builder getDatasetBuilder(Map<String,List<String>> formParams){
        HashMap<String, List<String>> datasetInfo = new HashMap<>();
        HashMap<String,List<String>> locationInfo = new LinkedHashMap<>();
        List<String> formParamKeys = new ArrayList<>(formParams.keySet());
        Dataset.Builder builder = new Dataset.Builder();
        // Sort it so priority locations added to locationInfo first
        Collections.sort( formParamKeys );  
        for(String name: formParamKeys){
            if(name.startsWith("location")){
                locationInfo.put( name, formParams.get( name ));
            } else if(name.equals("versionMetadata")){
                builder.versionMetadata(processMetadata(formParams.get(name)));
            } else {
                datasetInfo.put(name, formParams.get( name));
            }
        }
        ObjectMapper mapper = new ObjectMapper();
        
        buildFromParams(mapper, builder, datasetSetters, datasetInfo);
        if(!locationInfo.isEmpty()){
            HashMap<String,HashMap<String,List<String>>> locationsEntries = new LinkedHashMap<>();
            List<DatasetLocationModel> locations = new ArrayList<>();
            for(String locParam: locationInfo.keySet()){ // locationInfo.keySet() is sorted
                String[] keys = locParam.split( "\\.");
                String locEntry = keys[0];
                String field = keys[1];
                if(!locationsEntries.containsKey( locEntry)){ // Will be sorted as well
                    locationsEntries.put(locEntry, new HashMap<String,List<String>>());
                }
                locationsEntries.get(locEntry).put(field, formParams.get(locParam));
            }
            for(HashMap<String,List<String>> loc: locationsEntries.values()){
                DatasetLocation.Builder dsb = new DatasetLocation.Builder();
                buildFromParams(mapper, dsb, locationSetters, loc);
                locations.add(dsb.build()); // Again, still sorted
            }
            if(locations.size() == 1){
                builder.location(locations.get(0));
            } else {
                builder.locations(locations);
            }
        }
        return builder;
    }

    private static void buildFromParams(ObjectMapper mapper, DatacatObject.Builder builder, HashMap<String, Method> setters, Map<String,List<String>> formParams){
        String key = null;
        try {
            for(Iterator<String> iter = formParams.keySet().iterator(); iter.hasNext();){
                key = iter.next();
                Method m = setters.get( key );
                if(m == null){
                    throw new IllegalArgumentException("Unable to set field: " + key);
                }
                List<String> lValue = formParams.get( key );
                Object value = null;
                if(key.equals("metadata")){
                    value = processMetadata(lValue);
                } else {
                    if(lValue.size() != 1){
                        throw new RuntimeException( "Only one value per parameter is supported" );
                    }
                    Class<?> targetValueType = m.getParameterTypes()[0]; // Should only be one
                    value = targetValueType == String.class ? lValue.get(0) : 
                            mapper.convertValue(lValue.get(0), targetValueType);
                }
                m.invoke( builder, value );
            }
        } catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) { 
            throw new IllegalArgumentException("Error attempting to build object. Check builder @JsonSetter annotations: " + key, ex);
        }
    }
    
    private static List<MetadataEntry> processMetadata(List<String> mdEntries) throws RuntimeException{
        TypeReference<List<MetadataEntry>> compoundRef = new TypeReference<List<MetadataEntry>>(){};
        try {
            ArrayList<MetadataEntry> metadata = new ArrayList<>();
            for(String mdString: mdEntries){
                List<MetadataEntry> mdList = mdMapper.readValue(mdString, compoundRef);
                metadata.addAll(mdList);
            }
            return metadata;
        } catch (IOException ex){
            throw new RuntimeException("Unable to deserialize metadata", ex);
        }
    }
}