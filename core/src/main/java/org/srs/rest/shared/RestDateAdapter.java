/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.rest.shared;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 *
 * @author bvan
 */
public class RestDateAdapter extends XmlAdapter<String, Timestamp> {
    private final TimeZone tz; // = TimeZone.getTimeZone("America/Denver");
    
    // TODO: Create different default time zones, or eliminate altogether?
    public RestDateAdapter(){
        this.tz = TimeZone.getTimeZone("America/Denver");
    }
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Override
    public String marshal(Timestamp date) throws Exception {
        return dateFormat.format(date);
    }

    @Override
    public Timestamp unmarshal(String dateString) throws Exception {
        XMLGregorianCalendar cal = DatatypeFactory.newInstance().newXMLGregorianCalendar(dateString);
        TimeZone whichTz = cal.getTimezone() == DatatypeConstants.FIELD_UNDEFINED ? tz : null;
        Timestamp ts = new Timestamp(cal.toGregorianCalendar(whichTz,Locale.ENGLISH,null).getTimeInMillis());
        
        if (cal.getFractionalSecond() != null && cal.getFractionalSecond().precision() > 3) {
            ts.setNanos((int) (cal.getFractionalSecond().doubleValue() * 1e9));
        }
        return ts;
    }
    
    public class Reverse extends XmlAdapter<Timestamp, String> {
        private final TimeZone tz = TimeZone.getTimeZone("America/Denver");
        private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        @Override
        public String unmarshal(Timestamp date) throws Exception {
            return dateFormat.format(date);
        }

        @Override
        public Timestamp marshal(String dateString) throws Exception {
            XMLGregorianCalendar cal = DatatypeFactory.newInstance().newXMLGregorianCalendar(dateString);
            TimeZone whichTz = cal.getTimezone() == DatatypeConstants.FIELD_UNDEFINED ? tz : null;
            Timestamp ts = new Timestamp(cal.toGregorianCalendar(whichTz, Locale.ENGLISH, null).getTimeInMillis());

            if (cal.getFractionalSecond() != null && cal.getFractionalSecond().precision() > 3) {
                ts.setNanos((int) (cal.getFractionalSecond().doubleValue() * 1e9));
            }
            return ts;
        }
    }
}