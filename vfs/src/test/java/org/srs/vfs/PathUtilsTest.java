/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
        
        String path = "/../";
        
        
        System.out.println("normalize");
        System.out.println(PathUtils.normalize("/EXO/path"));
        System.out.println(PathUtils.normalize("//EXO/path"));
        System.out.println(PathUtils.normalize("//EXO//path"));
       
        System.out.println("and check");
        System.out.println(PathUtils.normalizeSeparators("/EXO/path"));
        System.out.println(PathUtils.normalizeSeparators("//EXO/path"));
        System.out.println(PathUtils.normalizeSeparators("//EXO//path"));
        
        System.out.println("normalize");
        System.out.println(PathUtils.normalize("/EXO/path/"));
        System.out.println(PathUtils.normalize("//EXO/path/"));
        System.out.println(PathUtils.normalize("//EXO//path/"));
        
        System.out.println("and check");
        System.out.println(PathUtils.normalizeSeparators("/EXO/path/"));
        System.out.println(PathUtils.normalizeSeparators("//EXO/path/"));
        System.out.println(PathUtils.normalizeSeparators("//EXO//path/"));
        
        System.out.println("normalize");
        System.out.println(PathUtils.normalize("/EXO/path/."));
        System.out.println(PathUtils.normalize("//EXO/path/."));
        System.out.println(PathUtils.normalize("//EXO//path/."));
        
        System.out.println("and check");
        System.out.println(PathUtils.normalizeSeparators("/EXO/path/."));
        System.out.println(PathUtils.normalizeSeparators("//EXO/path/."));
        System.out.println(PathUtils.normalizeSeparators("//EXO//path/."));
        
        System.out.println("normalize");
        System.out.println(PathUtils.normalize("/EXO/path/./new"));
        System.out.println(PathUtils.normalize("//EXO/path/./new"));
        System.out.println(PathUtils.normalize("//EXO//path/./new"));
        
        System.out.println("and check");
        System.out.println(PathUtils.normalizeSeparators("/EXO/path/./new"));
        System.out.println(PathUtils.normalizeSeparators("//EXO/path/./new"));
        System.out.println(PathUtils.normalizeSeparators("//EXO//path/./new"));

        System.out.println("normalize");
        System.out.println(PathUtils.normalize("/EXO/path/.."));
        System.out.println(PathUtils.normalize("//EXO/path/.."));
        System.out.println(PathUtils.normalize("//EXO//path/.."));
        
        System.out.println("and check");
        System.out.println(PathUtils.normalizeSeparators("/EXO/path/.."));
        System.out.println(PathUtils.normalizeSeparators("//EXO/path/.."));
        System.out.println(PathUtils.normalizeSeparators("//EXO//path/.."));
        
        System.out.println("done");
        System.out.println(PathUtils.normalize(PathUtils.normalizeSeparators("/./EXO/./path")));
        
        System.out.println(PathUtils.normalizeSeparators("/EXO//path/something"));
        
        System.out.println(PathUtils.normalize("/./CTA"));
        System.out.println(PathUtils.normalize("/EXO/../CTA"));
        System.out.println(PathUtils.normalizeSeparators(PathUtils.normalize("/./CTA")));
        System.out.println(PathUtils.normalizeSeparators(PathUtils.normalize("/EXO/../CTA")));
        
        System.out.println("checking absolute subpath");
        String fullPath = PathUtils.normalize("/path/to/somewhere/fake.txt/");
        int[] offsets = PathUtils.offsets(fullPath);
        for(int i = 0; i <= offsets.length; i++){
            System.out.println(PathUtils.absoluteSubpath( fullPath, i, offsets ));
        }
    }
    
    @Test
    public void testNormalizeAndCheck(){
        System.out.println("normalized and check test");
        System.out.println(PathUtils.normalizeSeparators("/EXO/path/"));
        System.out.println(PathUtils.normalizeSeparators("//EXO/path/"));
        System.out.println(PathUtils.normalizeSeparators("//EXO//path//hi"));
        
    }
}
