
package org.srs.webapps.datacat;

import org.srs.datacat.model.DatasetContainer;
import org.srs.webapps.datacat.plugins.ContainerLinkPlugin;
import org.srs.webapps.datacat.plugins.Link;
import java.util.HashMap;

/**
                     <c:when test="${row.type=='Dataset'}">
                        <datacat:downloadLink dataset="${row.id}"/>
                        <c:if test="${row.DatasetDataType == 'MERIT'}">
                            <datacat:skimLink path="${folderName}" dataset="${row.name}"/>
                        </c:if>
                        <c:if test="${row.DatasetFileFormat == 'fits' || row.DatasetFileFormat == 'fits.gz'}">
                            <a href='javascript:firefly.getExternalViewer().plotFile("${row.path}")'>FireFly</a>
                        </c:if>
                    </c:when>
 * @author bvan
 */
public class FireflyContainerPlugin implements ContainerLinkPlugin{

    @Override
    public Link addLink(DatasetContainer object){
        String path = object.getPath();
        HashMap<String, String> attr = new HashMap<>();
        String href = String.format("javascript:firefly.getExternalViewer().plotFile('%s')", path);
        attr.put("href", href);
        return new Link("FireFly", attr);
    }

}
