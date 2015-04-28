
package org.srs.datacat.vfs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import junit.framework.TestCase;
import org.apache.commons.dbcp2.DelegatingConnection;
import org.junit.Before;
import org.junit.Test;
import org.junit.BeforeClass;
import org.srs.datacat.dao.DAOFactory;
import org.srs.datacat.dao.sql.mysql.DAOFactoryMySQL;
import org.srs.datacat.model.ModelProvider;
import org.srs.datacat.model.security.CallContext;
import org.srs.datacat.shared.Provider;
import org.srs.datacat.test.DbHarness;
import org.srs.datacat.vfs.DirectoryWalker.ContainerVisitor;
import org.srs.vfs.PathUtils;

/**
 *
 * @author bvan
 */
public class DirectoryWalkerTest {
    static DbHarness harness;
    DcFileSystemProvider provider;
    Path root;
    CallContext callContext = TestUtils.DEFAULT_TEST_CONTEXT;
    
    public DirectoryWalkerTest() throws IOException{
        DAOFactory factory = new DAOFactoryMySQL(harness.getDataSource());
        ModelProvider modelProvider = new Provider();
        this.provider  = new DcFileSystemProvider(factory, modelProvider);
    }
    
    @BeforeClass
    public static void setUpDb() throws SQLException, IOException{
        harness = DbHarness.getDbHarness();
        DataSource d = harness.getDataSource();
        Connection c = ((DelegatingConnection)d.getConnection()).getInnermostDelegate();
        
    }
    
    @Before
    public void setUp() throws IOException{
        URI uri = DcUriUtils.toFsUri( "/", "SRS");
        root = provider.getPath( uri );
    }

    @Test
    public void testWalk() throws IOException{
        
        ContainerVisitor visitor;
        visitor = new ContainerVisitor(provider.getFileSystem(), "glob:/te*");
        DirectoryWalker walker = new DirectoryWalker(provider,visitor,2);
        walker.walk(root, callContext);
        
        visitor = new ContainerVisitor(provider.getFileSystem(), "glob:/te**");
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root, callContext);
        TestCase.assertEquals("/testpath/a/b/c", visitor.files.get(0).getPath().toString());
        TestCase.assertEquals("/testpath/a/b", visitor.files.get(1).getPath().toString());
        TestCase.assertEquals("/testpath/abc/def/xyz", visitor.files.get(3).getPath().toString());
        TestCase.assertEquals("/testpath/abc/def/zyx", visitor.files.get(4).getPath().toString());
        
        visitor = new ContainerVisitor(provider.getFileSystem(), "glob:/te**$");
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root, callContext);
        TestCase.assertEquals("/testpath/a/b/c", visitor.files.get(0).getPath().toString());
        TestCase.assertEquals("/testpath/a/b", visitor.files.get(1).getPath().toString());
        TestCase.assertEquals("/testpath/abc/def/xyz", visitor.files.get(3).getPath().toString());
        TestCase.assertEquals("/testpath/abc/def", visitor.files.get(4).getPath().toString());
        
        visitor = new ContainerVisitor(provider.getFileSystem(), "glob:/te**^");
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root, callContext);
        TestCase.assertEquals("/testpath/abc/def/zyx", visitor.files.get(0).getPath().toString());
        TestCase.assertEquals("/testpath/abc/fed", visitor.files.get(1).getPath().toString());
        
        visitor = new ContainerVisitor(provider.getFileSystem(), "glob:/**/te*");
        walker = new DirectoryWalker(provider,visitor, 4);
        walker.walk(root, callContext);
        TestCase.assertEquals("/testpath/testfolder", visitor.files.get(0).getPath().toString());
        TestCase.assertEquals("/testpath/testgroup", visitor.files.get(1).getPath().toString());
        
        visitor = new ContainerVisitor(provider.getFileSystem(), "glob:/**/te*^");
        walker = new DirectoryWalker(provider,visitor, 4);
        walker.walk(root, callContext);
        System.out.println(visitor.files.toString());
        TestCase.assertEquals("/testpath/testgroup", visitor.files.get(0).getPath().toString());
        TestCase.assertTrue(visitor.files.size() == 1);
        
        visitor = new ContainerVisitor(provider.getFileSystem(), "glob:/**/te*$");
        walker = new DirectoryWalker(provider,visitor, 4);
        walker.walk(root, callContext);
        TestCase.assertEquals("/testpath/testfolder", visitor.files.get(0).getPath().toString());
        TestCase.assertTrue(visitor.files.size() == 1);
        
        Boolean searchGroups = true;
        Boolean searchFolders = true;
        visitor = new ContainerVisitor(provider.getFileSystem(), "/testpath", searchGroups, searchFolders);
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root, callContext);
        TestCase.assertEquals("/testpath", visitor.files.get(0).getPath().toString());
        TestCase.assertTrue(visitor.files.size() == 1);
        
        searchGroups = true;
        searchFolders = false;
        visitor = new ContainerVisitor(provider.getFileSystem(), "/testpath", searchGroups, searchFolders);
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root, callContext);
        TestCase.assertTrue(visitor.files.size() == 0);
        
        searchGroups = false;
        searchFolders = true;
        visitor = new ContainerVisitor(provider.getFileSystem(), "/testpath/*", searchGroups, searchFolders);
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root, callContext);
        TestCase.assertEquals("/testpath/a", visitor.files.get(0).getPath().toString());
        TestCase.assertEquals("/testpath/abc", visitor.files.get(1).getPath().toString());
        TestCase.assertEquals("/testpath/testfolder", visitor.files.get(2).getPath().toString());
        TestCase.assertTrue(visitor.files.size() == 3);
        
        searchGroups = true;
        searchFolders = false;
        visitor = new ContainerVisitor(provider.getFileSystem(), "/testpath/*", searchGroups, searchFolders);
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root, callContext);
        TestCase.assertEquals("/testpath/testgroup", visitor.files.get(0).getPath().toString());
        TestCase.assertTrue(visitor.files.size() == 1);
        
        searchGroups = null;
        searchFolders = null;
        visitor = new ContainerVisitor(provider.getFileSystem(), "/testpath/test*^", searchGroups, searchFolders);
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root, callContext);
        TestCase.assertEquals("/testpath/testgroup", visitor.files.get(0).getPath().toString());
        TestCase.assertTrue(visitor.files.size() == 1);
        
        searchGroups = null;
        searchFolders = null;
        visitor = new ContainerVisitor(provider.getFileSystem(), "/testpath/test*$", searchGroups, searchFolders);
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root, callContext);
        TestCase.assertEquals("/testpath/testfolder", visitor.files.get(0).getPath().toString());
        TestCase.assertTrue(visitor.files.size() == 1);

        
        searchGroups = false;
        searchFolders = true;
        
        visitor = new ContainerVisitor(provider.getFileSystem(), PathUtils.normalize( "/testpath/."), searchGroups, searchFolders);
        walker = new DirectoryWalker(provider,visitor,5);
        walker.walk(root, callContext);
        TestCase.assertEquals("/testpath", visitor.files.get(0).getPath().toString());
        TestCase.assertTrue(visitor.files.size() == 1);
        
    }
    
}
