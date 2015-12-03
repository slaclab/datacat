package org.srs.webapps.datacat.tags;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

/**
 * A formatter for formatting byte sizes. For example, formatting 12345 byes results in "12.1 kB"
 * and 1234567 results in "1.18 MB".
 *
 * @author Bill Lynch
 * @author Tony Johnson
 */
public class ByteFormat extends Format {

    private final static String[] mags = {" B", " kB", " MB", " GB", " TB", " PB"};
    private final static DecimalFormat formatter = new DecimalFormat("#,##0.0");

    /**
     * Formats a long which represent a number of bytes.
     */
    public String format(long bytes){
        return format(new Long(bytes));
    }

    /**
     * Format the given object (must be a Long).
     *
     * @param obj assumed to be the number of bytes as a Long.
     * @param buf the StringBuffer to append to.
     * @param pos
     * @return A formatted string representing the given bytes in more human-readable form.
     */
    public StringBuffer format(Object obj, StringBuffer buf, FieldPosition pos){
        if(obj instanceof Long){
            long numBytes = ((Long) obj).longValue();
            if(numBytes > 1024){
                int mag = 1;
                for(; mag < mags.length; mag++){
                    if(numBytes < 1024 * 1024) {
                        break;
                    }
                    numBytes /= 1024;
                }

                buf.append(formatter.format((double) numBytes / 1024.0)).append(mags[mag]);
            } else {
                buf.append(numBytes).append(mags[0]);
            }
        }
        return buf;
    }

    /**
     * In this implementation, returns null always.
     *
     * @param source
     * @param pos
     * @return returns null in this implementation.
     */
    public Object parseObject(String source, ParsePosition pos){
        return null;
    }
}
