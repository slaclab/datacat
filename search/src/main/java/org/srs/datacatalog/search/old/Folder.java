
package org.srs.datacatalog.search.old;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Map;
import org.srs.datacatalog.search.tables.MetajoinedStatement;
import org.srs.datacatalog.search.tables.Metatable;
import org.zerorm.core.Column;
import org.zerorm.core.Expr;
import org.zerorm.core.Op;
import org.zerorm.core.Param;
import org.zerorm.core.Select;
import org.zerorm.core.Sql;
import org.zerorm.core.Table;
import org.zerorm.core.interfaces.MaybeHasAlias;
import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "DatasetLogicalFolder")
public class Folder extends Table {
    @Schema public Column<Long> datasetLogicalFolder;
    @Schema public Column<String> name;
    @Schema public Column<Long> parent;

    public Folder(){
        super();
    }

    public static Select recursiveFoldersFrom(Long folder){
        return oracleRecursiveFoldersFrom( folder );
    }

    private static Select oracleRecursiveFoldersFrom(Long folder){
        Folder f = new Folder();
        String connectBy = "1=1 connect by prior datasetlogicalfolder = parent  " + "start with datasetlogicalFolder";
        Param<Long> parentFolder = new Param<>("parentFolder", folder);
        Expr e = new Expr( new Sql( connectBy ), Op.EQ, parentFolder ) { };
        return f.select( f.datasetLogicalFolder ).where( e );
    }
    
    public class FolderSelect extends MetajoinedStatement {
        
        public FolderSelect(){
            from(Folder.this);
        }
        
        public FolderSelect(MaybeHasAlias... a){
            selection( Arrays.asList( a ) ).from(Folder.this);
        }
        
        @Override
        public Metatable getMetatableForType(String alias, Class type){
            Metatable ms = null;
            if(Number.class.isAssignableFrom( type )){
                ms = new FolderMetanumber().as( alias, Metatable.class );
            } else if(String.class.isAssignableFrom( type )){
                ms = new FolderMetastring().as( alias, Metatable.class );
            } else if(Timestamp.class.isAssignableFrom( type )){
                ms = new FolderMetatimestamp().as( alias, Metatable.class );
            }
            return ms;
        }

        @Override
        public Map<String, Select> getMetajoins(){
            return metaJoins;
        }

        @Override
        public Column getMetajoinColumn(){
            return datasetLogicalFolder;
        }

        @Override
        public String getMetanamePrefix(){
            return "lf";
        }
    }

}
