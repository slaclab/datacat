
package org.srs.webapps.datacat;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.TracingConfig;

import org.glassfish.jersey.server.mvc.jsp.JspMvcFeature;

/**
 *
 * @author bvan
 */
public class WebApp extends ResourceConfig {

    public WebApp() {
        System.out.println("App initialization");
        // MVC.
        register(JspMvcFeature.class);
        packages("org.srs.webapps.datacat.controllers");

        // Tracing support.
        property(ServerProperties.TRACING, TracingConfig.ON_DEMAND.name());
    }
}
