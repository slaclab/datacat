
package org.srs.datacat.vfs;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.RecordType;
import org.srs.datacat.model.dataset.DatasetOption;
import org.srs.datacat.model.container.DatasetContainerBuilder;
import org.srs.datacat.model.security.DcGroup;
import org.srs.datacat.model.security.DcUser;

import org.srs.datacat.security.DcUserLookupService;
import org.srs.datacat.test.HSqlDbHarness;
import org.srs.datacat.vfs.attribute.ContainerCreationAttribute;

import org.srs.datacat.test.DbHarness;

/**
 *
 * @author bvan
 */
public class TestUtils {
    
    
    public static void generateDatasets(DcPath root, DcFileSystemProvider provider, int folders, int datasets) throws IOException{
        DcPath parent = root.resolve( "/testpath");
        DatasetContainerBuilder builder = (DatasetContainerBuilder) provider.getModelProvider()
                .getContainerBuilder().type(RecordType.FOLDER);

        /*
            The following is faster:
            To create 100 folders, THEN create datasets in each of those folders
        */
        // Create 10 folders
        for(int i = 0; i < folders; i++){
            String name =String.format("folder%05d", i);
            DcPath newPath = parent.resolve(name);
            builder.name(name);
            provider.createDirectory( newPath, new ContainerCreationAttribute(builder.build()) );
        }
        
        List opts = Arrays.asList(DatasetOption.CREATE_NODE, DatasetOption.CREATE_VERSION, DatasetOption.SKIP_NODE_CHECK);
        HashSet<DatasetOption> options = new HashSet<>(opts);
        // Create 20k datasets
        for(int i = 0; i < folders; i++){
            String name =String.format("folder%05d", i);
            DcPath newPath = parent.resolve(name);
            for(int j = 0; j < datasets; j++){
            DatasetModel.Builder dsBuilder = provider.getModelProvider().getDatasetBuilder();
                name = String.format("dataset%05d", j);
                dsBuilder.name(name);
                dsBuilder.dataType(HSqlDbHarness.JUNIT_DATASET_DATATYPE);
                dsBuilder.datasetSource(HSqlDbHarness.JUNIT_DATASET_DATASOURCE);
                dsBuilder.fileFormat(HSqlDbHarness.JUNIT_DATASET_FILEFORMAT);
                dsBuilder.versionId( DatasetView.NEW_VER );
                HashMap<String, Object> metadata = new HashMap<>();
                metadata.put(DbHarness.numberName, DbHarness.numberMdValues[i % 4]);
                metadata.put(DbHarness.alphaName, DbHarness.alphaMdValues[j % 4]);
                dsBuilder.versionMetadata( metadata );
                provider.createDataset(newPath.resolve(name), dsBuilder.build(), options );
            }
        }
    
    }
    
    public static DcUserLookupService getLookupService(){
        return new DcUserLookupService(){

            @Override
            public DcUser lookupPrincipalByName(String name) throws IOException{
                return super.lookupPrincipalByName( name );
            }

            @Override
            public DcGroup lookupPrincipalByGroupName(String group) throws IOException{
                return super.lookupPrincipalByGroupName( group );
            }


            @Override
            public Set<DcGroup> lookupGroupsForUser(DcUser member) throws IOException {
                Set<DcGroup> ug = new HashSet<>(super.lookupGroupsForUser( member ));
                if(member != null && member.getName().equals( "test_user")){
                    ug.add( new DcGroup("test_group","SRS"));
                }
                return ug;
            }
        };
    }

}
