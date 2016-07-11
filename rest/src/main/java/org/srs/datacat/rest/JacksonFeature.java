
package org.srs.datacat.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import java.text.SimpleDateFormat;
import java.util.Map.Entry;
import java.util.TimeZone;
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
    
    // Private no-arg constructor
    private JacksonFeature(){}

    public JacksonFeature(ModelProvider provider){
        if(jsonProvider == null){
            ObjectMapper jsonMapper = new ObjectMapper();
            jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            XmlMapper xmlMapper = new XmlMapper();
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            jsonMapper.setDateFormat(dateFormat);
            xmlMapper.setDateFormat(dateFormat);
            for(Entry<Class, Class> e: provider.modelProviders().entrySet()){
                jsonMapper.addMixIn(e.getKey(), e.getValue());
                xmlMapper.addMixIn(e.getKey(), e.getValue());
            }
            jsonProvider = new JacksonJsonProvider(jsonMapper);
            xmlProvider = new JacksonXMLProvider(xmlMapper);
        }
    }

    @Override
    public boolean configure(final FeatureContext context){
        final String disableMoxy = CommonProperties.MOXY_JSON_FEATURE_DISABLE + '.' + 
                context.getConfiguration().getRuntimeType().name().toLowerCase();
        context.property(disableMoxy, true);
        context.register(xmlProvider, MessageBodyReader.class,MessageBodyWriter.class);
        context.register(jsonProvider, MessageBodyReader.class,MessageBodyWriter.class);
        return true;
    }

}
