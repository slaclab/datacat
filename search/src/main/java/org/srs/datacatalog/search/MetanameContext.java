package org.srs.datacatalog.search;

import java.util.List;
import java.util.TreeSet;
import org.srs.datacatalog.search.MetanameContext.Entry;

/**
 * A treeset with prefix elements that can be embedded tree sets.
 * @author bvan
 */
public class MetanameContext extends TreeSet<Entry> {
    
    public static class Entry implements Comparable {
        String metaname;
        TreeSet<Entry> postfixes;
        int splitIdx = 3;

        public Entry(String metaname){
            this.metaname = metaname;
        }
        
        public Entry(String prefix, TreeSet<Entry> postfixes){
            this.metaname = prefix;
            this.postfixes = postfixes;
            this.splitIdx = prefix.length();
        }
        
        public Entry(String prefix, List<String> fullStrings, int splitIdx){
            this.metaname = prefix;
            this.splitIdx = splitIdx < 1 ? prefix.length() : splitIdx;
            this.postfixes = new TreeSet<>();
            for(String s: fullStrings){
                postfixes.add( new Entry(s.substring( splitIdx ) ) );
            }
        }

        @Override
        public int compareTo(Object o){
            String that = o.toString();
            if(postfixes != null && that.startsWith( metaname )){
                Entry cmp = new Entry( that.substring( splitIdx ) );
                Entry maybe = postfixes.floor( cmp );
                return maybe != null ?  maybe.compareTo( cmp ) : -1;
            }
            return metaname.compareTo( o.toString() );
        }
        
        @Override
        public String toString(){
            return this.metaname;
        }
    }

    @Override
    public Entry floor(Entry e){
        Entry maybePrefix = super.floor( e );
        if(maybePrefix != null && maybePrefix.postfixes != null ){
            String pre = maybePrefix.metaname;
            int idx = maybePrefix.splitIdx;
            if(e.metaname.startsWith( pre )){
                Entry epostfix = new Entry(e.metaname.substring( idx ) );
                Entry postfix = maybePrefix.postfixes.floor(epostfix);
                return new Entry(pre + postfix.metaname);
            }
            return new Entry(pre + maybePrefix.postfixes.last().metaname);
        }
        return maybePrefix;
    }

    public Entry floor(String s){
        return floor( new Entry( s ) );
    }

    public boolean contains(Entry e){
        return floor(e) != null ? 
                floor( e ).compareTo( e ) == 0 : 
                false;
    }

    @Override
    public boolean contains(Object o){
        return contains( new Entry( o.toString() ) );
    }
}
