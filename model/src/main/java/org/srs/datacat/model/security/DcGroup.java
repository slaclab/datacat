package org.srs.datacat.model.security;

import java.nio.file.attribute.GroupPrincipal;
import java.util.Objects;

/**
 *
 * @author bvan
 */
public class DcGroup extends DcSubject implements GroupPrincipal {

    public static final DcGroup PUBLIC_GROUP = new DcGroup(PUBLIC_NAME);
    public static final DcGroup PROTECTED_GROUP = new DcGroup(PROTECTED_NAME);
    
    public DcGroup(String name){
        super(name);
    }

    public DcGroup(String name, String project){
        super((name != null ? name : "") + "@" + (project != null ? project : ""));
    }
        
    @Override
    public String toString(){
        return getName();
    }

    @Override
    public int hashCode(){
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object obj){
        if(obj == null){
            return false;
        }
        if(!getClass().isAssignableFrom(obj.getClass()) &&
                !obj.getClass().isAssignableFrom(getClass())){
            return false;
        }
        final DcGroup other = (DcGroup) obj;
        return Objects.equals(getName(), other.getName());
    }

}
