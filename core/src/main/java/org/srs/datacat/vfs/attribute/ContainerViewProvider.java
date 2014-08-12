
package org.srs.datacat.vfs.attribute;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import org.srs.datacat.shared.DatacatObject;
import org.srs.datacat.shared.DatacatObjectBuilder.DatasetContainerBuilder;
import org.srs.datacat.shared.container.BasicStat;
import org.srs.datacat.shared.container.BasicStat.StatType;
import org.srs.datacat.sql.ContainerDAO;
import org.srs.datacat.sql.Utils;
import org.srs.datacat.vfs.DcFile;

/**
 *
 * @author bvan
 */
public class ContainerViewProvider implements DcViewProvider<StatType> {
    
    private final DcFile file;
    private HashMap<String,BasicStat> stats = new HashMap<>(4);

    public ContainerViewProvider(DcFile file){
        this.file = file;
    }

    @Override
    public String name(){
        return "cstat";
    }
    
    @Override
    public DatacatObject withView(StatType statType) throws FileNotFoundException, IOException {
        if(statType == StatType.NONE){
            return file.getDatacatObject();
        }
        String wantName = statType.toString();
        String basicName = StatType.BASIC.toString();
        
        BasicStat basicStat = null;
        if(!stats.containsKey(wantName)){
            try(ContainerDAO dao = new ContainerDAO(Utils.getConnection())){
                if(!stats.containsKey(basicName)){
                    stats.put(basicName, dao.getBasicStat(file.getDatacatObject()));
                }
                basicStat = stats.get(basicName);
                if(statType == StatType.DATASET){
                    BasicStat s = dao.getDatasetStat(file.getDatacatObject(), basicStat);
                    stats.put(statType.toString(), s);
                }
            } catch(SQLException ex) {
                throw new IOException("unknown SQL error", ex);
            }
        }
        BasicStat retStat = stats.get(wantName);
        DatasetContainerBuilder b = new DatasetContainerBuilder(file.getDatacatObject());
        b.stat( retStat );
        return b.build();
    }
    
}
