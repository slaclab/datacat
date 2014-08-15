
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
public abstract class AbstractPath<T extends AbstractPath> implements Path {
    
    private final String userName;
    private final AbstractFs fileSystem;
    protected final String path;
    private volatile int[] offsets;
    
    protected AbstractPath(String userName, AbstractFs fileSystem, String path){
        this.path = PathUtils.normalizeSeparators( path );
        this.fileSystem = fileSystem;
        this.userName = userName;
    }
    
    private T createPath(String user, AbstractFs fs, String path){
        return (T) fs.getPathProvider().getPath( user, path );
    }
    
    private T createPath(AbstractFs fs, String path){
        return (T) fs.getPathProvider().getPath(userName, path);
    }
    
    @Override
    public boolean isAbsolute() {
        return (path.length() > 0 && path.charAt(0) == '/');
    }
    
    @Override
    public AbstractFs getFileSystem(){
        return fileSystem;
    }
    
    public String getUserName(){
        return userName;
    }
    
    public T withUser(String user){
        return createPath(user, getFileSystem(), path);
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
        if (other == null)
            throw new NullPointerException();
        Path parent = getParent();
        return (T) ((parent == null) ? other : parent.resolve(other));
    }

    @Override
    public final AbstractPath resolveSibling(String other) {
        return resolveSibling(getFileSystem().getPath(other));
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
        return smearHash(toAbsolutePath().path.hashCode());
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
