package org.srs.datacat.shared;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import java.io.IOException;
import junit.framework.TestCase;
import org.srs.datacat.shared.dataset.FlatDataset;

/**
 *
 * @author bvan
 */
public class DatasetDeserialization extends TestCase {

    
    String datasetBase = 
            "    \"name\": \"000006\",\n"
            + "    \"path\": \"/CTA/MC/MST61_B100_Z2000/corsika/gamma\",\n"
            + "    \"versionMetadata\": [{\n"
            + "        \"key\": \"nEnergyMaxLog10GeV\",\n"
            + "        \"value\": 4,\n"
            + "        \"$type\": \"integer\"\n"
            + "    }, {\n"
            + "        \"key\": \"nPrimaryAzimuthDeg\",\n"
            + "        \"value\": 0,\n"
            + "        \"$type\": \"integer\"\n"
            + "    }, {\n"
            + "        \"key\": \"nEnergyMinLog10GeV\",\n"
            + "        \"value\": 1,\n"
            + "        \"$type\": \"integer\"\n"
            + "    }],\n"
            + "    \"versionId\": 0,\n"
            + "    \"resource\": \"/nfs/farm/g/agis/u01/CTASims/MC/MST61_B100_Z2000/corsika/gamma_E1000_4000_ZN200_AZ000_000007.dat.gz\",\n"
            + "    \"size\": 4650118345,\n"
            + "    \"site\": \"SLAC\",\n"
            + "    \"dataType\": \"CTAEVENTIO\",\n"
            + "    \"fileFormat\": \"dat.gz\"\n"
            + "}";

    String datasetWithNoType = "{ " + datasetBase;
    String datasetWithType = "{ \"$type\":\"dataset\", " + datasetBase;
    
    public void testNoType() throws IOException{
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        AnnotationIntrospector pair = new AnnotationIntrospectorPair(primary, secondary);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector( pair );
        
        Dataset fds;
        
        fds = mapper.readValue( datasetWithType, Dataset.class);
        //mapper.enableDefaultTyping();
        fds = mapper.readValue( datasetWithNoType, new TypeReference<FlatDataset>(){});
                System.out.println(fds.getFileFormat());

        fds = mapper.readValue( datasetWithNoType, new TypeReference<FlatDataset>(){});
                System.out.println(fds.getFileFormat());

        fds = mapper.readValue( datasetWithNoType, FlatDataset.class);
                System.out.println(fds.getFileFormat());

    }
}
