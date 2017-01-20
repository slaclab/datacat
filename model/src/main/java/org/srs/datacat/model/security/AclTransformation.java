package org.srs.datacat.model.security;

import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.srs.datacat.model.security.DcAclEntryScope.*;

/**     
 * Portions of the code adapted from AclTransformation and ScopedAclEntries classes in
 * HDFS implementation. 
 * 
 * @author bvan
 */
public final class AclTransformation {
    
    private AclTransformation(){}

    private static final int MAX_ENTRIES = 32;

    public static List<DcAclEntry> mergeAclEntries(List<DcAclEntry> existingAcl,
            List<DcAclEntry> inAclSpec) throws IOException{

        HashMap<DcSubject, DcAclEntry> result = new HashMap<>();
        for(DcAclEntry e: existingAcl){
            result.put(e.getSubject(), e);
        }
        
        ValidatedAclSpec aclSpec = new ValidatedAclSpec(inAclSpec);
        for(DcAclEntry newEntry: aclSpec){
            result.put(newEntry.getSubject(), newEntry);
        }

        ArrayList<DcAclEntry> retAcl = new ArrayList<>(MAX_ENTRIES);
        retAcl.addAll(result.values());
        return buildAndValidateAcl(retAcl);
    }
    
    public static Optional<List<DcAclEntry>> parseAcl(String aclString){
        if(aclString == null || aclString.isEmpty()){
            return Optional.absent();
        }
        List<DcAclEntry> acl = new ArrayList<>();
        String[] aclEntries = aclString.split(",");
        for(String aclEntry: aclEntries){
            acl.add(parseAclEntry(aclEntry));
        }
        return Optional.of(acl);
    }
    
    public static String aclToString(List<DcAclEntry> acl){
        StringBuilder builder = new StringBuilder();
        for(Iterator<DcAclEntry> iter = acl.iterator(); iter.hasNext();){
            builder.append(iter.next()).append(iter.hasNext() ? "," : "");
        }
        return builder.toString();
    }
    
    public static DcAclEntry parseAclEntry(String aclEntryString){
        String[] ace = aclEntryString.split(":");
        String subjectString = ace[0];
        String entryType = ace[1];
        
        // TODO: Support user principals as well
        if(!"g".equals(entryType)){
            throw new IllegalArgumentException("Illegal principal type:" + entryType);
        }
                
        DcSubject subject = DcSubject.newBuilder()
                .name(subjectString)
                .type(entryType).build();
        
        String permissions = ace[2];
        Set<DcPermissions> perms = DcPermissions.unpackString(permissions);
        return DcAclEntry.newBuilder()
                .subject(subject)
                .permissions(perms)
                .scope(ACCESS)
                .build();
    }

    private static List<DcAclEntry> buildAndValidateAcl(ArrayList<DcAclEntry> aclBuilder) throws IOException{
        if(aclBuilder.size() > MAX_ENTRIES){
            throw new IOException("Invalid ACL: ACL has " + aclBuilder.size()
                    + " entries, which exceeds maximum of " + MAX_ENTRIES + ".");
        }
        
        Collections.sort(aclBuilder, ACL_INPUT_COMPARATOR);
        // Full iteration to check for duplicates and invalid named entries.
        DcAclEntry prevEntry = null;
        for(Iterator<DcAclEntry> iter = aclBuilder.iterator(); iter.hasNext();){
            DcAclEntry entry = iter.next();
            if(entry.getPermissions().isEmpty()){
                iter.remove();
            }
            if(prevEntry != null && ACL_ENTRY_COMPARATOR.compare(prevEntry, entry) == 0){
                throw new IOException(
                        "Invalid ACL: multiple entries with same scope, type and name.");
            }
            prevEntry = entry;
        }
        aclBuilder.trimToSize();
        return Collections.unmodifiableList(aclBuilder);
    }

    /**
     * Comparator that enforces required ordering for entries within an ACL: 1. First we have OWNER
     * entries, then ACCESS entries, then DEFAULT entries. 2. Then, sort by principal name.
     */
    static final Comparator<DcAclEntry> ACL_ENTRY_COMPARATOR = new Comparator<DcAclEntry>(){
        @Override
        public int compare(DcAclEntry entry1, DcAclEntry entry2){
            DcSubject principal1 = entry1.getSubject();
            DcSubject principal2 = entry2.getSubject();
            return ComparisonChain.start()
                .compare(entry1.getScope(), entry2.getScope(), Ordering.explicit(ACCESS, DEFAULT))
                .compare(principal1, principal2, Ordering.natural().nullsFirst())
                .result();
        }
    };
    
    /**
     * Comparator that enforces required ordering for entries within an ACL: 1. First we have OWNER
     * entries, then ACCESS entries, then DEFAULT entries. 2. Then, sort by principal name.
     */
    static final Comparator<DcAclEntry> ACL_INPUT_COMPARATOR = new Comparator<DcAclEntry>(){
        @Override
        public int compare(DcAclEntry entry1, DcAclEntry entry2){
            DcSubject principal1 = entry1.getSubject();
            DcSubject principal2 = entry2.getSubject();
            return ComparisonChain.start()
                .compare(principal1, principal2, Ordering.natural().nullsFirst())
                .result();
        }
    };
    
    private static final class ValidatedAclSpec implements Iterable<DcAclEntry> {
        private final List<DcAclEntry> aclSpec;

        public ValidatedAclSpec(List<DcAclEntry> aclSpec) throws IOException{
            if(aclSpec.size() > MAX_ENTRIES){
                throw new IOException("Invalid ACL: ACL spec has " + aclSpec.size()
                        + "  entries, which exceeds maximum of " + MAX_ENTRIES + ".");
            }
            Collections.sort(aclSpec, ACL_ENTRY_COMPARATOR);
            this.aclSpec = aclSpec;
        }

        public boolean containsKey(DcAclEntry key){
            return Collections.binarySearch(aclSpec, key, ACL_ENTRY_COMPARATOR) >= 0;
        }

        public DcAclEntry findByKey(DcAclEntry key){
            int index = Collections.binarySearch(aclSpec, key, ACL_ENTRY_COMPARATOR);
            if(index >= 0){
                return aclSpec.get(index);
            }
            return null;
        }

        @Override
        public Iterator<DcAclEntry> iterator(){
            return aclSpec.iterator();
        }
    }

}
