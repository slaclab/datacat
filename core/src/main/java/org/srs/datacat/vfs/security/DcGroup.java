
package org.srs.datacat.vfs.security;

import java.nio.file.attribute.GroupPrincipal;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author bvan
 */
public class DcGroup implements Group, GroupPrincipal {
    
    public static final String PUBLIC_NAME = "$PUBLIC$";
    public static final String PROTECTED_NAME = "$PROTECTED$";
    
    public static final DcGroup PUBLIC_GROUP = new DcGroup(PUBLIC_NAME,null){
        @Override
        public boolean isMember(Principal member){
            // Everyone is in PUBLIC
            return true;
        }
    };
    
    public static final DcGroup PROTECTED_GROUP = new DcGroup(PROTECTED_NAME,null){
        @Override
        public boolean isMember(Principal member){
            // PUBLIC can't be in protected, everyone else can.
            if(member != null && !PUBLIC_NAME.equals(member.getName())){
                return true;
            }
            return false;
        }
    };
   
    public static class DcGroupPrincipal implements GroupPrincipal {
        private String name;
        
        public DcGroupPrincipal(String name){
            this.name = name;
        }
        
        @Override
        public String getName(){
            return name;
        }

        @Override
        public int hashCode(){
            return name.hashCode();
        }
    }
    
    private String name;
    private String experiment;
    private Set<DcGroupPrincipal> principals = new HashSet<>();
    
    
    public DcGroup(String name, String experiment){
        this.experiment = experiment;
        this.name = name;
        principals.add(new DcGroupPrincipal(name));
    }
    
    @Override
    public String getName(){
        return name;
    }

    @Override
    public boolean isMember(Principal member){
        if(member instanceof DcUser){
            DcUser user = (DcUser) member;
            boolean isMember = principals.retainAll(user.getPrincipals(experiment));
            return isMember;
        }
        if(member instanceof DcGroupPrincipal){
            return principals.contains(member);
        }
        return false;
    }
    
    @Override
    public Enumeration<? extends Principal> members(){
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addMember(Principal user){
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeMember(Principal user){
        throw new UnsupportedOperationException();
    }


}
