
package org.srs.webapps.datacat.auth;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 *
 * @author bvan
 */
public class GroupManagerServerFeature implements Feature {

    @Override
    public boolean configure(FeatureContext context){
        context.register(GroupManagerServerAuthFilter.class);
        context.register(GroupManagerAuthProvider.class);
        return true;
    }

}
