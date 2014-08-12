/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacatalog.search.plugins;

import org.zerorm.core.Select;
import org.zerorm.core.interfaces.SimpleTable;

/**
 *
 * @author bvan
 */
public interface DatacatPlugin {

    public String getNamespace();
    public SimpleTable joinToStatement(Select statement);
    public boolean containsKey(String key);
    public void reset();
    
}