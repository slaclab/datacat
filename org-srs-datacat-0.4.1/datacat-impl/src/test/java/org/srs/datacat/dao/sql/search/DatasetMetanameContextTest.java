/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacat.dao.sql.search;

import org.srs.datacat.dao.sql.search.MetanameContext;
import java.util.TreeSet;
import junit.framework.TestCase;
import org.srs.datacat.dao.sql.search.MetanameContext.Entry;

/**
 *
 * @author bvan
 */
public class DatasetMetanameContextTest extends TestCase {
    
    String[] noPrefix = {"prank", "prince"};
    String[] prefix = {"pre", "prefix", "pregame"};
    
    String[][] queryStringsArr = {
        {"pra", null, "prank"},
        {"prank", "prank", "prank" },
        {"prankdial", "prank", "pre"},
        {"prE", "prank", "pre"},
        {"pre", "pre", "pre"},
        {"pred", "pre", "prefix"},
        {"preFix", "pre", "prefix"},
        {"prefix", "prefix", "prefix"},
        {"pregame", "pregame", "pregame"},
        {"pregamer", "pregame", "prince"},
        {"pregnant", "pregame", "prince"},
        {"prime", "pregame", "prince"},
        {"print", "prince", null}
    };

    
    MetanameContext getContext(){
        MetanameContext context = new MetanameContext();
        context.add( new Entry(noPrefix[0]));
        context.add( new Entry(noPrefix[1]));
        return context;
    }
    
    MetanameContext getContextFull(){
        MetanameContext context = getContext();
        TreeSet<Entry> postfixSet = new TreeSet<>();
        Entry _pre = new Entry( prefix[0].substring( 3 ) );
        Entry _prefix = new Entry( prefix[1].substring( 3 ) );
        Entry _pregame = new Entry( prefix[2].substring( 3 ) );
        postfixSet.add( _pre );
        postfixSet.add( _prefix );
        postfixSet.add( _pregame );
        Entry complex = new Entry( prefix[0], postfixSet );
        context.add( complex );
        return context;
    }    
    
    public DatasetMetanameContextTest(String testName){
        super( testName );
    }

    /**
     * Test of floor method, of class MetanameContext.
     */
    public void testFloor_DatasetMetanameContextEntry(){
        System.out.println( "floor" );
        MetanameContext context = getContextFull();
        for(String[] queryStrings: queryStringsArr ){
            String metaname = queryStrings[0];
            String expected = queryStrings[1];
            System.err.println("Checking " + metaname + ", expecting " + expected);
            Entry floor = context.floor( metaname );
            if(expected == null && floor != null){
                assertEquals("Searching for floor of " + metaname, expected, floor.metaname);
            } else if( floor != null && !floor.metaname.equals( expected ) ){
                assertEquals("Searching for floor of " + metaname, expected, floor.metaname);
            }
        }
    }
    
    /**
     * Test of floor method, of class MetanameContext.
     */
    public void testContains_DatasetMetanameContextEntry(){
        System.out.println( "contains" );
        MetanameContext context = getContextFull();
        for(String[] queryStrings: queryStringsArr ){
            String metaname = queryStrings[0];
            boolean shouldContain = queryStrings[0].equals( queryStrings[1] );
            System.err.println("Checking contains(" + metaname + ") is " + shouldContain);
            assertTrue("Incorrect state",  context.contains( queryStrings[0] ) == shouldContain);
        }
    }
    
}
