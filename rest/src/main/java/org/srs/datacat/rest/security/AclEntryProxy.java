
package org.srs.datacat.rest.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Objects;
import org.srs.datacat.model.security.DcAclEntry;
import org.srs.datacat.model.security.DcAclEntryScope;
import org.srs.datacat.model.security.DcPermissions;
import org.srs.datacat.model.security.DcSubject;

/**
 * DcAclEntry proxy class for Jackson.
 * @author bvan
 */
@JsonTypeName(value="aclEntry")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="_type", defaultImpl=AclEntryProxy.class)
@JsonDeserialize(builder=AclEntryProxy.Builder.class)
@JsonPropertyOrder({"subject","permissions"})
public class AclEntryProxy {
    private DcAclEntry entry;

    public AclEntryProxy(DcAclEntry entry){
        this.entry = entry;
    }

    public String getSubject(){
        return entry.getSubject().toString();
    }

    public String getPermissions(){
        return DcPermissions.pack(entry.getPermissions());
    }
    
    @JsonIgnore
    public DcAclEntry entry(){
        return this.entry;
    }

    @Override
    public String toString(){
        return String.format( "subject:%s, permissions:%s", getSubject() ,getPermissions());
    }
    
    /**
     * Builder class.
     */
    public static class Builder {
        private String subject;
        private String permissions;
        
        private Builder(){ }
        
        @JsonSetter
        public Builder subject(String val){
            this.subject = val;
            return this;
        }
        
        @JsonSetter
        public Builder permissions(String val){
            this.permissions = val;
            return this;
        }
        
        public AclEntryProxy build(){
            Objects.requireNonNull(subject, "Subject must not be null");
        
            DcSubject dcSubject = DcSubject.newBuilder()
                    .name(subject)
                    .type("g").build();
            if(permissions == null){
                permissions = "";
            }
            DcAclEntry e = DcAclEntry.newBuilder()
                    .permissions(permissions)
                    .subject(dcSubject)
                    .scope(DcAclEntryScope.ACCESS)
                    .build();
            return new AclEntryProxy(e);
        }
    }
    
}
