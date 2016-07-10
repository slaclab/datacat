package org.srs.webapps.datacat.auth.tomcat;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 *
 * @author bvan
 */
public class TomcatServerFeature implements Feature {

    @Override
    public boolean configure(FeatureContext context){
        context.register(TomcatUserAuthProvider.class);
        return true;
    }

}
