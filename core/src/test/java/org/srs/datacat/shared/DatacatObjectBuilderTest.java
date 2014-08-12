/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.srs.datacat.shared;


import org.srs.datacat.shared.DatacatObjectBuilder;
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
        DatacatObjectBuilder b = new DatacatObjectBuilder();
        b.type( "DATASET");
        b.pk(1234L);
        b.name("hello");
        assertTrue(b.build().getPk() == 1234L);
        assertTrue(b.build().getName() == "hello");
       
    }
    
}
