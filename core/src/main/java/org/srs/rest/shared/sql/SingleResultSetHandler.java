/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.rest.shared.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author bvan
 */
public abstract class SingleResultSetHandler<T> extends ResultSetHandler<T> {

    /**
     * Do nothing for handle
     * @param rs
     * @throws SQLException 
     */
    public void handle(ResultSet rs) throws SQLException {}
    
}
