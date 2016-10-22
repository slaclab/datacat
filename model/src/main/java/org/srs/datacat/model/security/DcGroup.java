package org.srs.datacat.model.security;

import java.nio.file.attribute.GroupPrincipal;

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

}
