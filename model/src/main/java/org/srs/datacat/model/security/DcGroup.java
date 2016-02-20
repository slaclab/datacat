package org.srs.datacat.model.security;

import java.nio.file.attribute.GroupPrincipal;
import java.util.Objects;

/**
 *
 * @author bvan
 */
public class DcGroup extends DcSubject implements GroupPrincipal {

    public static final DcGroup PUBLIC_GROUP = new DcGroup(PUBLIC_NAME, null);
    public static final DcGroup PROTECTED_GROUP = new DcGroup(PROTECTED_NAME, null);

    private final String project;

    public DcGroup(String name, String project){
        super(name);
        this.project = project;
    }
    
    public static DcGroup fromSpec(String spec) throws IllegalArgumentException {
        int splitIndex = spec.indexOf("@");
        if(spec.isEmpty() || splitIndex < 0){
            throw new IllegalArgumentException("Invalid Group Spec");
        }
        return new DcGroup(spec.substring(0, splitIndex), spec.substring(splitIndex+1));
    }
    
    @Override
    public String toString(){
        return (getName() != null ? getName() : "") + "@" + (project != null ? project : "");
    }

    public String getProject(){
        return project;
    }

    @Override
    public int hashCode(){
        String hstr = (getName() != null ? getName() : "") + "@" + (project != null ? project : "");
        return hstr.hashCode();
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
        if(!Objects.equals(getName(), other.getName())){
            return false;
        }
        return Objects.equals(this.project, other.project);
    }

}
