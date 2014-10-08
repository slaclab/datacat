/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacatalog.search;

import org.freehep.commons.lang.AST;

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
    public void evaluate(AST.Node ast);

}
