/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.srs.datacat.rest;

import org.srs.datacat.model.RequestView;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import org.glassfish.jersey.uri.UriComponent;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.model.RecordType;

/**
 *
 * @author bvan
 */
public class RequestViewTest extends TestCase {
    
    public RequestViewTest(){
    }
    

    public void testFolder(){
        RequestView rv;
        Map<String,List<String>> mvmap = UriComponent.decodeMatrix( "/path;children", true);
        
        rv = new RequestView(RecordType.FOLDER, mvmap);
        assertTrue(rv.containsKey("children"));
        
        // Should pass
        mvmap = UriComponent.decodeMatrix( "/path;children;stat=dataset", true);
        rv = new RequestView(RecordType.FOLDER, mvmap);
        assertTrue(rv.containsKey("stat"));
                
        mvmap = UriComponent.decodeMatrix( "/path;children;v=3", true);
        rv = new RequestView(RecordType.FOLDER, mvmap);
        assertTrue(rv.getDatasetView().getVersionId() == 3);

        mvmap = UriComponent.decodeMatrix( "/path;metadata", true);
        rv = new RequestView(RecordType.FOLDER, mvmap);
        assertTrue(rv.containsKey("metadata"));
        
        mvmap = UriComponent.decodeMatrix( "/path;metadata;versions;pk", true);
        rv = new RequestView(RecordType.FOLDER, mvmap);
        assertTrue(rv.containsKey("pk"));
        
        // Should fail
        mvmap = UriComponent.decodeMatrix( "/path;children", true);
        try {
            rv = new RequestView(RecordType.DATASET, mvmap);
        } catch (IllegalArgumentException ex){}
        
        mvmap = UriComponent.decodeMatrix( "/path;children;stat=dataset;v=2", true);
        try {
            rv = new RequestView(RecordType.FOLDER, mvmap);
        } catch (IllegalArgumentException ex){}
        
        mvmap = UriComponent.decodeMatrix( "/path;children;stat=dataset;v=2", true);
        try {
            rv = new RequestView(RecordType.FOLDER, mvmap);
        } catch (IllegalArgumentException ex){}

    }
    
}
