
package org.srs.datacat.shared;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import junit.framework.TestCase;
import org.srs.rest.shared.metadata.MetadataEntry;

/**
 *
 * @author bvan
 */
public class LatestDatasetTest extends TestCase {
    
    
    public LatestDatasetTest(String testName){
        super( testName );
    }
    
    @Override
    protected void setUp() throws Exception{
        super.setUp();
    }

    
    public void testFullDataset() throws JAXBException, IOException{
        FullDataset.Builder db =  new FullDataset.Builder();
        db.name("my-flat-dataset.txt");
        db.fileFormat("FITS");
        db.resource( "/path/to/somewhere");
        db.size( 1234L );
        db.runMin( 0L );
        db.runMax( 10L );
        
        db.latest( true );
        db.taskName( "faketask");
        db.versionId( 123 );
        
        FullDataset ds = db.build();
        
        ObjectMapper mapper = new ObjectMapper();
        //mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        StringWriter json = new StringWriter();
        mapper.writeValue( json, ds);
        System.out.println(json.toString());

        FullDataset newObject = mapper.readValue( json.toString(), FullDataset.class );
        System.out.println(newObject.getVersion());
        
        StringWriter newJson = new StringWriter();
        mapper.writeValue( newJson, newObject);
        System.out.println(newJson.toString());
        
        assertEquals("Marshalling, Unmarshalling, and Marshalling back failed", json.toString(), newJson.toString());
        
        mapper = new XmlMapper();
        mapper.writeValue(System.out, ds);
        /*JAXBContext jaxbContext = JAXBContext.newInstance( FullDataset.class );
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        // output pretty printed
        jaxbMarshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );        
        jaxbMarshaller.marshal( ds, System.out );
        */
    }
    
    
    public void testFlatDataset() throws JAXBException, IOException{
        Dataset.Builder db =  new Dataset.Builder();
        db.name("my-flat-dataset.txt");
        db.fileFormat("FITS");
        db.resource( "/path/to/somewhere");
        db.size( 1234L );
        db.runMin( 0L );
        db.runMax( 10L );
        
        db.latest( true );
        db.taskName( "faketask");
        db.versionId( 123 );
        List<MetadataEntry> m = new ArrayList<>();
        m.add( new MetadataEntry("hello", "world") );
        m.add( new MetadataEntry("how", "doyoudo") );
        //m.add( new MetadataEntry("number", 1234) );
        m.add( new MetadataEntry("number2", 1234.25) );
        db.versionMetadata( m );
        
        FlatDataset ds = (FlatDataset) db.build();
        
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();   
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        AnnotationIntrospector pair = new AnnotationIntrospectorPair(primary, secondary);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(primary);
        //mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        StringWriter json = new StringWriter();
        mapper.writeValue( json, ds);
        System.out.println(json.toString());

        DatacatObject newObject = mapper.readValue( json.toString(), DatacatObject.class );
        
        StringWriter newJson = new StringWriter();
        mapper.writeValue( newJson, newObject);
        System.out.println(json.toString());
        System.out.println(newJson.toString());
        assertEquals("Marshalling, Unmarshalling, and Marshalling back failed", json.toString(), newJson.toString());
    }
    
