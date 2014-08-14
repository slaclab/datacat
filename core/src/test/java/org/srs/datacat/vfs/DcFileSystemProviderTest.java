/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.srs.datacat.vfs;

import java.io.FileNotFoundException;
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
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import org.junit.AfterClass;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.srs.datacat.shared.DatacatObject;
import org.srs.vfs.VirtualFile.FileType;
import org.srs.datacat.shared.Dataset;
import org.srs.datacat.shared.LogicalFolder;
import org.srs.datacat.sql.ContainerDAO;
import org.srs.datacat.sql.DatasetDAO;
import org.srs.datacat.sql.DatasetDAOTest;
import org.srs.datacat.sql.Utils;
import static org.srs.datacat.vfs.DcUriUtils.toFsUri;
import org.srs.datacat.vfs.attribute.ContainerCreationAttribute;
import org.srs.datacat.vfs.attribute.DatasetCreationAttribute;
import org.srs.datacat.vfs.attribute.DatasetOption;
import org.srs.vfs.PathUtils;

/**
 *
 * @author bvan
 */
public class DcFileSystemProviderTest {
    
    public DcFileSystemProviderTest(){ }
    
    @BeforeClass
    public static void setUpDb() throws SQLException, IOException{
        DatasetDAOTest.setUpDb();
    }
    
    @AfterClass
    public static void tearDownDb() throws Exception{
        DatasetDAOTest.tearDownDb();
    }
    
    @Before
    public void setUp() throws IOException, SQLException{
        DcFileSystemProvider provider  = new DcFileSystemProvider();
        URI uri = DcUriUtils.toFsUri( "/", null, "SRS");
        DcPath path = provider.getPath( uri );
        try(DirectoryStream<Path> s = Files.newDirectoryStream( path )){
            for(Path p: s){
                System.out.println(p.toString());
            }
        }
        
        provider  = new DcFileSystemProvider(true);

        System.out.println(path.toString());
        int ord = FileType.userType( provider.resolveFile(path).getType());
        System.out.println(DatacatObject.Type.values()[ord]);
        
        System.out.println(provider.resolveFile(path.resolve( "EXO")).getPath().toString());
        DatacatObject o = provider.resolveFile(path.resolve( "EXO")).getAttributeView(DcFile.class).getObject();
        
        long t0 = System.currentTimeMillis();
        try(DirectoryStream<Path> cstream = Files.newDirectoryStream( path.resolve("EXO") )){
            for(Iterator<Path> iter = cstream.iterator(); iter.hasNext();){
                iter.next();
                //System.out.println(iter.next().toString());
            }
        }
        System.out.println("uncached directory stream took:" + (System.currentTimeMillis() - t0));
                
        
        t0 = System.currentTimeMillis();
        for(int i = 0; i <100; i++){
            try(DirectoryStream<Path> cstream = Files.newDirectoryStream( path.resolve("EXO") )){
                for(Iterator<Path> iter = cstream.iterator(); iter.hasNext();){
                    iter.next();
                    //System.out.println(iter.next().toString());
                }
            }   
        }
        
        System.out.println("100 cached directory streams took:" + (System.currentTimeMillis() - t0));
        
        System.out.println("Walking tree...\n\n");
        Files.walkFileTree( path.resolve( "EXO"), new SimpleFileVisitor<Path>() {
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
        DcFileSystemProvider provider  = new DcFileSystemProvider();
        
        Dataset.Builder builder = new Dataset.Builder();
        builder.name("testCaseDataset001");
        builder.datasetDataType("TEST");
        builder.datasetFileFormat("TEST");
        Dataset request = builder.build();
        
        DcPath parentPath = provider.getPath( DcUriUtils.toFsUri( "/testpath/testfolder", null, "SRS"));
        DcPath filePath = parentPath.resolve(request.getName());
        DatasetCreationAttribute dsAttr = new DatasetCreationAttribute(request, DatasetOption.CREATE_NODE);
        //Files.createFile( parentPath, dsAttr );

    }
    
    @Test
    public void testCreateDeleteDirectory() throws IOException {
        DcFileSystemProvider provider  = new DcFileSystemProvider();
        
        String folderName = "createFolderTest";
        LogicalFolder request = new LogicalFolder(new DatacatObject(0L, 0L, folderName));
        ContainerCreationAttribute attr = new ContainerCreationAttribute(request);
        URI uri = DcUriUtils.toFsUri(DatasetDAOTest.TEST_BASE_PATH, null, "SRS");
        DcPath path =  provider.getPath(uri);
        provider.createDirectory(path.resolve(folderName), attr);
        provider.createDirectory(path.resolve(folderName).resolve(folderName), attr);
        
        // directory not empty
        try {
            provider.delete(path.resolve(folderName));
        } catch (DirectoryNotEmptyException ex){}
        
        provider.delete(path.resolve(folderName).resolve(folderName));
        provider.delete(path.resolve(folderName));
        
        try {
            // File should already be deleted
            provider.delete(path.resolve(folderName));
        } catch (FileNotFoundException ex){}
    }

    
}
