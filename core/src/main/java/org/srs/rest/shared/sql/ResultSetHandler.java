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
public abstract class ResultSetHandler<T> {
    private T object;
    public ResultSetHandler() {}

    public abstract void handle(ResultSet rs) throws SQLException;
    public abstract void finalize(ResultSet rs) throws SQLException;
    public void setObject(T object) { this.object = object; }
    public T getObject() { return object; }
}