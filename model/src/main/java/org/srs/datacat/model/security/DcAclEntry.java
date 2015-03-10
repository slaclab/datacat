package org.srs.datacat.model.security;


import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author bvan
 */
public final class DcAclEntry {

    private final DcSubject who;
    private final DcAclEntryScope scope;
    private final Set<DcPermissions> perms;
    
    // cached hash code
    private volatile int hash;

    // private constructor
    private DcAclEntry(DcAclEntryScope scope,
            DcSubject who,
            Set<DcPermissions> perms){
        this.who = who;
        this.scope = scope;
        this.perms = perms;
    }
    
    public DcSubject getSubject(){
        return who;
    }

    public DcAclEntryScope getScope(){
        return scope;
    }

    public Set<DcPermissions> getPermissions(){
        return new HashSet<>(perms);
    }

    @Override
    public boolean equals(Object ob){
        if(ob == this) {
            return true;
        }
        if(ob == null || !(ob instanceof DcAclEntry)) {
            return false;
        }
        DcAclEntry other = (DcAclEntry) ob;
        if(!this.who.equals(other.who)) {
            return false;
        }
        if(!this.perms.equals(other.perms)) {
            return false;
        }
        return true;
    }

    private static int hash(int h, Object o){
        return h * 127 + o.hashCode();
    }

    @Override
    public int hashCode(){
        // return cached hash if available
        if(hash != 0) {
            return hash;
        }
        int h = who.hashCode();
        h = hash(h, perms);
        h = hash(h, scope);
        hash = h;
        return hash;
    }
    
    /**
     * Builder.
     */
    public static final class Builder {
        private DcSubject who;
        private DcAclEntryScope scope;
        private Set<DcPermissions> perms;
       
        private Builder(DcSubject who,
                Set<DcPermissions> perms){
            this.who = who;
            this.perms = perms;
        }

        public DcAclEntry build(){
            scope = scope == null ? DcAclEntryScope.ACCESS : scope;
            return new DcAclEntry(scope, who, perms);
        }

        public Builder subject(DcSubject val){
            if(val == null) {
                throw new NullPointerException();
            }
            this.who = val;
            return this;
        }
        
        public Builder scope(DcAclEntryScope val){
            this.scope = val;
            return this;
        }

        public Builder permissions(Set<DcPermissions> val){
            if(val.isEmpty()){
                // EnumSet.copyOf does not allow empty set
                val = Collections.emptySet();
            } else {
                // copy and check for erroneous elements
                val = EnumSet.copyOf(val);
                checkSet(val, DcPermissions.class);
            }

            this.perms = val;
            return this;
        }
        
        public Builder permissions(String val){
            this.perms = DcPermissions.unpackString(val);
            return this;
        }
        
        private static void checkSet(Set<?> set, Class<?> scope){
            for(Object e: set){
                if(e == null) {
                    throw new NullPointerException();
                }
                scope.cast(e);
            }
        }

    }

    public static Builder newBuilder(){
        Set<DcPermissions> perms = Collections.emptySet();
        return new Builder(null, perms);
    }

    public static Builder newBuilder(DcAclEntry entry){
        return new Builder(entry.who, entry.perms);
    }

    @Override
    public String toString(){
        
        return String.format("%s:%s:%s:", 
                who != null ? who.toString() : "", 
                who instanceof DcGroup ? "g" : "",
                DcPermissions.pack(perms));
    }

}
