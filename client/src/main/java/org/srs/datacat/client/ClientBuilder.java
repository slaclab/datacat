
package org.srs.datacat.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Feature;
import org.srs.datacat.client.auth.AuthProvider;

/**
 *
 * @author bvan
 */
public class ClientBuilder {
    private URI url;
    private List<ClientRequestFilter> clientRequestFilters = new ArrayList<>();
    private List<ClientResponseFilter> clientResponseFilters = new ArrayList<>();
    private Map<String, Object> clientProperties = new HashMap<>();
    private List<Feature> features = new ArrayList<>();
    
    public ClientBuilder(){ }
    
    public ClientBuilder setUrl(String val) throws URISyntaxException{
        this.url = new URI(val);
        return this;
    }
    
    public ClientBuilder setUrl(URI val) throws URISyntaxException{
        this.url = val;
        return this;
    }

    public ClientBuilder addClientRequestFilter(ClientRequestFilter clientRequestFilter){
        clientRequestFilters.add(clientRequestFilter);
        return this;
    }

    public ClientBuilder addClientResponseFilter(ClientResponseFilter clientResponseFilter){
        clientResponseFilters.add(clientResponseFilter);
        return this;
    }
    
    public ClientBuilder addProperty(String name, Object value){
        clientProperties.put(name, value);
        return this;
    }

    public ClientBuilder addFeature(Feature feature){
        features.add(feature);
        return this;
    }
    
    public Client build(){
        return new Client(this.url, this.clientRequestFilters, 
                this.clientResponseFilters, this.features, this.clientProperties);
    }
    
    
    public static ClientBuilder newBuilder(){
        return new ClientBuilder();
    }
    
    public static ClientBuilder newBuilder(Map<String, String> config) throws URISyntaxException{
        ClientBuilder builder = new ClientBuilder();
        ClientRequestFilter authFilter = AuthProvider.fromConfig(config);
        if(authFilter != null){
            builder.addClientRequestFilter(authFilter);
        }
        builder.setUrl(config.get("url"));
        return builder;
    }
    
}
