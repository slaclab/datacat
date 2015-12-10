package org.srs.datacat.dao;

import com.google.common.base.Optional;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.model.DatasetContainer;
import org.srs.datacat.model.DatasetModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.model.RecordType;
import org.srs.datacat.model.dataset.DatasetLocationModel;
import org.srs.datacat.model.dataset.DatasetOption;
import org.srs.datacat.model.dataset.DatasetVersionModel;
import org.srs.datacat.model.dataset.DatasetViewInfoModel;
import org.srs.datacat.test.DbHarness;
import org.srs.datacat.test.HSqlDbHarness;

import static org.mockito.Mockito.*;
import org.srs.datacat.model.DatacatNode;

/**
 *
 * @author bvan
 */
public class DAOTestUtils {

    public static void generateDatasets(DAOFactory factory, int inFolders, int datasets) throws IOException{
        DatacatRecord tpObject = null;
        try(final BaseDAO dao = factory.newBaseDAO()) {
            tpObject = dao.getObjectInParent(dao.getObjectInParent(null, "/"), "testpath");
        }

        generateFolders(factory, inFolders);

        List opts = Arrays.asList(DatasetOption.CREATE_NODE, DatasetOption.CREATE_VERSION, DatasetOption.SKIP_NODE_CHECK);
        HashSet<DatasetOption> options = new HashSet<>(opts);
        for(int i = 0; i < inFolders; i++){
            String parentName = String.format("folder%05d", i);
            DatacatRecord parentObject = null;
            try(DatasetDAO dao = factory.newDatasetDAO()) {
                parentObject = dao.getObjectInParent(tpObject, parentName);
            
                for(int j = 0; j < datasets; j++){
                    String name = String.format("dataset%05d", j);
                    DatasetModel ds = mock(DatasetModel.class);
                    DatasetViewInfoModel view = mock(DatasetViewInfoModel.class);
                    DatasetVersionModel version = mock(DatasetVersionModel.class);
                    HashMap<String, Object> metadata = new HashMap<>();
                    metadata.put(DbHarness.numberName, DbHarness.numberMdValues[i % 4]);
                    metadata.put(DbHarness.alphaName, DbHarness.alphaMdValues[j % 4]);
                    
                    when(ds.getName()).thenReturn(name);
                    when(ds.getDataType()).thenReturn(HSqlDbHarness.JUNIT_DATASET_DATATYPE);
                    when(ds.getFileFormat()).thenReturn(HSqlDbHarness.JUNIT_DATASET_FILEFORMAT);
                    
                    when(version.getVersionId()).thenReturn(DatasetView.NEW_VER);
                    when(version.getMetadataMap()).thenReturn(metadata);
                    when(version.getDatasetSource()).thenReturn(HSqlDbHarness.JUNIT_DATASET_DATASOURCE);
                    
                    when(view.versionOpt()).thenReturn(Optional.of(version));
                    when(view.getVersion()).thenReturn(version);
                    when(view.locationsOpt()).thenReturn(Optional.<Set<DatasetLocationModel>>absent());

                    dao.createDataset(parentObject, name, Optional.of(ds), Optional.of(view), options);
                }
                dao.commit();
            }
        }
    }

    public static void generateFolders(DAOFactory factory, int folders) throws IOException{
        DatacatRecord tpObject = null;
        try(ContainerDAO dao = factory.newContainerDAO()) {
            tpObject = dao.getObjectInParent(dao.getObjectInParent(null, "/"), "testpath");
            for(int i = 0; i < folders; i++){
                String name = String.format("folder%05d", i);
                DatasetContainer cont = mock(DatasetContainer.class);
                when(cont.getType()).thenReturn(RecordType.FOLDER);
                dao.createNode(tpObject, name, cont);
            }
            dao.commit();
        }
    }

    public static List<DatacatNode> getFolders(DAOFactory factory, int folders) throws IOException{
        List<DatacatNode> ret = new ArrayList<>();
        try(ContainerDAO dao = factory.newContainerDAO()) {
            DatacatRecord tpObject = dao.getObjectInParent(dao.getObjectInParent(null, "/"), "testpath");

            for(int i = 0; i < folders; i++){
                String name = String.format("folder%05d", i);
                ret.add(dao.getObjectInParent(tpObject, name));
            }
            dao.commit();
        }
        return ret;
    }
    
    public static List<DatacatNode> getContainers(DAOFactory factory) throws IOException{
        List<DatacatNode> ret = new ArrayList<>();
        try(ContainerDAO dao = factory.newContainerDAO()) {
            DatacatRecord tpObject = dao.getObjectInParent(dao.getObjectInParent(null, "/"), "testpath");
                
            try (DirectoryStream<DatacatNode> stream = dao.getChildrenStream(tpObject, Optional.<DatasetView>absent())){
                for(DatacatNode node: stream){
                    if(node.getType().isContainer()){
                        ret.add(node);
                    }
                }
            }
        }
        return ret;
    }

}
