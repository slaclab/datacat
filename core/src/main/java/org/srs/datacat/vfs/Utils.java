
package org.srs.datacat.vfs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.Iterator;
import java.util.LinkedList;
import org.srs.datacat.model.DatacatNode;

/**
 *
 * @author bvan
 */
public final class Utils {
    
    private Utils(){}
    
    public static DirectoryStream<DatacatNode> getStream(final LinkedList<DatacatNode> list){
        return new DirectoryStream<DatacatNode>(){
            @Override
            public Iterator<DatacatNode> iterator(){
                return new Iterator<DatacatNode>(){
                    @Override
                    public boolean hasNext(){
                        return list.peek() != null;
                    }

                    @Override
                    public DatacatNode next(){
                        return list.pop();
                    }
                };
            }

            @Override
            public void close() throws IOException{
                list.clear();
            }
        };
    }

}
