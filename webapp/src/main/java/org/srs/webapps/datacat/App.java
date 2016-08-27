package org.srs.webapps.datacat;

import org.srs.datacat.rest.Utils;
import org.srs.datacat.shared.Provider;
import org.srs.webapps.datacat.auth.GroupManagerAuthProvider;
import org.srs.webapps.datacat.auth.GroupManagerServerFeature;

/**
 *
 * @author bvan
 */
public class App extends org.srs.datacat.rest.App {
    public App(){
        super(Utils.initDatasource("jdbc/datacat-prod"),
                Utils.initModelProvider(Provider.class),
                Utils.initUserLookupService(GroupManagerAuthProvider.class)
        );
        register(GroupManagerServerFeature.class);
    }
}
