
package org.srs.datacat.dao.sql.search;

import org.freehep.commons.lang.AST;

/**
 * Class to implement when writing an AST parser.
 * @author bvan
 */
public interface SearchContext {

    boolean inScope(String ident);
    boolean inSelectionScope(String ident);
    boolean inPluginScope(String ident);
    boolean inMetanameScope(String ident);
    void assertIdentsValid(AST ast);
    void evaluate(AST.Node ast);

}
