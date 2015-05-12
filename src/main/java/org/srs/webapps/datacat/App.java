
package org.srs.webapps.datacat;

import org.srs.webapps.datacat.auth.GroupManagerAuthProvider;
import org.srs.webapps.datacat.auth.GroupManagerServerFeature;

/**
 *
 * @author bvan
 */
public class App extends org.srs.datacat.rest.App {
    public App(){
        super(new GroupManagerAuthProvider());
        register(GroupManagerServerFeature.class);
    }
}
