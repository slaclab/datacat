
package org.srs.datacat.security;


import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.file.attribute.UserPrincipal;

/**
 *
 * @author bvan
 */
public class DcUser implements UserPrincipal {
    
    private final String name;
    
    public DcUser(String name){
        this.name = checkNotNull(name);
    }
    
    @Override
    public String getName(){
        return name;
    }
    
}
