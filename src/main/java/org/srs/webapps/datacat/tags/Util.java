
package org.srs.webapps.datacat.tags;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import org.srs.datahandling.common.util.pipeline.ByteFormat;

/**
 *
 * @author bvan
 */
public class Util {
   private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss",Locale.US);
   private static final ByteFormat byteFormat = new ByteFormat();
   static
   {
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
   }
   public static String formatTimestamp(Timestamp timestamp)
   {
      synchronized (byteFormat)
      {
         return timestamp == null ? "" : dateFormat.format(timestamp);
      }
   }
   public static String formatBytes(long bytes)
   {
      synchronized (byteFormat)
      {
         return byteFormat.format(bytes);
      }
   }
   public static String formatEvents(long events)
   {
      return String.format("%,d",events);
   }
}
