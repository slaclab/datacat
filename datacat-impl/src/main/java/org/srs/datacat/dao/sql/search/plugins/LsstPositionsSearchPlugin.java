package org.srs.datacat.dao.sql.search.plugins;

import java.sql.Timestamp;
import java.util.HashMap;
import org.zerorm.core.Column;
import org.zerorm.core.Table;
import org.zerorm.core.interfaces.Schema;
import org.zerorm.core.interfaces.SimpleTable;
import org.srs.datacat.dao.sql.search.tables.MetajoinedStatement;

/**
 *
 * @author bvan
 */
public class LsstPositionsSearchPlugin implements DatacatPlugin {
    
    @Schema(name="jgates_fitsTest.FitsPositions", alias="lsstpos")
    class FitsPositions extends Table {
            
        @Schema(name="fitsFileId") 
        public Column<Long> fileId;
        @Schema public Column<Long> hdu;
        @Schema public Column<Double> equinox;
        @Schema public Column<Double> pDec;
        @Schema public Column<Double> pRa;
        @Schema public Column<Double> rotAng;
        @Schema public Column<Timestamp> pDate;
        
        public FitsPositions(){ super(); }
        
    };
    
    private static final String NAMESPACE = "pos";
    FitsPositions lsstpos = new FitsPositions();
    private boolean joined;
    
    private HashMap<String, Column> mappings = new HashMap<>();
    
    public LsstPositionsSearchPlugin(){

        for(Column c: new FitsPositions().getColumns()){
            mappings.put(c.canonical(), c);
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
    public SimpleTable joinToStatement(String key, MetajoinedStatement statement){
        if(joined){
            return lsstpos;
        }
        String metadataPivot = "fileId";
        Column vecColumn = statement.setupMetadataOuterJoin(metadataPivot, Number.class);

        statement.selection(lsstpos.getColumns())
                .leftOuterJoin(lsstpos, vecColumn.eq(lsstpos.fileId));
        joined = true;
        return lsstpos;
    }

}
