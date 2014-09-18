
package org.srs.datacat.model;

import javax.xml.bind.annotation.XmlTransient;


/**
 *
 * @author bvan
 */
public class DatasetView {
    
    @XmlTransient
    public static class VersionId {
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
                case "next":
                    return new VersionId(NEW_VER);
                case "curr":
                case "current":
                    return new VersionId(CURRENT_VER);
                default:
                    return new VersionId(Integer.valueOf(versionId));
            }
        }
    }
        
    public static final int EMPTY_VER = -3;
    public static final int NEW_VER = -2;
    public static final int CURRENT_VER = -1;
    
    public static final String EMPTY_SITES = "$";      // Zero
    public static final String ANY_SITES = "$*";       // Zero or More
    public static final String ALL_SITES = "$+";       // One or More
    public static final String CANONICAL_SITE = "$C";  // One
    
    public static final DatasetView EMPTY = new DatasetView(EMPTY_VER, EMPTY_SITES);
    public static final DatasetView MASTER = new DatasetView(CURRENT_VER, CANONICAL_SITE);
    public static final DatasetView CURRENT_ALL = new DatasetView(CURRENT_VER, ALL_SITES);
    
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
    
    public int getVersionId(){
        return vid;
    }
    
    public boolean isCanonical(){
        return CANONICAL_SITE.equals(site);
    }

    public boolean isCurrent(){
        return vid == CURRENT_VER;
    }
    
    public boolean isAll(){
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
