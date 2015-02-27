
package org.srs.datacat.model.security;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import java.nio.file.attribute.UserPrincipal;

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

}
