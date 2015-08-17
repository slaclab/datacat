
package org.srs.datacat.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import com.google.common.base.Optional;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import org.glassfish.jersey.CommonProperties;
import org.srs.datacat.client.resources.Path;
import org.srs.datacat.client.resources.Search;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetResultSetModel;
import org.srs.datacat.rest.ErrorResponse;
import org.srs.datacat.shared.Provider;

/**
 * Main client interface for RESTful API.
 * @author bvan
 */
public class Client {
    private javax.ws.rs.client.Client client;
    private WebTarget baseTarget;
    private Path pathResource;
    private Search searchResource;
    private AuthenticationFilter filter;
    
    /**
     * Exceptions that occur when making calls into the API.
     */
    public static class DcException extends RuntimeException { 
        
        private String type;
        private String code;
        private int status;
        
        public DcException(String resp, int status){
            super(resp);
            this.status = status;
            this.type = "ApplicationError";
        }
    
        public DcException(ErrorResponse err, int status){
            super(err.getMessage(), new Throwable(err.getCause()));
            this.type = err.getType();
            this.code = err.getCode();
            this.status = status;
        }
        
        public String getType(){
            return type;
        }

        public String getCode(){
            return code;
        }

        public int getStatus(){
            return status;
        }
    
    }
    
    public Client() throws MalformedURLException{
        init("http://lsst-db2.slac.stanford.edu:8180/rest-datacat-v1/r", null);
    }
    
    public Client(HttpServletRequest delegatedRequest) throws MalformedURLException{
        init("http://lsst-db2.slac.stanford.edu:8180/rest-datacat-v1/r", delegatedRequest);
    }
    
    public Client(String url) throws MalformedURLException{
        init(url, null);
    }
    
    public Client(String url, HttpServletRequest delegatedRequest) throws MalformedURLException{
        init(url, delegatedRequest);
    }
    
    /**
     * Jackson JSON/XML support.
     */
    public static class JacksonFeature implements Feature {
        static JacksonJsonProvider jsonProvider;
        static JacksonXMLProvider xmlProvider;
        
        public JacksonFeature(){
            if(jsonProvider == null){
                ObjectMapper jsonMapper = new ObjectMapper();
                jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                XmlMapper xmlMapper = new XmlMapper();
                for(Map.Entry<Class, Class> e : new Provider().modelProviders().entrySet()){
                    jsonMapper.addMixIn(e.getKey(), e.getValue());
                    xmlMapper.addMixIn(e.getKey(), e.getValue());
                }

                jsonProvider = new JacksonJsonProvider(jsonMapper);
                xmlProvider = new JacksonXMLProvider(xmlMapper);
            }
        }

        @Override
        public boolean configure(final FeatureContext context){
            final String disableMoxy = CommonProperties.MOXY_JSON_FEATURE_DISABLE + '.'
                    + context.getConfiguration().getRuntimeType().name().toLowerCase();
            context.property( disableMoxy, true );

            context.register( xmlProvider, MessageBodyReader.class, 
                    MessageBodyWriter.class );

            context.register( jsonProvider, MessageBodyReader.class, 
                    MessageBodyWriter.class );
            return true;
        }
    }
    
    private void init(String baseUrl, HttpServletRequest delegatedRequest) throws MalformedURLException{
        this.filter = new AuthenticationFilter(delegatedRequest);
        
        client = ClientBuilder.newBuilder()
                .register(this.filter)
                .register(new JacksonFeature())
                .build();
        baseTarget = client.target(baseUrl);
        this.pathResource = new Path(baseTarget);
        this.searchResource = new Search(baseTarget);
    }
    
    public List<DatacatNode> getChildren(String path){
        Response resp = pathResource.getChildren(path, Optional.<String>absent(), Optional.<String>absent(),
                Optional.<Integer>absent(), Optional.<Integer>absent());
        checkResponse(resp);
        return resp.readEntity(new GenericType<List<DatacatNode>>(){});
    }
    
