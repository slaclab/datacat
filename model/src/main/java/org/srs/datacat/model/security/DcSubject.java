
package org.srs.datacat.model.security;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import java.nio.file.attribute.UserPrincipal;
import java.util.Objects;

/**
 *
 * @author bvan
 */
public abstract class DcSubject implements  UserPrincipal, Comparable<DcSubject> {
    private final String name;

    public DcSubject(String name){
        this.name = checkNotNull(name);
    }
    
    @Override
    public String getName(){
        return name;
    }
    
    @Override
    public int compareTo(DcSubject o){
        String thisProject = this instanceof DcGroup ? ((DcGroup) this).getProject() : null;
        String thatProject = o instanceof DcGroup ? ((DcGroup) o).getProject() : null;
        return ComparisonChain.start()
            .compare(thisProject, thatProject, Ordering.natural().nullsFirst())
            .compare(getName(), o.getName(), Ordering.natural().nullsFirst())
            .result();    
    }
    
    public static Builder newBuilder(){
        return new Builder();
    }
    
    /**
     * Entry Builder.
     */
    public static final class Builder {
        
        private String name;
        private String project;
        private String type;
        
        private Builder(){ }

        public Builder name(String val){
            this.name = val;
            return this;
        }

        public Builder domain(String val){
            this.project = val;
            return this;
        }

        public Builder type(String val){
            this.type = val;
            return this;
        }
        
        public DcSubject build(){
            Objects.requireNonNull(this.name, "Need a non-null name");
            if(this.project != null || "g".equals(this.type)){
                return new DcGroup(this.name, this.project);
            }
            if(DcGroup.PROTECTED_NAME.equals(name) || DcGroup.PUBLIC_NAME.equals(name)){
                return new DcGroup(name, null);
            }
            return new DcUser(name);
        }
    }

}
