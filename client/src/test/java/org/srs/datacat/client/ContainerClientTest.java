
package org.srs.datacat.client;

import java.io.IOException;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.ModelProvider;
import org.srs.datacat.model.RecordType;
import org.srs.datacat.shared.Provider;

/**
 *
 * @author bvan
 */
public class ContainerClientTest {
    
    public static void generateFolders(Client testClient, int folders) throws IOException{
        String parent = "/testpath";
        // Create n folders
        for(int i = 0; i < folders; i++){
            String name =String.format("folder%05d", i);
            ModelProvider provider = new Provider();
            DatasetContainer newContainer = (DatasetContainer) provider.getContainerBuilder()
                    .name(name)
                    .type(RecordType.FOLDER)
                    .build();
            
            testClient.createContainer(parent, newContainer);
        }
    }

}
