/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacat.vfs.security;

import org.srs.datacat.security.DcPermissions;
import org.srs.datacat.security.DcUser;
import org.srs.datacat.security.DcGroup;
import org.srs.datacat.security.OwnerAclAttributes;
import com.google.common.base.Optional;
import java.io.IOException;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.UserPrincipal;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.srs.datacat.vfs.TestUtils;
import org.srs.vfs.AbstractFsProvider;

/**
 *
 * @author Brian Van Klaveren<bvan@slac.stanford.edu>
 */
public class DcPermissionsTest {
    
    public DcPermissionsTest(){
    }
    
    @Before
    public void setUp(){
    }

    @Test
    public void testPack(){
    }

    @Test
    public void testUnpack(){
        TestCase.assertEquals( DcPermissions.INSERT, DcPermissions.unpack('i'));
        TestCase.assertEquals( DcPermissions.READ, DcPermissions.unpack('r'));      
        TestCase.assertEquals( DcPermissions.DELETE, DcPermissions.unpack('d'));
        TestCase.assertEquals( DcPermissions.WRITE, DcPermissions.unpack('w'));
        TestCase.assertEquals( DcPermissions.ADMIN, DcPermissions.unpack('a'));
    }

    @Test
    public void testGetOwnerAclAttributes() throws IOException{
        Optional<OwnerAclAttributes> attrOpt = DcPermissions.getOwnerAclAttributes("john@fermi:o:,$PUBLIC$@:g:riw");
        TestCase.assertTrue(attrOpt.isPresent());
        DcUser u  = attrOpt.get().getOwner();
        TestCase.assertEquals("john", u.getName());
        OwnerAclAttributes attrs = attrOpt.get();
        TestCase.assertTrue(attrs.getAcl().get(0).permissions().contains(DcPermissions.READ));
        TestCase.assertTrue(attrs.getAcl().get(0).permissions().contains(DcPermissions.INSERT));
        TestCase.assertTrue(attrs.getAcl().get(0).permissions().contains(DcPermissions.WRITE));
        TestCase.assertFalse(attrs.getAcl().get(0).permissions().contains(DcPermissions.DELETE));
        TestCase.assertFalse(attrs.getAcl().get(0).permissions().contains(DcPermissions.ADMIN));
        //checkPermission(attrs, DcGroup.PUBLIC_GROUP, DcPermissions.READ);
    }
    
    public static void checkPermission(OwnerAclAttributes attrs, DcUser user, AclEntryPermission permission) throws IOException {
        
        for(AclEntry entry: attrs.getAcl()){
            UserPrincipal principal = entry.principal();
            if(entry.type() == AclEntryType.ALARM || entry.type() == AclEntryType.AUDIT){
                AbstractFsProvider.AfsException.ACCESS_DENIED.throwError("", "Unsupported Access Control Entry found: " + entry.type());
            }
            boolean allow = entry.type() == AclEntryType.ALLOW;
            if(principal instanceof DcGroup){
                boolean isMember = TestUtils.getLookupService().lookupGroupsForUser(user).contains( entry.principal());
                boolean hasPermission = entry.permissions().contains(permission);
                if(isMember && hasPermission){
                    if(allow){
                        return;
                    } else {
                        AbstractFsProvider.AfsException.ACCESS_DENIED.throwError("", "User disallowed access to path");
                    }
                }
            }
        }
        AbstractFsProvider.AfsException.ACCESS_DENIED.throwError("", "No Access Control Entries Found");
    }
    
}
