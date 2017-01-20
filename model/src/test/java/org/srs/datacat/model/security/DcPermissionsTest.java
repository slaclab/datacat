
package org.srs.datacat.model.security;

import com.google.common.base.Optional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junit.framework.TestCase;
import org.junit.Test;

/**
 *
 * @author bvan
 */
public class DcPermissionsTest {
    
    @Test
    public void testUnpackPack(){
        TestCase.assertEquals("ridwa", DcPermissions.pack(DcPermissions.unpackString("adirw")));
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
    public void testGetAclAttributes() throws IOException{
        Optional<List<DcAclEntry>> attrOpt = AclTransformation.parseAcl("john@fermi:g:riw,$PUBLIC$@:g:r");
        TestCase.assertTrue(attrOpt.isPresent());
        List<DcAclEntry> acl = attrOpt.get();
        TestCase.assertTrue(acl.get(0).getPermissions().contains(DcPermissions.READ));
        TestCase.assertTrue(acl.get(0).getPermissions().contains(DcPermissions.INSERT));
        TestCase.assertTrue(acl.get(0).getPermissions().contains(DcPermissions.WRITE));
        TestCase.assertFalse(acl.get(0).getPermissions().contains(DcPermissions.DELETE));
        TestCase.assertFalse(acl.get(0).getPermissions().contains(DcPermissions.ADMIN));
        
        TestCase.assertTrue(acl.get(1).getPermissions().contains(DcPermissions.READ));
        TestCase.assertFalse(acl.get(1).getPermissions().contains(DcPermissions.INSERT));
        TestCase.assertFalse(acl.get(1).getPermissions().contains(DcPermissions.WRITE));
        TestCase.assertFalse(acl.get(1).getPermissions().contains(DcPermissions.DELETE));
        TestCase.assertFalse(acl.get(1).getPermissions().contains(DcPermissions.ADMIN));
    }
    
    @Test
    public void testMergeAclEntries() throws Exception{

        DcAclEntry keepFirstEntry = DcAclEntry.newBuilder()
                .subject(new DcGroup("admin@fermi"))
                .permissions("rw")
                .scope(DcAclEntryScope.DEFAULT)
                .build();
        
        DcAclEntry keepAfterEntry1 = DcAclEntry.newBuilder()
                .subject(new DcGroup("user@exo"))
                .permissions("rw")
                .scope(DcAclEntryScope.DEFAULT)
                .build();
        
        DcAclEntry keepAfterEntry2 = DcAclEntry.newBuilder()
                .subject(new DcGroup("user@fermi"))
                .permissions("rw")
                .scope(DcAclEntryScope.DEFAULT)
                .build();
        
        DcAclEntry oldEntry = DcAclEntry.newBuilder()
                .subject(new DcGroup("developer@fermi"))
                .permissions("r")
                .scope(DcAclEntryScope.DEFAULT)
                .build();
        
        DcAclEntry newEntry = DcAclEntry.newBuilder()
                .subject(new DcGroup("developer@fermi"))
                .permissions("ridwa")
                .scope(DcAclEntryScope.ACCESS)
                .build();
        
        List<DcAclEntry> existing = new ArrayList<>();
        List<DcAclEntry> updated = new ArrayList<>();
        existing.add(keepFirstEntry);
        existing.add(oldEntry);
        existing.add(keepAfterEntry1);
        existing.add(keepAfterEntry2);
        updated.add(newEntry);
        updated = AclTransformation.mergeAclEntries(existing, updated);
        TestCase.assertEquals(updated.get(0).getSubject(), keepFirstEntry.getSubject());
        TestCase.assertEquals(updated.get(1).getSubject(), newEntry.getSubject());
        TestCase.assertEquals(updated.get(2).getPermissions(), keepAfterEntry1.getPermissions());
        TestCase.assertEquals(updated.get(3).getSubject(), keepAfterEntry2.getSubject());
        TestCase.assertEquals(updated.size(), 4);
        
    }
    
    @Test
    public void testRemoveEmptyPermissionEntries() throws Exception{
        
        DcAclEntry keepFirstEntry = DcAclEntry.newBuilder()
                .subject(new DcGroup("admin@fermi"))
                .permissions("rw")
                .scope(DcAclEntryScope.DEFAULT)
                .build();
        
        DcAclEntry oldEntry = DcAclEntry.newBuilder()
                .subject(new DcGroup("developer@fermi"))
                .permissions("r")
                .scope(DcAclEntryScope.DEFAULT)
                .build();
        
        DcAclEntry keepAfterEntry1 = DcAclEntry.newBuilder()
                .subject(new DcGroup("user@exo"))
                .permissions("rw")
                .scope(DcAclEntryScope.DEFAULT)
                .build();
        
        DcAclEntry keepAfterEntry2 = DcAclEntry.newBuilder()
                .subject(new DcGroup("user@fermi"))
                .permissions("rw")
                .scope(DcAclEntryScope.DEFAULT)
                .build();
        
        
        DcAclEntry newEntry = DcAclEntry.newBuilder()
                .subject(new DcGroup("developer@fermi"))
                .permissions(Collections.EMPTY_SET)
                .scope(DcAclEntryScope.ACCESS)
                .build();
                
        List<DcAclEntry> existing = new ArrayList<>();
        List<DcAclEntry> updated = new ArrayList<>();
        existing.add(keepAfterEntry1);
        existing.add(oldEntry);
        existing.add(keepAfterEntry2);
        existing.add(keepFirstEntry);
        updated.add(newEntry);
        
        updated = AclTransformation.mergeAclEntries(existing, updated);
        TestCase.assertEquals(updated.get(0).getSubject(), keepFirstEntry.getSubject());
        TestCase.assertEquals(updated.get(1).getSubject(), keepAfterEntry1.getSubject());
        TestCase.assertEquals(updated.get(2).getSubject(), keepAfterEntry2.getSubject());
        TestCase.assertEquals(updated.size(), 3);
    }
}
