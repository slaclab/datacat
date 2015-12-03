package org.srs.webapps.datacat.tags;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 *
 * @author bvan
 */
public class Util {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US);
    private static final ByteFormat byteFormat = new ByteFormat();

    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static String formatTimestamp(Timestamp timestamp){
        synchronized(byteFormat) {
            return timestamp == null ? "" : dateFormat.format(timestamp);
        }
    }

    public static String formatBytes(long bytes){
        synchronized(byteFormat) {
            return byteFormat.format(bytes);
        }
    }

    public static String formatEvents(long events){
        return String.format("%,d", events);
    }

    public static String getValueType(Object value){
        if(value instanceof Long || value instanceof Integer || value instanceof BigInteger) {
            return "integer";
        }
        if(value instanceof Double || value instanceof Float || value instanceof BigDecimal) {
            return "decimal";
        }
        if(value instanceof Timestamp){
            return "timestamp";
        }
        return "string";
    }
}
