/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.srs.datacat.shared.adapters;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Turns a string into/from a timestamp.
 * @author bvan
 */
public class RestDateAdapter {
    private final TimeZone tz; // = TimeZone.getTimeZone("America/Denver");
    
    // TODO: Create different default time zones, or eliminate altogether?
    public RestDateAdapter(){
        this.tz = TimeZone.getTimeZone("America/Denver");
    }
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public String marshal(Timestamp date){
        return dateFormat.format(date);
    }

    public Timestamp unmarshal(String dateString) {
        XMLGregorianCalendar cal;
        if(dateString.matches(".*[+-]\\d\\d\\d\\d$")){
            int sp = dateString.length() - 2;
            dateString = new StringBuilder(dateString.substring(0, sp))
                    .append(":")
                    .append(dateString.substring(sp, dateString.length()))
                    .toString();
        }
        try {
            cal = DatatypeFactory.newInstance().newXMLGregorianCalendar(dateString);
        } catch(DatatypeConfigurationException ex) {
            throw new IllegalArgumentException(ex);
        }
        TimeZone whichTz = cal.getTimezone() == DatatypeConstants.FIELD_UNDEFINED ? tz : null;
        Timestamp ts = new Timestamp(cal.toGregorianCalendar(whichTz, Locale.ENGLISH, null).getTimeInMillis());
        
        if (cal.getFractionalSecond() != null && cal.getFractionalSecond().precision() > 3) {
            ts.setNanos((int) (cal.getFractionalSecond().doubleValue() * 1e9));
        }
        return ts;
    }

}