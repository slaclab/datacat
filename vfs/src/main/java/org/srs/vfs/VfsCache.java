/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.srs.vfs;

import java.nio.file.Path;

/**
 *
 * @author bvan
 */
public interface VfsCache<V extends VirtualFile> {

    V getFile(final Path fileName);

    void putFile(final V fileObject);

    boolean putFileIfAbsent(final V fileObject);

    boolean removeFile(final Path key);

    void touchFile(final V fileObject);
    
    void clear();

    void close();
    
}
