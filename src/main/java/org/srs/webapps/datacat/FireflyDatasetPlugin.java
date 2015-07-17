package org.srs.webapps.datacat;

import java.util.HashMap;
import org.srs.datacat.model.DatasetModel;
import org.srs.webapps.datacat.plugins.DatasetLinkPlugin;
import org.srs.webapps.datacat.plugins.Link;

/**
 *
 * @author bvan
 */
public class FireflyDatasetPlugin implements DatasetLinkPlugin {

    @Override
    public Link addLink(DatasetModel object){
        if("fits".equals(object.getFileFormat()) || "fits.gz".equals(object.getFileFormat())){
            String path = object.getPath();
            HashMap<String, String> attr = new HashMap<>();
            String href = String.format("javascript:firefly.getExternalViewer().plotFile('%s')", path);
            attr.put("href", href);
            return new Link("FireFly", attr);
        }
        return null;
    }

}
