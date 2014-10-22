
package org.srs.vfs;

import org.srs.vfs.AbstractFs;
import org.srs.vfs.PathProvider;
import org.srs.vfs.AbstractPath;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Iterator;
import junit.framework.TestCase;

/**
 *
 * @author bvan
 */
public class AbstractPathTest extends TestCase {
    
    public AbstractPathTest(String testName){
        super( testName );
    }
    
    @Override
    protected void setUp() throws Exception{
    }
    
    class MockAbstractFs extends AbstractFs<AbstractPath> {

        class MockPathProvider extends PathProvider {

            @Override
            public AbstractPath getRoot(){
                return new AbstractPath( null, MockAbstractFs.this, "/" ){};
            }

            @Override
            public AbstractPath getPath(URI uri){
                return new AbstractPath( uri.getUserInfo(), MockAbstractFs.this, uri.getPath()){};
            }

            @Override
            public AbstractPath getPath(String userName, String path){
                return new AbstractPath( null, MockAbstractFs.this, path ){};
            }
        };

        public MockAbstractFs(){
            super( null );
        }

        @Override
        public Class getPathClass(){
            return AbstractPath.class;
        }

        @Override
        public PathProvider getPathProvider(){
            return new MockPathProvider();
        }

        @Override
        public UserPrincipalLookupService getUserPrincipalLookupService(){
            throw new UnsupportedOperationException( "Not supported yet." ); 
        }

    };


    public void testCreate() throws IOException{

        AbstractFs fs = new MockAbstractFs();
        AbstractPath path = new AbstractPath(null,fs,"/") {};
        Path sysPath = Paths.get("/");
        
        assertTrue(path.equals(sysPath));

        path = path.resolve("hi");
        sysPath = sysPath.resolve("hi");
        assertTrue(path.equals(sysPath));
        assertTrue(path.getFileName().equals( sysPath.getFileName()));
        assertEquals(path.getNameCount(), sysPath.getNameCount());
        System.out.println(path);
        System.out.println(sysPath);
        
        path = path.resolve("howareyou");
        sysPath = sysPath.resolve("howareyou");
        assertTrue(path.equals(sysPath));
        assertTrue(path.getFileName().equals( sysPath.getFileName()));
        assertEquals(path.getNameCount(), sysPath.getNameCount());
        System.out.println(path);
        System.out.println(sysPath);
        
        path = path.getParent();
        sysPath = sysPath.getParent();
        assertTrue(path.equals(sysPath));
        assertTrue(path.getFileName().equals( sysPath.getFileName()));
        assertEquals(path.getNameCount(), sysPath.getNameCount());

        assertEquals(path.resolve( "hello/."), sysPath.resolve("hello/."));
        assertEquals(path.resolve( "hello/.."), sysPath.resolve("hello/.."));
        assertEquals(path.resolve( "hello/./hi"), sysPath.resolve("hello/./hi"));
        assertEquals(path.resolve( "hello/../hi"), sysPath.resolve("hello/../hi"));
        assertEquals(path.resolve( "../hello"), sysPath.resolve( "../hello"));
        assertEquals(path.resolve( "./hello"), sysPath.resolve( "./hello"));
        
        System.out.println(path);
        System.out.println(sysPath);
        
        path = path.resolve("howareyou");
        sysPath = sysPath.resolve("howareyou");
        int i = 0;
        for(Iterator<Path> iter = path.iterator(); iter.hasNext(); i++){
            assertTrue(iter.next().equals( sysPath.getName( i)));
        }
        
        System.out.println(path.toUri().toString());
        
        System.out.println(path.toUri().getPath().toString());
        path = path.getRoot();
        sysPath = sysPath.getRoot();
        assertTrue(path.equals(sysPath));
        
        
    }
    
}
