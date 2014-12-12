
package org.srs.datacat.model;


/**
 * A hash-able representation of a DatasetView.
 * 
 * @author bvan
 */
public class DatasetView {
    
    /**
     * Helper class for understanding a VersionId.
     */
    public static final class VersionId {
        int id;
        private VersionId(int id){
            this.id = id;
        }
        
        public int getId(){
            return this.id;
        }
        
        public static VersionId valueOf(String versionId){
            if(versionId == null){
                return new VersionId(CURRENT_VER);
            }
            switch(versionId.toLowerCase()){
                case "new":
                case "next":
                    return new VersionId(NEW_VER);
                case "curr":
                case "current":
                    return new VersionId(CURRENT_VER);
                default:
                    return new VersionId(Integer.parseInt(versionId));
            }
        }
    }
        
    public static final int EMPTY_VER = -3;
    public static final int NEW_VER = -2;
    public static final int CURRENT_VER = -1;
    
    public static final String EMPTY_SITES = "$";      // Zero
    public static final String ANY_SITES = "$*";       // Zero or More
    public static final String ALL_SITES = "$+";       // All (Zero or More)
    public static final String CANONICAL_SITE = "$C";  // One
    
    public static final DatasetView EMPTY = new DatasetView(EMPTY_VER, EMPTY_SITES);
    public static final DatasetView MASTER = new DatasetView(CURRENT_VER, CANONICAL_SITE);
    public static final DatasetView CURRENT_ALL = new DatasetView(CURRENT_VER, ALL_SITES);
    public static final DatasetView CURRENT_ANY = new DatasetView(CURRENT_VER, ANY_SITES);
    
    private int vid = CURRENT_VER;
    private String site = CANONICAL_SITE;
    
    private DatasetView(){}
    
    public DatasetView(int versionId, String site){
        if(versionId < EMPTY_VER){
            throw new RuntimeException("Version must be non-negative");
        }
        if(site == null || site.isEmpty()){
            throw new RuntimeException("Invalid site");
        }
        this.site = site;
        this.vid = versionId;
    }
            
    public String getSite(){
        return site;
    }
    
    public boolean zeroSites(){
        return EMPTY_SITES.equals(site);
    }
    
    public boolean zeroOrMoreSites(){
        return ANY_SITES.equals(site);
    }
    
    public int getVersionId(){
        return vid;
    }
    
    public boolean isCanonical(){
        return CANONICAL_SITE.equals(site);
    }

    public boolean isCurrent(){
        return vid == CURRENT_VER;
    }
    
    public boolean allSites(){
        return ALL_SITES.equals(site);
    }

    @Override
    public String toString(){
        return String.format( "%d.%s", vid, site );
    }
    
    @Override
    public int hashCode(){
        return toString().hashCode();
    }

}
