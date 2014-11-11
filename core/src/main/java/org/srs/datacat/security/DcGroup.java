
package org.srs.datacat.security;

import java.nio.file.attribute.GroupPrincipal;
import java.util.Objects;

/**
 *
 * @author bvan
 */
public class DcGroup implements GroupPrincipal {
    
    public static final String PUBLIC_NAME = "$PUBLIC$";
    public static final String PROTECTED_NAME = "$PROTECTED$";
    
    public static final DcGroup PUBLIC_GROUP = new DcGroup(PUBLIC_NAME,null);
    public static final DcGroup PROTECTED_GROUP = new DcGroup(PROTECTED_NAME,null);
       
    private final String name;
    private final String experiment;
    
    public DcGroup(String name, String experiment){
        this.experiment = experiment;
        this.name = name;
    }
    
    @Override
    public String getName(){
        return name;
    }

    @Override
    public String toString(){
        return (name != null ? name : "") + "@" + (experiment != null ? experiment : "");
    }

    @Override
    public int hashCode(){
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj){
        if(obj == null){
            return false;
        }
        if(getClass() != obj.getClass()){
            return false;
        }
        final DcGroup other = (DcGroup) obj;
        if(!Objects.equals( this.name, other.name )){
            return false;
        }
        if(!Objects.equals( this.experiment, other.experiment )){
            return false;
        }
        return true;
    }
    
}