    public void testFlatDatasetXML() throws JAXBException, IOException {
        Dataset.Builder db =  new Dataset.Builder();
        db.name("my-flat-dataset.txt");
        db.fileFormat("FITS");
        db.resource( "/path/to/somewhere");
        db.size( 1234L );
        db.runMin( 0L );
        db.runMax( 10L );
        
        db.latest( true );
        db.taskName( "faketask");
        db.versionId( 123 );
        List<MetadataEntry> m = new ArrayList<>();
        m.add( new MetadataEntry("hello", "world") );
        m.add( new MetadataEntry("helloInt", 1234) );
        m.add( new MetadataEntry("helloDouble", 1234.4321) );
        db.versionMetadata( m );

        Dataset ds = db.build();
        
        String expected = 
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<flatDataset>\n"
                + "    <name>my-flat-dataset.txt</name>\n"
                + "    <fileFormat>FITS</fileFormat>\n"
                + "    <latest>true</latest>\n"
                + "    <taskName>faketask</taskName>\n"
                + "    <versionId>123</versionId>\n"
                + "    <versionMetadata>\n"
                + "        <entry>\n"
                + "            <key>hello</key>\n"
                + "            <string>world</string>\n"
                + "        </entry>\n"
                + "        <entry>\n"
                + "            <key>helloDouble</key>\n"
                + "            <decimal>1234.4321</decimal>\n"
                + "        </entry>\n"
                + "        <entry>\n"
                + "            <key>helloInt</key>\n"
                + "            <integer>1234</integer>\n"
                + "        </entry>\n"
                + "    </versionMetadata>\n"
                + "    <resource>/path/to/somewhere</resource>\n"
                + "    <runMax>10</runMax>\n"
                + "    <runMin>0</runMin>\n"
                + "    <size>1234</size>\n"
                + "</flatDataset>\n";
        
        expected = 
        "<?xml version='1.0' encoding='UTF-8'?>"
                + "<flatDataset _type=\"dataset#flat\">\n"
                + "  <name>my-flat-dataset.txt</name>\n"
                + "  <fileFormat>FITS</fileFormat>\n"
                + "  <versionId>123</versionId>\n"
                + "  <latest>true</latest>\n"
                + "  <versionMetadata>\n"
                + "    <entry>\n"
                + "      <key>helloDouble</key>\n"
                + "      <value>1234.4321</value>\n"
                + "      <type>decimal</type>\n"
                + "    </entry>\n"
                + "    <entry>\n"
                + "      <key>hello</key>\n"
                + "      <value>world</value>\n"
                + "      <type>string</type>\n"
                + "    </entry>\n"
                + "    <entry>\n"
                + "      <key>helloInt</key>\n"
                + "      <value>1234</value>\n"
                + "      <type>integer</type>\n"
                + "    </entry>\n"
                + "  </versionMetadata>\n"
                + "  <taskName>faketask</taskName>\n"
                + "  <resource>/path/to/somewhere</resource>\n"
                + "  <size>1234</size>\n"
                + "  <runMin>0</runMin>\n"
                + "  <runMax>10</runMax>\n"
                + "</flatDataset>";
        
        /*JAXBContext jaxbContext = JAXBContext.newInstance( FlatDataset.class );
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        
        
        // output pretty printed
        jaxbMarshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
        
        StringWriter xmlOut = new StringWriter();
        jaxbMarshaller.marshal( ds, xmlOut );*/

        JacksonXmlModule xmm = new JacksonXmlModule();
        //xmm.setDefaultUseWrapper(false);
        XmlFactory woodstoxFactory = new XmlFactory(new WstxInputFactory(), new WstxOutputFactory());
        XmlMapper mapper = new XmlMapper(woodstoxFactory, xmm);
        mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        String xmlOut = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ds);
        //assertEquals("Marshalled XML differs than expected", expected, xmlOut);
        
        /*
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        StringReader xmlIn = new StringReader(xmlOut.toString());
        System.out.println(jaxbUnmarshaller.unmarshal(xmlIn).toString());
        */   
    }
    
    public void testMultivaluedMap() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException{
        
        HashMap<String, List<String>> mmap = new HashMap<>();        
        mmap.put( "name", Arrays.asList("my-flat-dataset.txt"));
        mmap.put( "fileFormat",Arrays.asList("FITS"));
        mmap.put( "resource", Arrays.asList("/path/to/somewhere"));
        mmap.put( "size", Arrays.asList("1234"));
        mmap.put( "runMin", Arrays.asList("0"));
        mmap.put( "runMax", Arrays.asList("10"));
        mmap.put( "latest", Arrays.asList("true") );
        mmap.put( "taskName", Arrays.asList("faketask"));
        mmap.put( "versionId", Arrays.asList("123") );
        mmap.put( "versionMetadata", Arrays.asList("[ {\"key\":\"nRuns\", \"value\":\"hello\"} ]") );

        Dataset.Builder builder = new FlatDataset.Builder();
        HashMap<String, Method> methods = new HashMap<>();
        
        for(Method m: builder.getClass().getMethods()){
            if( m.getAnnotation( JsonSetter.class ) != null ){
                methods.put( m.getName(), m);
            }
        }
        
        ObjectMapper mapper =  new ObjectMapper();
        for(String key: mmap.keySet()){
            Method m = methods.get( key );
            Type params = m.getGenericParameterTypes()[0];
            JavaType jt = TypeFactory.defaultInstance().constructType( params );
            String sValue = mmap.get( key ).get( 0);
            if(jt.getRawClass().equals( String.class )){
                m.invoke(builder, sValue);
            } else {
                Object value = mapper.readValue( sValue, jt );
                m.invoke(builder, value);
            }
        }
        FlatDataset ds = (FlatDataset) builder.build();
        System.out.println(ds.getLocation().toString());
        
        System.out.println(ds.toString());

    }
    
    public static void main(String[] args) throws Exception{
        LatestDatasetTest l = new LatestDatasetTest("main");
        l.testFlatDataset();
    }

}
