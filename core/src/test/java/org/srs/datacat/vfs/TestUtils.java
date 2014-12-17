
package org.srs.datacat.vfs;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.security.DcGroup;
import org.srs.datacat.security.DcUserLookupService;
import org.srs.datacat.security.DcUser;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.LogicalFolder;
import org.srs.datacat.test.HSqlDbHarness;
import org.srs.datacat.vfs.attribute.ContainerCreationAttribute;
import org.srs.datacat.model.dataset.DatasetOption;

/**
 *
 * @author bvan
 */
public class TestUtils {
    
    public static String alphaMdValues[] = {"abc","def","ghi","jkl"};
    public static Number numberMdValues[] = {0, 3.14159f, 4294967296L, -1.0000000001d};
    public static String alphaName = "alpha";
    public static String numberName = "num";
    
    public static final String TEST_BASE_NAME = "junit";
    public static final String TEST_USER = "test_user";
    public static final String TEST_BASE_PATH = "/" + TEST_BASE_NAME;
    public static final String TEST_FILEFORMAT_01 = "junit.test";
    public static final String TEST_DATASET_SOURCE = "JUNIT";
    public static final String TEST_DATATYPE_01 = "JUNIT_TEST01";
    
    public static void generateDatasets(DcPath root, DcFileSystemProvider provider, int folders, int datasets) throws IOException{
        DcPath parent = root.resolve( "/testpath");
        LogicalFolder.Builder builder = new LogicalFolder.Builder();

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
            Dataset.Builder dsBuilder = new Dataset.Builder();
                name = String.format("dataset%05d", j);
                dsBuilder.name(name);
                dsBuilder.dataType(HSqlDbHarness.JUNIT_DATASET_DATATYPE);
                dsBuilder.datasetSource(HSqlDbHarness.JUNIT_DATASET_DATASOURCE);
                dsBuilder.fileFormat(HSqlDbHarness.JUNIT_DATASET_FILEFORMAT);
                dsBuilder.versionId( DatasetView.NEW_VER );
                HashMap<String, Object> metadata = new HashMap<>();
                metadata.put( numberName, numberMdValues[i % 4]);
                metadata.put( alphaName, alphaMdValues[j % 4]);
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
            public Set<DcGroup> lookupGroupsForUser(DcUser member){
                Set<DcGroup> ug = new HashSet<>(super.lookupGroupsForUser( member ));
                if(member != null && member.getName().equals( "test_user")){
                    ug.add( new DcGroup("test_group","SRS"));
                }
                return ug;
            }
        };
    }

}
