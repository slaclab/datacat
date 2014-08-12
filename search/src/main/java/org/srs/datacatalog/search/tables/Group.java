
package org.srs.datacatalog.search.tables;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.zerorm.core.Column;
import org.zerorm.core.Select;
import org.zerorm.core.Table;
import org.zerorm.core.interfaces.MaybeHasAlias;
import org.zerorm.core.interfaces.Schema;

/**
 *
 * @author bvan
 */
@Schema(name = "DatasetGroup")
public class Group extends Table {
    @Schema public Column<Long> datasetGroup;
    @Schema public Column<String> name;
    @Schema public Column<Long> datasetLogicalFolder;

    public Group(){
        super();
    }

    public Select is(Long pk){
        return select( datasetGroup ).
                where( datasetGroup.eq( datasetGroup.checkedParam( "group", pk ) ) );
    }

    public Select groupsInRecursiveFolders(Long folder){
        return select( datasetGroup ).
                where( datasetLogicalFolder.in( new Folder().recursiveFoldersFrom( folder ) ) );
    }

    public Select groupsInFolder(Long folder){
        return select( datasetGroup ).where( datasetLogicalFolder.eq( folder ) );
    }
    
    public class GroupSelect extends MetajoinedStatement {
        
        public GroupSelect(){
            from(Group.this);
        }
        
        public GroupSelect(MaybeHasAlias... a){
            selection( Arrays.asList( a ) ).from(Group.this);
        }
        
        @Override
        public Metatable getMetatableForType(String alias, Class type){
            Metatable ms = null;
            if(Number.class.isAssignableFrom( type )){
                ms = new GroupMetanumber().as( alias, Metatable.class );
            } else if(String.class.isAssignableFrom( type )){
                ms = new GroupMetastring().as( alias, Metatable.class );
            } else if(Timestamp.class.isAssignableFrom( type )){
                ms = new GroupMetatimestamp().as( alias, Metatable.class );
            }
            return ms;
        }

        @Override
        public Map<String, Select> getMetajoins(){
            return metaJoins;
        }

        @Override
        public Column getMetajoinColumn(){
            return datasetGroup;
        }

        @Override
        public String getMetanamePrefix(){
            return "dsgmv";
        }
    }

}
