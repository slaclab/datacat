package org.srs.datacatalog.search.plugins;

import java.util.HashMap;
import org.zerorm.core.Column;
import org.zerorm.core.Select;
import org.zerorm.core.Table;
import org.zerorm.core.interfaces.Schema;
import org.zerorm.core.interfaces.SimpleTable;
import org.srs.datacatalog.search.tables.DatasetVersions;

/**
 *
 * @author bvan
 */
public class LsstKVSearchPlugin implements DatacatPlugin {
    
    @Schema(name="jgates_fitsTest.FitsKeyValues", alias="lsstkv")
    class FitsKeyValues extends Table {
        boolean joined = false;
            
        @Schema(name="fitsFileId") 
        public Column<Long> fileId;
        
        @Schema public Column<String> fitsKey;
        @Schema public Column<String> stringValue;
        @Schema public Column<Long> intValue;
        @Schema public Column<Double> doubleValue;
        @Schema public Column<Long> lineNum;
        @Schema public Column<Long> hdu;
        @Schema public Column<String> comment;
        
        public FitsKeyValues(){ super(); }
        
    };
    
    final private String namespace = "kv";
    FitsKeyValues lsstkv = new FitsKeyValues();
    private boolean joined;
    
    private HashMap<String, Column> mappings = new HashMap<>();
    
    public LsstKVSearchPlugin(){

        for(Column c: new FitsKeyValues().getColumns()){
            mappings.put( c.canonical(), c);
        }
    }

    public String getNamespace(){
        return this.namespace;
    }

    public boolean containsKey(String key){
        return mappings.containsKey( key );
    }

    public SimpleTable joinToStatement(String key, Select statement){
        if(joined){
            return lsstkv;
        }
        String metadataPivot = "fileId";
        DatasetVersions dsv = (DatasetVersions) statement;
        Column vecColumn = dsv.setupMetadataOuterJoin( metadataPivot,  Number.class );

        dsv.selection( lsstkv.getColumns() ).leftOuterJoin( lsstkv, vecColumn.eq( lsstkv.fileId )  );
        joined = true;
        return lsstkv;
    }

    public void reset(){
        this.joined = false;
    }

}
