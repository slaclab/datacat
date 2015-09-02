package org.srs.datacat.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.configuration.ConfigurationException;
import org.glassfish.jersey.filter.LoggingFilter;
import org.srs.datacat.client.exception.DcClientException;
import org.srs.datacat.client.exception.DcException;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.shared.Provider;

/**
 *
 * @author bvan
 */
public class ClientTest {

    
    // TODO: Integrate in with a webserver instance

    public void testMain() throws Exception{
        
        Client c = new Client(new URI("http://lsst-db2:8180/datacat-v0.3/r"));
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

        HashMap<String, Object> md = new HashMap<>();
        md.put("someFloat4", new Double(43211));
        DatasetModel ds = new Provider().getDatasetBuilder().versionMetadata(md).build();
        try{
            c.patchDataset("/LSST/dataset0001", null, "SLAC", ds);
        } catch (DcClientException e){
            System.out.println(e.toString());
        } catch (DcException ex){
            System.out.println(ex.toString());
        }

    }
    
    public void testX() throws IOException, ConfigurationException, URISyntaxException{
        Map<String,String> config = Config.defaultConfig("0.3-dev");
        
        Client c = ClientBuilder.newBuilder(config)
                .addClientRequestFilter(new LoggingFilter()).build();
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
