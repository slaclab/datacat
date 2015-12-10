
package org.srs.datacat.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import java.util.Map;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import org.glassfish.jersey.CommonProperties;
import org.srs.datacat.model.ModelProvider;

/**
 * Jackson JSON/XML support.
 */
public class JacksonFeature implements Feature {
    static JacksonJsonProvider jsonProvider;
    static JacksonXMLProvider xmlProvider;
    private ModelProvider provider;

    public JacksonFeature(ModelProvider provider){
        if(jsonProvider == null){
            ObjectMapper jsonMapper = new ObjectMapper();
            jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            XmlMapper xmlMapper = new XmlMapper();
            for(Map.Entry<Class, Class> e: provider.modelProviders().entrySet()){
                jsonMapper.addMixIn(e.getKey(), e.getValue());
                xmlMapper.addMixIn(e.getKey(), e.getValue());
            }
            jsonProvider = new JacksonJsonProvider(jsonMapper);
            xmlProvider = new JacksonXMLProvider(xmlMapper);
        }
    }
    
    ModelProvider getProvider(){
        return this.provider;
    }

    @Override
    public boolean configure(final FeatureContext context){
        final String disableMoxy = CommonProperties.MOXY_JSON_FEATURE_DISABLE + '.' + context.getConfiguration().
                getRuntimeType().name().toLowerCase();
        context.property(disableMoxy, true);
        context.register(xmlProvider, MessageBodyReader.class, MessageBodyWriter.class);
        context.register(jsonProvider, MessageBodyReader.class, MessageBodyWriter.class);
        return true;
    }

}
