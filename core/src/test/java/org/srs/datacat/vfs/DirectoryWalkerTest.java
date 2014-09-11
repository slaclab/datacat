
package org.srs.datacat.vfs;

import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import junit.framework.TestCase;
import org.apache.commons.dbcp2.DelegatingConnection;
import org.junit.Before;
import org.junit.Test;
import org.junit.BeforeClass;
import org.srs.datacat.test.HSqlDbHarness;
import org.srs.datacat.vfs.DirectoryWalker.ContainerVisitor;
import org.srs.vfs.PathUtils;

/**
 *
 * @author Brian Van Klaveren<bvan@slac.stanford.edu>
 */
public class DirectoryWalkerTest {
    static HSqlDbHarness harness;
    DcFileSystemProvider provider;
    DcPath root;
    
    public DirectoryWalkerTest(){
    }
    
    @BeforeClass
    public static void setUpDb() throws SQLException, IOException{
        harness = new HSqlDbHarness();
        DataSource d = harness.getDataSource();
        Connection c = ((DelegatingConnection)d.getConnection()).getInnermostDelegate();
        
    }
    
    @Before
    public void setUp() throws IOException{
        URI uri = DcUriUtils.toFsUri( "/", null, "SRS");
        provider = new DcFileSystemProvider(harness.getDataSource());
        root = provider.getPath( uri );
    }

    @Test
    public void testWalk() throws IOException{
        
        ContainerVisitor visitor;
        visitor = new ContainerVisitor(root.getFileSystem(), "glob:/te*");
        DirectoryWalker walker = new DirectoryWalker(provider,visitor,2);
        walker.walk(root);
        
        visitor = new ContainerVisitor(root.getFileSystem(), "glob:/te**");
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root);
        TestCase.assertEquals("/testpath/a/b/c", visitor.files.get(0).getPath().toString());
        TestCase.assertEquals("/testpath/a/b", visitor.files.get(1).getPath().toString());
        TestCase.assertEquals("/testpath/abc/def/xyz", visitor.files.get(3).getPath().toString());
        TestCase.assertEquals("/testpath/abc/def/zyx", visitor.files.get(4).getPath().toString());
        
        visitor = new ContainerVisitor(root.getFileSystem(), "glob:/te**$");
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root);
        TestCase.assertEquals("/testpath/a/b/c", visitor.files.get(0).getPath().toString());
        TestCase.assertEquals("/testpath/a/b", visitor.files.get(1).getPath().toString());
        TestCase.assertEquals("/testpath/abc/def/xyz", visitor.files.get(3).getPath().toString());
        TestCase.assertEquals("/testpath/abc/def", visitor.files.get(4).getPath().toString());
        
        visitor = new ContainerVisitor(root.getFileSystem(), "glob:/te**^");
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root);
        TestCase.assertEquals("/testpath/abc/def/zyx", visitor.files.get(0).getPath().toString());
        TestCase.assertEquals("/testpath/abc/fed", visitor.files.get(1).getPath().toString());
        
        visitor = new ContainerVisitor(root.getFileSystem(), "glob:/**/te*");
        walker = new DirectoryWalker(provider,visitor, 4);
        walker.walk(root);
        TestCase.assertEquals("/testpath/testfolder", visitor.files.get(0).getPath().toString());
        TestCase.assertEquals("/testpath/testgroup", visitor.files.get(1).getPath().toString());
        
        visitor = new ContainerVisitor(root.getFileSystem(), "glob:/**/te*^");
        walker = new DirectoryWalker(provider,visitor, 4);
        walker.walk(root);
        System.out.println(visitor.files.toString());
        TestCase.assertEquals("/testpath/testgroup", visitor.files.get(0).getPath().toString());
        TestCase.assertTrue(visitor.files.size() == 1);
        
        visitor = new ContainerVisitor(root.getFileSystem(), "glob:/**/te*$");
        walker = new DirectoryWalker(provider,visitor, 4);
        walker.walk(root);
        TestCase.assertEquals("/testpath/testfolder", visitor.files.get(0).getPath().toString());
        TestCase.assertTrue(visitor.files.size() == 1);
        
        Boolean searchGroups = true;
        Boolean searchFolders = true;
        visitor = new ContainerVisitor(root.getFileSystem(), "/testpath", searchGroups, searchFolders);
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root);
        TestCase.assertEquals("/testpath", visitor.files.get(0).getPath().toString());
        TestCase.assertTrue(visitor.files.size() == 1);
        
        searchGroups = true;
        searchFolders = false;
        visitor = new ContainerVisitor(root.getFileSystem(), "/testpath", searchGroups, searchFolders);
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root);
        TestCase.assertTrue(visitor.files.size() == 0);
        
        searchGroups = false;
        searchFolders = true;
        visitor = new ContainerVisitor(root.getFileSystem(), "/testpath/*", searchGroups, searchFolders);
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root);
        TestCase.assertEquals("/testpath/a", visitor.files.get(0).getPath().toString());
        TestCase.assertEquals("/testpath/abc", visitor.files.get(1).getPath().toString());
        TestCase.assertEquals("/testpath/testfolder", visitor.files.get(2).getPath().toString());
        TestCase.assertTrue(visitor.files.size() == 3);
        
        searchGroups = true;
        searchFolders = false;
        visitor = new ContainerVisitor(root.getFileSystem(), "/testpath/*", searchGroups, searchFolders);
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root);
        TestCase.assertEquals("/testpath/testgroup", visitor.files.get(0).getPath().toString());
        TestCase.assertTrue(visitor.files.size() == 1);
        
        searchGroups = null;
        searchFolders = null;
        visitor = new ContainerVisitor(root.getFileSystem(), "/testpath/test*^", searchGroups, searchFolders);
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root);
        TestCase.assertEquals("/testpath/testgroup", visitor.files.get(0).getPath().toString());
        TestCase.assertTrue(visitor.files.size() == 1);
        
        searchGroups = null;
        searchFolders = null;
        visitor = new ContainerVisitor(root.getFileSystem(), "/testpath/test*$", searchGroups, searchFolders);
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root);
        TestCase.assertEquals("/testpath/testfolder", visitor.files.get(0).getPath().toString());
        TestCase.assertTrue(visitor.files.size() == 1);

        
        searchGroups = false;
        searchFolders = true;
        
        visitor = new ContainerVisitor(root.getFileSystem(), PathUtils.normalize( "/testpath/."), searchGroups, searchFolders);
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root);
        TestCase.assertEquals("/testpath", visitor.files.get(0).getPath().toString());
        TestCase.assertTrue(visitor.files.size() == 1);
        
    }
    
}
