
package org.srs.datacat.model;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;

/**
 * Custom file exceptions/file exception helper.
 */
public enum DcExceptions {
    
    NO_SUCH_VERSION, 
    NO_SUCH_LOCATION, 
    DATASET_EXISTS, 
    VERSION_EXISTS, 
    LOCATION_EXISTS, 
    VERSION_CONFLICT, 
    NEWER_VERSION_EXISTS;

    public boolean throwError(String targetPath, String msg) throws FileSystemException{
        String path = targetPath;
        String reason = toString();
        switch(this){
            case NO_SUCH_VERSION:
            case NO_SUCH_LOCATION:
                throw new NoSuchFileException(path, msg, reason);
            default:
                throw new FileAlreadyExistsException(path, msg, reason);
        }
    }
    
}