    public List<DatacatNode> getChildren(String path, String versionId, String site){
        Response resp = pathResource.getChildren(path, Optional.fromNullable(versionId), Optional.fromNullable(site),
                Optional.<Integer>absent(), Optional.<Integer>absent());
        checkResponse(resp);
        return resp.readEntity(new GenericType<List<DatacatNode>>(){});
    }
    
    public List<DatacatNode> getChildren(String path, String versionId, String site, int offset, int max){
        Response resp = pathResource.getChildren(path, Optional.fromNullable(versionId), Optional.fromNullable(site),
                Optional.of(offset), Optional.of(max));
        checkResponse(resp);
        return resp.readEntity(new GenericType<List<DatacatNode>>(){});
    }
    
    public List<DatacatNode> getContainers(String path){
        Response resp = pathResource.getContainers(path);
        checkResponse(resp);
        return resp.readEntity(new GenericType<List<DatacatNode>>(){});
    }
    
    public DatacatNode getObject(String path){
        Response resp = pathResource.getObject(path, Optional.fromNullable("current"), Optional.fromNullable("all"));
        checkResponse(resp);
        return resp.readEntity(new GenericType<DatacatNode>(){});
    }
    
    public DatacatNode getContainer(String path, String stat){
        Response resp = pathResource.getContainer(path, Optional.<String>absent(), Optional.<String>absent(),
                Optional.of(stat));
        checkResponse(resp);
        return resp.readEntity(new GenericType<DatacatNode>(){});
    }
    
    public DatacatNode patchContainer(String path, DatasetContainer payload){
        Response resp = pathResource.patchContainer(path, 
                Entity.entity(payload, MediaType.APPLICATION_JSON));
        checkResponse(resp);
        return resp.readEntity(new GenericType<DatacatNode>(){});
    }
    
    public DatacatNode patchDataset(String path, String versionId, String site, 
            DatasetModel payload){
        Response resp = pathResource.patchDataset(path, Optional.of(versionId), Optional.of(site),
                Entity.entity(payload, MediaType.APPLICATION_JSON));
        checkResponse(resp);
        return resp.readEntity(new GenericType<DatacatNode>(){});
    }
    
    public DatasetResultSetModel searchForDatasets(String target, String versionId, String site, 
            String query, String[] sort, String show, int offset, int max){
        Response resp = searchResource.searchForDatasets(target, Optional.fromNullable(versionId), 
                Optional.fromNullable(site), Optional.fromNullable(query), 
                Optional.fromNullable(sort), Optional.fromNullable(show), 
                Optional.<Integer>fromNullable(offset), Optional.<Integer>fromNullable(max));
        checkResponse(resp);
        return resp.readEntity(new GenericType<DatasetResultSetModel>(){});
    }

    protected void checkResponse(Response resp) throws DcException {
        if(resp.getStatusInfo().getFamily() == Status.Family.SUCCESSFUL){
            return;
        }
        if(resp.getStatusInfo().getFamily() == Status.Family.CLIENT_ERROR ||
                resp.getStatusInfo().getFamily() == Status.Family.SERVER_ERROR){
            if(MediaType.APPLICATION_JSON_TYPE.isCompatible(resp.getMediaType())){
                ErrorResponse err = resp.readEntity(ErrorResponse.class);
                throw new DcException(err, resp.getStatus());
            }
            throw new DcException(resp.readEntity(String.class), resp.getStatus());
        }
    }

    public static void main(String[] argv) throws MalformedURLException{
        Client c = new Client();
        DatacatNode n = c.getContainer("/LSST", "dataset");
        n = c.getContainer("/LSST", "basic");
        n = c.getObject("/LSST");
        
        
        System.out.println(n.toString());
        List<? extends DatacatNode> children = c.getChildren("/LSST", "current", "master");
        for(DatacatNode child: children){
            System.out.println(child.toString());
        }
        children = c.searchForDatasets("/LSST", "current", "master", "", null, null, 0, 1000).getResults();
        for(DatacatNode child: children){
            System.out.println(child.toString());
            System.out.println(child.getClass());
        }
    }

}
