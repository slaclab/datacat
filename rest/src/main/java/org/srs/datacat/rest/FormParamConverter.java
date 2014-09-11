
package org.srs.datacat.rest;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.dataset.FlatDataset;

/**
 *
 * @author bvan
 */
public class FormParamConverter {

    final static HashMap<String, Method> containerSetters = new HashMap<>();
    final static HashMap<String, Method> flatDatasetSetters = new HashMap<>();

    static {
        
        FlatDataset.Builder dsbuilder = new FlatDataset.Builder();
        for(Method m: dsbuilder.getClass().getMethods()){
            if(m.getAnnotation( JsonSetter.class ) != null){
                containerSetters.put( m.getName(), m );
            }
        }
        
        DatasetContainer.Builder builder = new DatasetContainer.Builder();
        for(Method m: builder.getClass().getMethods()){
            if(m.getAnnotation( JsonSetter.class ) != null){
                containerSetters.put( m.getName(), m );
            }
        }

    }
    
    public static DatasetContainer.Builder getContainerBuilder(DatacatObject.Type type, MultivaluedMap<String,String> formParams){
        DatasetContainer.Builder builder = new DatasetContainer.Builder();
        builder.jsonType(type.toString());
        ObjectMapper mapper = new ObjectMapper();
        buildFromParams(mapper, builder, containerSetters, formParams);
        return builder;
    }

    public static FlatDataset.Builder getDatasetBuilder(MultivaluedMap<String,String> formParams){
        FlatDataset.Builder builder = new FlatDataset.Builder();
        ObjectMapper mapper = new ObjectMapper();
        buildFromParams(mapper, builder, flatDatasetSetters, formParams);
        return builder;
    }
    
    private static void buildFromParams(ObjectMapper mapper, Object builder, HashMap<String, Method> setters, MultivaluedMap<String,String> formParams){
        try {
            for(String key: formParams.keySet()){
                Method m = setters.get( key );
                List<String> lValue = formParams.get( key );
                if(lValue.size() != 1){
                    throw new RuntimeException( "Only one value per parameter is supported" );
                }
                Class<?> targetValueType = m.getParameterTypes()[0]; // Should only be one
                Object value = mapper.convertValue( lValue.get( 0 ), targetValueType );
                m.invoke( builder, value );
            }
        } catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) { 
            throw new RuntimeException("Error attempting to build object. Check builder @JsonSetter annotations", ex);
        }
    }
}
