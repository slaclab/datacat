 
package org.srs.datacat.vfs;

import org.srs.datacat.vfs.DcPath;
import org.srs.datacat.vfs.DcFileSystemProvider;
import org.srs.datacat.vfs.DcUriUtils;
import org.srs.datacat.vfs.DcFile;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import javax.sql.DataSource;
import junit.framework.TestCase;
import org.junit.AfterClass;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.srs.datacat.shared.DatacatObject;

import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.LogicalFolder;
import org.srs.datacat.dao.sql.DatasetSqlDAOTest;
import org.srs.datacat.model.DatacatRecord;
import org.srs.datacat.security.DcUser;
import org.srs.datacat.test.DbHarness;

import org.srs.datacat.vfs.attribute.ContainerCreationAttribute;
import org.srs.datacat.vfs.attribute.DatasetOption;
import org.srs.vfs.AbstractPath;

/**
 *
 * @author bvan
 */
public class DcFileSystemProviderTest {
    
    static DbHarness harness;
    
    public DcFileSystemProviderTest(){ }
    
    @BeforeClass
    public static void setUpDb() throws SQLException, IOException{
        DatasetSqlDAOTest.setUpDb();
        harness = DbHarness.getDbHarness();
        DataSource d = harness.getDataSource();
        DatasetSqlDAOTest.addRecords(d.getConnection());
    }
    
    @AfterClass
    public static void tearDownDb() throws Exception{DatasetSqlDAOTest.removeRecords( harness.getDataSource().getConnection());
    }
    
    @Before
    public void setUp() throws IOException, SQLException{
        DcFileSystemProvider provider  = new DcFileSystemProvider(harness.getDataSource(), TestUtils.getLookupService());
        URI uri = DcUriUtils.toFsUri( "/", (DcUser) null, "SRS");
        DcPath rootPath = provider.getPath( uri );
        try(DirectoryStream<Path> s = provider.newDirectoryStream( rootPath )){
            for(Path p: s){
                System.out.println(p.toString());
            }
        }
        
        provider  = new DcFileSystemProvider(harness.getDataSource(), TestUtils.getLookupService());
        
        DatacatRecord o = provider.resolveFile(rootPath.resolve("testpath")).getAttributeView(DcFile.class).getObject();
        
        long t0 = System.currentTimeMillis();
        try(DirectoryStream<? extends AbstractPath> cstream = provider.unCachedDirectoryStream( rootPath.resolve("testpath") )){
            for(Iterator<? extends AbstractPath> iter = cstream.iterator(); iter.hasNext();){
                iter.next();
                //System.out.println(iter.next().toString());
            }
        }
        System.out.println("uncached directory stream took:" + (System.currentTimeMillis() - t0));
        
        t0 = System.currentTimeMillis();
        for(int i = 0; i <100; i++){
            try(DirectoryStream<Path> cstream = provider.newDirectoryStream( rootPath.resolve("testpath") )){
                for(Iterator<Path> iter = cstream.iterator(); iter.hasNext();){
                    iter.next();
                    //System.out.println(iter.next().toString());
                }
            }   
        }
        
        System.out.println("100 cached directory streams took:" + (System.currentTimeMillis() - t0));
        
        System.out.println("Walking tree...\n\n");
        Files.walkFileTree( rootPath.resolve("testpath"), new SimpleFileVisitor<Path>() {
            int filesVisited = 0;
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException{
                if(filesVisited > 50){
                    return FileVisitResult.TERMINATE;
                }
                filesVisited++;
                //StringBuilder sb = new StringBuilder();
                //for(int i = 0; i< file.getNameCount(); sb.append("  "), i++);
                //sb.append(file.toString());
                //System.out.println(sb.toString());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e)
                    throws IOException{
                if(e == null){
                    //StringBuilder sb = new StringBuilder();
                    //for(int i = 0; i < dir.getNameCount(); sb.append( "  " ), i++);
                    //sb.append( "in directory: ");
                    //sb.append( dir.toString() );
                    //System.out.println(sb.toString());
                    return FileVisitResult.CONTINUE;
                } else {
                    // directory iteration failed
                    throw e;
                }
            }
        } );
        System.out.println(o.toString());
    }

    @Test
    public void testCreateDataset() throws IOException{
        DcFileSystemProvider provider  = new DcFileSystemProvider(harness.getDataSource(), TestUtils.getLookupService());
        
        Dataset.Builder builder = new Dataset.Builder();
        builder.name("testCaseDataset001");
        builder.dataType(TestUtils.TEST_DATATYPE_01);
        builder.fileFormat(TestUtils.TEST_FILEFORMAT_01);
        builder.datasetSource( TestUtils.TEST_DATASET_SOURCE);
        
        Dataset request = builder.build();
        DcPath parentPath = provider.getPath( DcUriUtils.toFsUri(TestUtils.TEST_BASE_PATH, TestUtils.TEST_USER, "SRS"));
        DcPath filePath = parentPath.resolve(request.getName());
        HashSet<DatasetOption> options = new HashSet<>(Arrays.asList( DatasetOption.CREATE_NODE));
        provider.createDataset( filePath, request, options);
    }
    
    @Test
    public void testCreateDeleteDirectory() throws IOException {
        DcFileSystemProvider provider  = new DcFileSystemProvider(harness.getDataSource(), TestUtils.getLookupService());
        
        String folderName = "createFolderTest";
        LogicalFolder request = new LogicalFolder(new DatacatObject(0L, 0L, folderName));
        ContainerCreationAttribute attr = new ContainerCreationAttribute(request);
        URI uri = DcUriUtils.toFsUri(TestUtils.TEST_BASE_PATH, TestUtils.TEST_USER, "SRS");
        DcPath path =  provider.getPath(uri);
        provider.createDirectory(path.resolve(folderName), attr);
        provider.createDirectory(path.resolve(folderName).resolve(folderName), attr);
        
        // directory not empty
        try {
            provider.delete(path.resolve(folderName));
            TestCase.fail( "Should have failed deleting directory");
        } catch (DirectoryNotEmptyException ex){}
        
        provider.delete(path.resolve(folderName).resolve(folderName));
        provider.delete(path.resolve(folderName));
        
        try {
            // File should already be deleted
            provider.delete(path.resolve(folderName));
        } catch (NoSuchFileException ex){}
    }

    
}
