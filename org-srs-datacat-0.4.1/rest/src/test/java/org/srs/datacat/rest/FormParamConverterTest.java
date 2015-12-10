
package org.srs.datacat.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.junit.Test;
import org.srs.datacat.model.dataset.DatasetLocationModel;
import org.srs.datacat.model.DatasetView;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.DatasetLocation;
import org.srs.datacat.shared.FlatDataset;
import org.srs.datacat.shared.FullDataset;
import org.srs.datacat.test.HSqlDbHarness;
import org.srs.datacat.shared.metadata.MetadataEntry;


/**
 *
 * @author bvan
 */
public class FormParamConverterTest {
    ObjectMapper mdMapper = new ObjectMapper();
    
    public FormParamConverterTest(){
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        AnnotationIntrospector pair = new AnnotationIntrospectorPair(primary, secondary);
        mdMapper.setAnnotationIntrospector( pair );
    }
    
    
    private HashMap<String, List<String>> startDataset(String name){
        HashMap<String,List<String>> flatMap = new HashMap<>();
        flatMap.put( "name", Arrays.asList(name));
        flatMap.put( "dataType", Arrays.asList(HSqlDbHarness.JUNIT_DATASET_DATATYPE));
        flatMap.put( "datasetSource", Arrays.asList(HSqlDbHarness.JUNIT_DATASET_DATASOURCE));
        flatMap.put( "fileFormat", Arrays.asList(HSqlDbHarness.JUNIT_DATASET_FILEFORMAT));
        flatMap.put( "versionId", Arrays.asList(Integer.toString(DatasetView.NEW_VER)));
        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put( "num", 1234);
        metadata.put( "alpha", "abc");
        String md = "";
        try {
            md = mdMapper.writeValueAsString(MetadataEntry.toList( metadata ));
            System.out.println(md);
        } catch(JsonProcessingException ex) { }
        flatMap.put("versionMetadata", Arrays.asList(md));
        return flatMap;
    }
    
    @Test
    public void testFlatDataset(){
        HashMap<String, List<String>> dsMap = null;
        Dataset.Builder b = null;
        FlatDataset ds = null;
        dsMap = startDataset("hello");
        b = FormParamConverter.getDatasetBuilder( dsMap );
        ds = (FlatDataset) b.build();
        TestCase.assertEquals("hello", ds.getName());
        TestCase.assertEquals(HSqlDbHarness.JUNIT_DATASET_DATASOURCE, ds.getDatasetSource());
        System.out.println(ds.isMaster());
        
        dsMap = startDataset("test2");
        dsMap.put("resource", Arrays.asList("file:///afs/slac/fakeFile.txt"));
        dsMap.put("site", Arrays.asList("SLAC"));
        b = FormParamConverter.getDatasetBuilder( dsMap );
        ds = (FlatDataset) b.build();
        TestCase.assertEquals("test2", ds.getName());
        TestCase.assertEquals("file:///afs/slac/fakeFile.txt", ds.getResource());
        TestCase.assertEquals("SLAC", ds.getSite());
        System.out.println(ds.isMaster());
        
        String locationId = null;
        locationId = "location1.";
        dsMap = startDataset("test3");
        dsMap.put(locationId + "resource", Arrays.asList("file:///afs/slac/fakeFile.txt"));
        dsMap.put(locationId + "site", Arrays.asList("SLAC"));
        dsMap.put(locationId + "master", Arrays.asList("true"));
        b = FormParamConverter.getDatasetBuilder( dsMap );
        ds = (FlatDataset) b.build();
        TestCase.assertEquals("test3", ds.getName());
        TestCase.assertEquals("file:///afs/slac/fakeFile.txt", ds.getResource());
        TestCase.assertEquals("SLAC", ds.getSite());
        TestCase.assertTrue(ds.isMaster());
        
    }
    
    public void testFullDataset(){
        HashMap<String, List<String>> dsMap = null;
        List<DatasetLocationModel> locations = null;
        Dataset.Builder b = null;
        FullDataset ds = null;
        String locationId = null;
        
        // Ordered location
        dsMap = startDataset("test1");
        locationId = "location1.";
        dsMap.put(locationId + "resource", Arrays.asList("file:///afs/slac/fakeFile1.txt"));
        dsMap.put(locationId + "site", Arrays.asList("SLAC1"));
        dsMap.put(locationId + "master", Arrays.asList("true"));
        
        locationId = "location2.";
        dsMap.put(locationId + "resource", Arrays.asList("file:///afs/slac/fakeFile2.txt"));
        dsMap.put(locationId + "site", Arrays.asList("SLAC2"));

        b = FormParamConverter.getDatasetBuilder( dsMap );
        ds = (FullDataset) b.build();
        locations = new ArrayList<>(ds.getLocations());
        
        TestCase.assertEquals("test1", ds.getName());
        
        DatasetLocationModel loc1 = locations.get(0);
        TestCase.assertEquals("file:///afs/slac/fakeFile1.txt", loc1.getResource());
        TestCase.assertEquals("SLAC1", loc1.getSite());
        TestCase.assertTrue(loc1.isMaster());
        DatasetLocationModel loc2 = locations.get(1);
        TestCase.assertEquals("file:///afs/slac/fakeFile2.txt", loc2.getResource());
        TestCase.assertEquals("SLAC2", loc2.getSite());
        
        // Add the location in a different order (check sorting)
        dsMap = startDataset("test2");
        locationId = "location2.";
        dsMap.put(locationId + "resource", Arrays.asList("file:///afs/slac/fakeFile2.txt"));
        dsMap.put(locationId + "site", Arrays.asList("SLAC2"));
        
        locationId = "location1.";
        dsMap.put(locationId + "resource", Arrays.asList("file:///afs/slac/fakeFile1.txt"));
        dsMap.put(locationId + "site", Arrays.asList("SLAC1"));
        dsMap.put(locationId + "master", Arrays.asList("true"));

        b = FormParamConverter.getDatasetBuilder( dsMap );
        ds = (FullDataset) b.build();
        locations = new ArrayList<>(ds.getLocations());
        
        TestCase.assertEquals("test3", ds.getName());
        loc1 = locations.get(0);
        TestCase.assertEquals("file:///afs/slac/fakeFile1.txt", loc1.getResource());
        TestCase.assertEquals("SLAC1", loc1.getSite());
        TestCase.assertTrue(loc1.isMaster());
        loc2 = locations.get(1);
        TestCase.assertEquals("file:///afs/slac/fakeFile2.txt", loc2.getResource());
        TestCase.assertEquals("SLAC2", loc2.getSite());
    }
}
