package org.srs.datacat.dao.sql.search.plugins;

import java.util.HashMap;
import org.zerorm.core.Column;
import org.zerorm.core.Select;
import org.zerorm.core.Table;
import org.zerorm.core.interfaces.Schema;
import org.zerorm.core.interfaces.SimpleTable;
import org.srs.datacat.dao.sql.search.tables.DatasetVersions;

/**
 *
 * @author bvan
 */
public class LsstKVSearchPlugin implements DatacatPlugin {
    
    @Schema(name="jgates_fitsTest.FitsKeyValues", alias="lsstkv")
    class FitsKeyValues extends Table {
            
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
    
    private static final String NAMESPACE = "kv";
    FitsKeyValues lsstkv = new FitsKeyValues();
    private boolean joined;

    private HashMap<String, Column> mappings = new HashMap<>();

    public LsstKVSearchPlugin(){

        for(Column c: new FitsKeyValues().getColumns()){
            mappings.put(c.canonical(), c);
        }
    }

    @Override
    public String getNamespace(){
        return NAMESPACE;
    }

    @Override
    public boolean containsKey(String key){
        return mappings.containsKey(key);
    }

    @Override
    public SimpleTable joinToStatement(String key, Select statement){
        if(joined){
            return lsstkv;
        }
        String metadataPivot = "fileId";
        DatasetVersions dsv = (DatasetVersions) statement;
        Column vecColumn = dsv.setupMetadataOuterJoin(metadataPivot, Number.class);

        dsv.selection(lsstkv.getColumns()).leftOuterJoin(lsstkv, vecColumn.eq(lsstkv.fileId));
        joined = true;
        return lsstkv;
    }

}
