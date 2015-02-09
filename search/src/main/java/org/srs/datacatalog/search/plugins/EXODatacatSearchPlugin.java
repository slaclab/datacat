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
public class EXODatacatSearchPlugin implements DatacatPlugin {
    
    @Schema(name="EXORunIndex", alias="eri")
    class EXORunIndex extends Table {
        boolean joined = false;
            
        @Schema(name="runIndex", alias="runId") 
        public Column<Long> runId;
        @Schema(name="fullTypeName", alias="runType") 
        public Column<String> runType;
        @Schema(name="quality", alias="runQuality")
        public Column<String> runQuality;

        
        public EXORunIndex(){ super(); }
        
    };
    
    final private String namespace = "exo";
    EXORunIndex eri = new EXORunIndex();
    private boolean joined;
    
    private HashMap<String, Column> mappings = new HashMap<String, Column>();
    
    public EXODatacatSearchPlugin(){

        for(Column c: new EXORunIndex().getColumns()){
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
            return eri;
        }
        String metadataPivot = "nRun";
        DatasetVersions dsv = (DatasetVersions) statement;
        Column vecColumn = dsv.setupMetadataOuterJoin( metadataPivot,  Number.class );

        dsv.selection( eri.getColumns() ).leftOuterJoin( eri, vecColumn.eq( eri.runId )  );
        joined = true;
        return eri;
    }

    public void reset(){
        this.joined = false;
    }

}
