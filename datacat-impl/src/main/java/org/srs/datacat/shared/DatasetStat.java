
package org.srs.datacat.shared;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Reported statistics for a given container, like DatasetlogicalFolder or 
 * DatasetGroup.
 * Statistics are calculated and derived, and may take a long time.
 * @author bvan
 */
@JsonTypeName(value="dsStat")
public class DatasetStat extends BasicStat {
    
    private long diskUsageBytes;
    private long eventCount;
    private long runMin;
    private long runMax;
    
    public DatasetStat(){}
    
    public DatasetStat(BasicStat stat){
        super(stat);
    }
    
    public long getDiskUsageBytes() { return this.diskUsageBytes; }
    public long getEventCount() { return this.eventCount; }
    public long getRunMin() { return this.runMin; }
    public long getRunMax() { return this.runMax; }

    public void setDiskUsageBytes(long diskUsageBytes) { this.diskUsageBytes = diskUsageBytes; }
    public void setEventCount(long eventCount) { this.eventCount = eventCount; }
    public void setRunMin(long runMin) { this.runMin = runMin; }
    public void setRunMax(long runMax) { this.runMax = runMax; }

}
