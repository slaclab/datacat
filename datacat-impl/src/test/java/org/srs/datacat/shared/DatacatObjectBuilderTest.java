/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.srs.datacat.shared;


import junit.framework.TestCase;
import static org.junit.Assert.*;

/**
 *
 * @author bvan
 */
public class DatacatObjectBuilderTest extends TestCase {
    
    public DatacatObjectBuilderTest(){
    }

    public void testMask(){
        DatacatObject.Builder b = new DatacatObject.Builder();
        b.jsonType( "DATASET");
        b.pk(1234L);
        b.name("hello");
        assertTrue(b.build().getPk() == 1234L);
        assertTrue("hello".equals( b.build().getName() ));
       
    }
    
}
