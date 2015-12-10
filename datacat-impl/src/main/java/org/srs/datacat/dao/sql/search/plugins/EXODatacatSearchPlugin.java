package org.srs.datacat.dao.sql.search.plugins;

import java.util.HashMap;
import org.zerorm.core.Column;
import org.zerorm.core.Table;
import org.zerorm.core.interfaces.Schema;
import org.zerorm.core.interfaces.SimpleTable;
import org.srs.datacat.dao.sql.search.tables.DatasetVersions;

/**
 *
 * @author bvan
 */
public class EXODatacatSearchPlugin implements DatacatPlugin {
    
    @Schema(name="EXORunIndex", alias="eri")
    class EXORunIndex extends Table {
            
        @Schema(name="runIndex", alias="runId") 
        public Column<Long> runId;
        @Schema(name="fullTypeName", alias="runType") 
        public Column<String> runType;
        @Schema(name="quality", alias="runQuality")
        public Column<String> runQuality;

        public EXORunIndex(){ super(); }
    };
    
    private static final String NAMESPACE = "exo";
    EXORunIndex eri = new EXORunIndex();
    private boolean joined;
    
    private HashMap<String, Column> mappings = new HashMap<>();
    
    public EXODatacatSearchPlugin(){

        for(Column c: new EXORunIndex().getColumns()){
            mappings.put( c.canonical(), c);
        }
    }

    @Override
    public String getNamespace(){
        return NAMESPACE;
    }

    @Override
    public boolean containsKey(String key){
        return mappings.containsKey( key );
    }

    @Override
    public SimpleTable joinToStatement(String key, DatasetVersions statement){
        if(joined){
            return eri;
        }
        String metadataPivot = "nRun";
        DatasetVersions dsv = (DatasetVersions) statement;
        Column vecColumn = dsv.setupMetadataOuterJoin( metadataPivot,  Number.class );

        dsv.selection( eri.getColumns() ).leftOuterJoin( eri, vecColumn.eq( eri.runId )  );
        joined = true;
        return eri;
    }

}
