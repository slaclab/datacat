/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.srs.vfs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 *
 * @author Brian Van Klaveren<bvan@slac.stanford.edu>
 */
public class GlobToRegexTest {
    
    public GlobToRegexTest(){
    }

    @Test
    public void testPathGlob(){
        
        String sep = "/";
        Pattern pattern;
        Matcher matcher;
        
        String glob01 = "/a*";
        String path01_01 = "/a";
        String path01_02 = "/abc";
        String path01_03 = "/a*";
        String path01_04 = "/a/";
        String path01_05 = "/abc/";
        String path01_06 = "/abc/a";
        String path01_07 = "/abc/b";
        String path01_08 = "/abc/a/a";
        String path01_09 = "/abc/a/b";
        String path01_10 = "/abc/b/a";
        String path01_11 = "/abc/b/b";

        System.out.println(GlobToRegex.toRegex( glob01, sep));
        pattern = Pattern.compile(GlobToRegex.toRegex( glob01, sep));
        matcher = pattern.matcher(path01_01);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_02);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_03);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_04);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_05);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_06);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_07);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_08);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_09);
        System.out.println(matcher.matches());
        
        String glob02 = "/a*/*";
        System.out.println(PathUtils.normalizeRegex(glob02) );

        System.out.println(GlobToRegex.toRegex( glob02, sep));
        pattern = Pattern.compile(GlobToRegex.toRegex( glob02, sep));
        matcher = pattern.matcher(path01_01);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_02);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_03);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_04);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_05);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_06);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_07);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_08);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_09);
        System.out.println(matcher.matches());
        
        
        String glob03 = "/a*/*/*";
        System.out.println(PathUtils.normalizeRegex(glob03) );
        System.out.println(GlobToRegex.toRegex( glob03, sep));
        pattern = Pattern.compile(GlobToRegex.toRegex( glob03, sep));
        matcher = pattern.matcher(path01_01);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_02);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_03);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_04);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_05);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_06);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_07);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_08);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_09);
        System.out.println(matcher.matches());
        
        String glob04 = "/a*/**/a";
        System.out.println(PathUtils.normalizeRegex(glob04) );  
        System.out.println(GlobToRegex.toRegex( glob04, sep));
        pattern = Pattern.compile(GlobToRegex.toRegex( glob04, sep));
        matcher = pattern.matcher(path01_01);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_02);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_03);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_04);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_05);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_06);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_07);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_08);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_09);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_10);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_11);
        System.out.println(matcher.matches());
        
        String glob05 = "/a*/**a";
        System.out.println(PathUtils.normalizeRegex(glob05));

        System.out.println(GlobToRegex.toRegex( glob05, sep));
        pattern = Pattern.compile(GlobToRegex.toRegex( glob05, sep));
        matcher = pattern.matcher(path01_01);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_02);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_03);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_04);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_05);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_06);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_07);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_08);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_09);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_10);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_11);
        System.out.println(matcher.matches());
        
        
        String glob06 = "/abc/**a";
        System.out.println(PathUtils.normalizeRegex(glob06));

        System.out.println(GlobToRegex.toRegex( glob06, sep));
        pattern = Pattern.compile(GlobToRegex.toRegex( glob06, sep));
        matcher = pattern.matcher(path01_01);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_02);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_03);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_04);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_05);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_06);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_07);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_08);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_09);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_10);
        System.out.println(matcher.matches());
        matcher = pattern.matcher(path01_11);
        System.out.println(matcher.matches());
        
    }    
    

    
}
