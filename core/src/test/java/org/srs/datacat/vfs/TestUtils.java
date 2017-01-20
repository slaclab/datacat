
package org.srs.datacat.vfs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.srs.datacat.dao.DAOFactory;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.ModelProvider;
import org.srs.datacat.model.security.CallContext;
import org.srs.datacat.model.security.DcGroup;
import org.srs.datacat.model.security.DcUser;
import org.srs.datacat.security.DcUserLookupService;

import org.srs.datacat.test.DbHarness;
import org.srs.datacat.vfs.DirectoryWalker.ContainerVisitor;

/**
 *
 * @author bvan
 */
public class TestUtils {
    
    protected static final CallContext DEFAULT_TEST_CONTEXT = 
            new CallContext(
                    new DcUser(DbHarness.TEST_USER), 
                    new HashSet<>(Arrays.asList(DcGroup.PUBLIC_GROUP, new DcGroup("test_group@SRS")))
            );
        
    public static final class TestUserLookupService implements DcUserLookupService {

        @Override
        public DcUser lookupPrincipalByName(String name) throws IOException{
            return name != null ? new DcUser(name) : null;
        }

        @Override
        public Set<DcGroup> lookupGroupsForUser(DcUser member) throws IOException{
            Set<DcGroup> ug = new HashSet<>();
            if(member != null && member.getName().equals("test_user")){
                ug.add(new DcGroup("test_group@SRS"));
            }
            return ug;
        }
    }

    public static DcUserLookupService getLookupService(){
        try {
            return TestUserLookupService.class.newInstance();
        } catch(InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException("Unable to instantiate lookupservice", ex);
        }
    }
    
    public static LinkedList<DatacatNode> walkPath(DAOFactory factory, ModelProvider modelProvider,
            Path searchPath, String pathPattern, Boolean searchGroups, Boolean searchFolders) throws IOException{
        DcFileSystemProvider provider = new DcFileSystemProvider(factory, modelProvider);
        ContainerVisitor visitor = new ContainerVisitor(pathPattern, searchGroups, searchFolders);
        DirectoryWalker walker = new DirectoryWalker(provider, visitor, 100);
        walker.walk(searchPath, TestUtils.DEFAULT_TEST_CONTEXT);
        return visitor.files;
    }    
}
