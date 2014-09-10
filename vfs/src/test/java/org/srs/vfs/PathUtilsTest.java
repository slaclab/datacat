
package org.srs.vfs;

import junit.framework.TestCase;
import org.junit.Test;

/**
 *
 * @author bvan
 */
public class PathUtilsTest extends TestCase {
    
    public PathUtilsTest(){
    }

    public void testDotDot(){
                
        TestCase.assertEquals("", PathUtils.normalize(""));
        TestCase.assertEquals("a", PathUtils.normalize("a"));
        TestCase.assertEquals("/", PathUtils.normalize("/"));
        
        String expected001 = "/abc/def";
        TestCase.assertEquals(expected001, PathUtils.normalize("/abc/def"));
        TestCase.assertEquals(expected001, PathUtils.normalize("//abc/def"));
        TestCase.assertEquals(expected001, PathUtils.normalize("//abc//def"));
        TestCase.assertEquals(expected001, PathUtils.normalize("/abc/def/"));
        TestCase.assertEquals(expected001, PathUtils.normalize("//abc/def/"));
        TestCase.assertEquals(expected001, PathUtils.normalize("//abc//def/"));
        TestCase.assertEquals(expected001, PathUtils.normalize("/abc/def/."));
        TestCase.assertEquals(expected001, PathUtils.normalize("//abc/def/."));
        TestCase.assertEquals(expected001, PathUtils.normalize("//abc//def/."));
       
        TestCase.assertEquals(expected001, PathUtils.normalizeSeparators("/abc/def"));
        TestCase.assertEquals(expected001, PathUtils.normalizeSeparators("//abc/def"));
        TestCase.assertEquals(expected001, PathUtils.normalizeSeparators("//abc//def"));
        TestCase.assertEquals(expected001, PathUtils.normalizeSeparators("/abc/def/"));
        TestCase.assertEquals(expected001, PathUtils.normalizeSeparators("//abc/def/"));
        TestCase.assertEquals(expected001, PathUtils.normalizeSeparators("//abc//def/"));
        
        String expected002 = "/abc/def/.";
        TestCase.assertEquals(expected002, PathUtils.normalizeSeparators("/abc/def/."));
        TestCase.assertEquals(expected002, PathUtils.normalizeSeparators("//abc/def/."));
        TestCase.assertEquals(expected002, PathUtils.normalizeSeparators("//abc//def/."));
        
        String expected003 = "/abc/def/xyz";
        TestCase.assertEquals(expected003, PathUtils.normalize("/abc/def/./xyz"));
        TestCase.assertEquals(expected003, PathUtils.normalize("//abc/def/./xyz"));
        TestCase.assertEquals(expected003, PathUtils.normalize("//abc//def/./xyz"));
        
        String expected004 = "/abc/def/./xyz";
        TestCase.assertEquals(expected004, PathUtils.normalizeSeparators("/abc/def/./xyz"));
        TestCase.assertEquals(expected004, PathUtils.normalizeSeparators("//abc/def/./xyz"));
        TestCase.assertEquals(expected004, PathUtils.normalizeSeparators("//abc//def/./xyz"));

        String expected005 = "/abc";
        TestCase.assertEquals(expected005, PathUtils.normalize("/abc/def/.."));
        TestCase.assertEquals(expected005, PathUtils.normalize("//abc/def/.."));
        TestCase.assertEquals(expected005, PathUtils.normalize("//abc//def/.."));
        
        String expected006 = "/abc/def/..";
        TestCase.assertEquals(expected006, PathUtils.normalizeSeparators("/abc/def/.."));
        TestCase.assertEquals(expected006, PathUtils.normalizeSeparators("//abc/def/.."));
        TestCase.assertEquals(expected006, PathUtils.normalizeSeparators("//abc//def/.."));
        
        String expected007 = "/xyz";
        TestCase.assertEquals(expected007, PathUtils.normalize("/./xyz"));
        TestCase.assertEquals(expected007, PathUtils.normalize("/abc/../xyz"));
        TestCase.assertEquals(expected007, PathUtils.normalizeSeparators(PathUtils.normalize("/./xyz")));
        TestCase.assertEquals(expected007, PathUtils.normalizeSeparators(PathUtils.normalize("/abc/../xyz")));
        
        System.out.println("done");
        System.out.println(PathUtils.normalize(PathUtils.normalizeSeparators("/./abc/./def")));
        System.out.println(PathUtils.normalizeSeparators("/abc//def/something"));
        
        System.out.println("checking absolute subpath");
        String fullPath = PathUtils.normalize("/def/to/somewhere/fake.txt/");
        int[] offsets = PathUtils.offsets(fullPath);
        for(int i = 0; i <= offsets.length; i++){
            System.out.println(PathUtils.absoluteSubpath( fullPath, i, offsets ));
        }
    }
    
    @Test
    public void testNormalizeAndCheck(){
        System.out.println("normalized and check test");
        System.out.println(PathUtils.normalizeSeparators("/abc/def/"));
        System.out.println(PathUtils.normalizeSeparators("//abc/def/"));
        System.out.println(PathUtils.normalizeSeparators("//abc//def//xyz"));
        
    }
    
    @Test
    public void testPathPattern(){
        TestCase.assertEquals("/abc/def001", PathUtils.normalize(PathUtils.normalizeRegex("/abc/def001")));
        TestCase.assertEquals("/abc/def001", PathUtils.normalize(PathUtils.normalizeRegex("/abc/def001/")));
        TestCase.assertEquals("/", PathUtils.normalize(PathUtils.normalizeRegex("/a*")));
        TestCase.assertEquals("/abc", PathUtils.normalize(PathUtils.normalizeRegex("/abc/*")));
        TestCase.assertEquals("/abc", PathUtils.normalize(PathUtils.normalizeRegex("/abc/[")));
        
        TestCase.assertEquals("", PathUtils.normalize(PathUtils.normalizeRegex("a*")));
        TestCase.assertEquals("abc", PathUtils.normalize(PathUtils.normalizeRegex("abc/*")));
        TestCase.assertEquals("abc", PathUtils.normalize(PathUtils.normalizeRegex("abc/[")));
    }
    
}
