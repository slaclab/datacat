/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


/*
 * This file is derived in part from sun.nio.fs.AbstractPath.java.
 * It is licensed as GPLv2 with "Classpath" exception"
 */
package org.srs.vfs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author bvan
 */
public class AbstractPath<T extends AbstractPath> implements Path {
    
    private final AbstractFs fileSystem;
    protected final String path;
    private volatile int[] offsets;
    private final PathProvider<T> pathProvider;
    
    // cached version of the hash
    private volatile int hash;
    
    protected AbstractPath(PathProvider<T> pathProvider, String path){
        this.path = PathUtils.normalizeSeparators( path );
        this.pathProvider = pathProvider;
        this.fileSystem = null;
    }
        
    private T createPath(AbstractFs fs, String path){
        return (T) pathProvider.getPath(path);
    }
    
    @Override
    public boolean isAbsolute() {
        return (path.length() > 0 && path.charAt(0) == '/');
    }
    
    @Override
    public AbstractFs getFileSystem(){
        return fileSystem;
    }
    
    @Override
    public T getRoot() {
        if (path.length() > 0 && path.charAt(0) == '/') {
            return createPath(getFileSystem(), "/");
        } else {
            return null;
        }
    }

    @Override
    public T getFileName() {
        initOffsets();
        return createPath(getFileSystem(), PathUtils.getFileName( path, offsets ));
    }

    @Override
    public T getParent() {
        initOffsets();
        return createPath(getFileSystem(), PathUtils.getParentPath( path, offsets ));
    }

    @Override
    public int getNameCount() {
        initOffsets();
        return offsets.length;
    }

    @Override
    public T getName(int index) {
        initOffsets();
        if (index < 0)
            throw new IllegalArgumentException();
        if (index >= offsets.length)
            throw new IllegalArgumentException();

        int begin = offsets[index];
        int end;
        if (index == (offsets.length-1)) {
            end = path.length();
        } else {
            end = offsets[index+1] - 1;
        }
        // construct result
        return createPath(getFileSystem(), path.substring( begin, end));
    }

    @Override
    public T subpath(int beginIndex, int endIndex) {
        initOffsets();
        String subpath = PathUtils.subpath(path, beginIndex, endIndex, offsets);
        return createPath(getFileSystem(), subpath);
    }


    @Override
    public boolean startsWith(Path other){
        return startsWith(other.toString());
    }

    @Override
    public boolean startsWith(String other){
        return path.startsWith(other);
    }

    @Override
    public boolean endsWith(Path other){
        return endsWith(other.toString());
    }

    @Override
    public boolean endsWith(String other){
        return path.endsWith(other);
    }

    @Override
    public T normalize() {
        initOffsets();
        String normalizedPath = PathUtils.normalize(path, offsets);
        if(path.equals(normalizedPath)){
            return (T) this;
        }
        return createPath(getFileSystem(), normalizedPath);
    }

    @Override
    public T resolve(String obj) {
        return resolve(createPath(getFileSystem(), obj));
    }

    @Override
    public T resolve(Path obj) {
        if(!getClass().isInstance( obj )){
            obj = createPath(getFileSystem(), obj.toString());
        }
        String other = obj.toString();
        if (other.length() > 0 && other.charAt(0) == '/')
            return (T) obj;
        return createPath(getFileSystem(), PathUtils.resolve(path, other));
    }
    
    @Override
    public final AbstractPath resolveSibling(Path other) {
        return resolveSibling(other.toString());
    }

    @Override
    public final AbstractPath resolveSibling(String other) {
        if (other == null)
            throw new NullPointerException();
        Path parent = getParent();
        return (T) ((parent == null) ? other : parent.resolve(other));
    }

    @Override
    public AbstractPath relativize(Path other){
        throw new UnsupportedOperationException();
    }

    @Override
    public URI toUri() {
        String scheme = null;
        String userInfo = null;
        String host = null;
        int port = -1;
        String query = null;
        String fragment = null;
        
        if(getFileSystem() != null && getFileSystem().provider() != null){
            scheme = getFileSystem().provider().getScheme();
        }
        try {
            return new URI(scheme, userInfo, host, port, path, query, fragment);
        } catch(URISyntaxException ex) {
            return null;
        }
    }


    @Override
    public T toAbsolutePath(){
        if(isAbsolute()){
            return normalize();
        }
        return (T) createPath(getFileSystem(), "/").resolve(normalize());
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException{
        return this;
    }

    @Override
    public File toFile(){
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register(WatchService watcher,
            WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException{
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register(WatchService watcher,
            WatchEvent.Kind<?>... events) throws IOException{
        throw new UnsupportedOperationException();
    }
    
    /*
        Code borrowed from sun.nio.fs.AbstractPath, UnixPath
    */
     @Override
     public final Iterator<Path> iterator() {
         return new Iterator<Path>() {
             private int i = 0;
             @Override
             public boolean hasNext() {
                 return (i < getNameCount());
             }
             @Override
             public Path next() {
                 if (i < getNameCount()) {
                     Path result = getName(i);
                     i++;
                     return result;
                 } else {
                     throw new NoSuchElementException();
                 }
             }
             @Override
             public void remove() {
                 throw new UnsupportedOperationException();
             }
         };
     }

    @Override
    public int compareTo(Path obj) {
        return path.compareTo(obj.toString());
    }
       
    // create offset list if not already created
    private void initOffsets() {
        if (offsets == null) {
            int result[] = PathUtils.offsets( path );
            synchronized (this) {
                if (offsets == null)
                    offsets = result;
            }
        }
    }
    
    @Override
    public String toString(){
        return path;
    }
    
    @Override
    public boolean equals(Object obj) {
        if ((obj != null)) {
            return path.equals(obj.toString());
        }
        return false;
    }

    @Override
    public int hashCode(){
        // Use absolute, normalized name for this (extra CPUs, but easier)
        if(hash == 0){
            hash = smearHash(toAbsolutePath().path.hashCode());
        }
        return hash;
    }
    
    private static final int C1 = 0xcc9e2d51;
    private static final int C2 = 0x1b873593;

    /*
     * This was taken from Google's jimfs implementation.
     * 
     * This method was rewritten in Java from an intermediate step of the Murmur hash function in
     * http://code.google.com/p/smhasher/source/browse/trunk/MurmurHash3.cpp, which contained the
     * following header:
     *
     * MurmurHash3 was written by Austin Appleby, and is placed in the public domain. The author
     * hereby disclaims copyright to this source code.
     */
    static int smearHash(int hashCode){
        return C2 * Integer.rotateLeft( hashCode * C1, 15 );
    }

}
