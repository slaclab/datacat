
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
    
    public static final String PUBLIC_NAME = "$PUBLIC$";
    public static final String PROTECTED_NAME = "$PROTECTED$";
    public static final String SYSTEM_NAME = "$SYSTEM$";

    public DcSubject(String name){
        this.name = checkNotNull(name);
    }
    
    @Override
    public String getName(){
        return name;
    }
    
    @Override
    public int compareTo(DcSubject o){
        Boolean thisIsGroup = this instanceof DcGroup ? true : null;
        Boolean thatIsGroup = o instanceof DcGroup ? true : null;
        return ComparisonChain.start()
            .compare(thisIsGroup, thatIsGroup, Ordering.natural().nullsFirst())
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
        private String type;
        
        private Builder(){ }

        public Builder name(String val){
            this.name = val;
            return this;
        }

        public Builder type(String val){
            this.type = val;
            return this;
        }
        
        public DcSubject build(){
            Objects.requireNonNull(this.name, "Need a non-null name");
            if("g".equals(this.type)){
                return new DcGroup(this.name);
            }
            if(DcGroup.PROTECTED_NAME.equals(name) || DcGroup.PUBLIC_NAME.equals(name)){
                return new DcGroup(name, null);
            }
            return new DcUser(name);
        }
    }

}
