
package org.srs.datacat.dao;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.DatacatRecord;

/**
 *
 * @author bvan
 */
public interface BaseDAO extends AutoCloseable {

    void commit() throws IOException;
    
    @Override
    void close() throws IOException;
    
    /**
     * Create a Folder, Group, or Dataset node.
     * 
     * @param <T> Folder, Group, or Dataset
     * @param parent The record which will be the parent
     * @param name The name of the target node
     * @param request The request Object representing the node to be created
     * @return The representation of the node that was created
     * @throws IOException An exception occurred performing the operation or talking to the data source.
     * @throws FileSystemException An illegal action occurred. 
     */
    <T extends DatacatNode> T createNode(DatacatRecord parent, String name, 
            T request) throws IOException, FileSystemException;
    
    /**
     * Using the parent record, find the object that corresponds to the given path.
     * 
     * @param parent
     * @param name The file name
     * @return
     * @throws IOException An exception occurred performing the operation or talking to the data source.
     * @throws NoSuchFileException No parent with the name found in parent.
     */
    DatacatNode getObjectInParent(DatacatRecord parent, String name) throws IOException, NoSuchFileException;
        
    /**
     * Merge metadata of an existing record.
     * 
     * @param record A Folder, Group, or DatasetVersion
     * @param metaData Metadata to be merged
     * @throws IOException An exception occurred performing the operation or talking to the data source.
     */
    void mergeMetadata(DatacatRecord record, Map<String, Object> metaData) throws IOException;
    
    /**
     * Set the ACL field on a record.
     * 
     * @param record A Folder, Group, or DatasetVersion
     * @param acl String version of the acl to be set.
     * @throws IOException An exception occurred performing the operation or talking to the data source.
     */
    void setAcl(DatacatRecord record, String acl) throws IOException;
    
    /**
     * Delete a DatacatRecord.
     * 
     * @param record A Folder, Group, or Dataset
     * @throws IOException An exception occurred performing the operation or talking to the data source.
     */
    void delete(DatacatRecord record) throws IOException;

}
