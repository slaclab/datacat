package org.srs.datacat.vfs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author bvan
 */
public class DirectoryStreamWrapper<T> implements DirectoryStream<T> {

    private AutoCloseable closeable;
    private final Iterator iter;
    private final IteratorAcceptor acceptor;
    T next = null;
    
    /**
     * Used to verify entries.
     * @param <U> 
     */
    public static class IteratorAcceptor<U> {
        private DirectoryStreamWrapper<U> thisWrapper;
        public boolean acceptNext() throws IOException { return false; }
        
        public void setNext(U next){
            thisWrapper.next = next;
        }
    }

    public DirectoryStreamWrapper(Iterator iterator, IteratorAcceptor acceptor){
        this.iter = iterator;
        this.acceptor = acceptor;
        this.acceptor.thisWrapper = this;
    }

    public DirectoryStreamWrapper(DirectoryStream dStream, IteratorAcceptor acceptor){
        this.iter = dStream.iterator();
        this.closeable = dStream;
        this.acceptor = acceptor;
        this.acceptor.thisWrapper = this;
    }
        
    @Override
    public Iterator<T> iterator(){
        return new Iterator<T>() {           
           
            @Override
            public boolean hasNext(){
                try {
                    return doAcceptNext();
                } catch(IOException | NoSuchElementException ex) {
                    return false;
                }
            }
            
            public boolean doAcceptNext() throws IOException {
                if(next != null){  // already accepted
                    return true;
                }
                return acceptor.acceptNext();
            }

            @Override
            public T next(){
                try {
                    if(doAcceptNext()){
                        T ret = next;
                        next = null;
                        return ret;
                    }
                } catch(IOException ex) {
                    throw new NoSuchElementException( ex.getMessage() );
                }
                throw new NoSuchElementException( "end of list" );
            }

            @Override
            public void remove(){
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public void close() throws IOException{
        if(closeable != null){
            try {
                closeable.close();
            } catch(Exception ex) {
                throw new IOException("Exception caught when closing resource", ex);
            }
        }
    }

}
