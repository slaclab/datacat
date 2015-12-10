package org.srs.datacat.client.auth;

import com.google.common.base.Strings;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author bvan
 */
public class HmacAuthFilter implements ClientRequestFilter {
    private String keyId;
    private byte[] secretKey;
    private String headerName;
    private String signatureFormat;
    private URI baseUrl;

    private static final String HMACSHA1 = "HmacSHA1";

    public HmacAuthFilter(String keyId, String secretKey, String headerName, String signatureFormat,
            URI url) {
        this.keyId = keyId;
        this.secretKey = DatatypeConverter.parseBase64Binary(secretKey);
        this.headerName = headerName;
        this.signatureFormat = signatureFormat;
        this.baseUrl = url;
    }

    private void checkDateHeader(MultivaluedMap<String, Object> headers){
        if(!headers.containsKey("Date") && !headers.containsKey("date")){
            headers.putSingle("Date", new Date(System.currentTimeMillis()));
        }
    }

    protected String requestToString(String method, URI uri, MultivaluedMap<String, Object> headers){
        String path = uri.getPath();
        
        if(baseUrl != null){
            path = path.replace(baseUrl.getPath(), "");
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        StringBuilder fullHeader = new StringBuilder()
                .append(method).append("\n")
                .append(path).append("\n")
                .append(Strings.nullToEmpty((String)headers.getFirst("content-md5"))).append("\n")
                .append(Strings.nullToEmpty((String)headers.getFirst("content-type"))).append("\n")
                .append(dateFormat.format(headers.getFirst("date"))).append("\n");
        System.out.println(fullHeader.toString());
        return fullHeader.toString();
    }

    @Override
    public void filter(ClientRequestContext crc) throws IOException{
        MultivaluedMap<String, Object> headers = crc.getHeaders();
        checkDateHeader(headers);
        doFilter(crc.getMethod(), crc.getUri(), headers);
        System.out.println(crc.getHeaders());
        
    }
        
    public void doFilter(String method, URI uri, MultivaluedMap<String, Object> headers) throws IOException{
        String fullHeader = requestToString(method, uri, headers);

        try {
            Mac hmac = Mac.getInstance(HMACSHA1);
            //Get key from database using clientKeyID
            SecretKey hmacKey = new SecretKeySpec(secretKey, HMACSHA1);
            hmac.init(hmacKey);
            byte[] serverRawDigest = hmac.doFinal(fullHeader.getBytes());
            String digest = DatatypeConverter.printBase64Binary(serverRawDigest);
            String header = MessageFormat.format(signatureFormat, keyId, digest);
            headers.add(headerName, header);
        } catch(NoSuchAlgorithmException | InvalidKeyException | IllegalStateException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public String toString(){
        return "HmacAuthFilter{" + "keyId=" + keyId + ", secretKey=" + secretKey + 
                ", headerName=" + headerName + ", signatureFormat=" + signatureFormat + ", baseUrl=" + baseUrl + '}';
    }

}
