
package org.srs.datacat.model.security;


/**
 *
 * @author bvan
 */
public class DcUser extends DcSubject {
    
    public static final DcUser PUBLIC_USER = new DcUser(PUBLIC_NAME);
    
    public DcUser(String name){
        super(name);
    }
    
}
