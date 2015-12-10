
package org.srs.datacat.shared;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marker interface to denote which fields are patchable for a given class.
 * @author bvan
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Patchable {
    String column() default "";
}
