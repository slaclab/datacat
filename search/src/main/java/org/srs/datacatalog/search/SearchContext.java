/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacatalog.search;

import org.freehep.commons.lang.AST;
import org.zerorm.core.Expr;
import org.zerorm.core.Select;

/**
 *
 * @author bvan
 */
public interface SearchContext {

    public boolean inScope(String ident);
    public boolean inSelectionScope(String ident);
    public boolean inPluginScope(String ident);
    public boolean inMetanameScope(String ident);
    public void assertIdentsValid(AST ast);

    //public Expr evaluateNode(AST.Node l);
    public Select getStatement();
}
