
package org.srs.datacat.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Cookie;

/**
 *
 * @author bvan
 */
public class AuthenticationFilter implements ClientRequestFilter {

    private List<Cookie> cookies;
    private Map<String, Object> headers;
    
    static final HashSet<String> ALLOWED_HEADERS = new HashSet<>(Arrays.asList(new String[]{
        "authenticaiton"
    }));
    
    public AuthenticationFilter(){
        
    }
    
    public AuthenticationFilter(HttpServletRequest delegatedRequest){
        if(delegatedRequest != null){
            ArrayList<Cookie> jaxrsCookies = new ArrayList<>();
            if(delegatedRequest.getCookies() != null){
                for(javax.servlet.http.Cookie c: delegatedRequest.getCookies()){
                    Cookie n = new Cookie(c.getName(), c.getValue(), c.getPath(), c.getDomain(), c.getVersion());
                    jaxrsCookies.add(n);
                }
                this.cookies = jaxrsCookies;
            }

            Enumeration e = delegatedRequest.getHeaderNames();
            Map<String, Object> jaxrsHeaders = new HashMap<>();
            while(e.hasMoreElements()){
                String name = e.nextElement().toString();
                if(!ALLOWED_HEADERS.contains(name.toLowerCase())){
                    continue;
                }
                Enumeration values = delegatedRequest.getHeaders(name);
                ArrayList<Object> more = new ArrayList<>();
                Object first = values.nextElement();
                while(values.hasMoreElements()){                
                    more.add(first);
                }
                jaxrsHeaders.put(name, more);
            }
            this.headers = jaxrsHeaders;
        }
    }
    
    public void setCookies(List<Cookie> cookies){
        this.cookies = cookies;
    }
    
    public void setHeaders(Map<String, Object> headers){
        this.headers = headers;
    }
    
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException{
        if(this.headers != null){
            for(Map.Entry<String, Object> e: headers.entrySet()){
                requestContext.getHeaders().add(e.getKey(), e.getValue());
            }
        }
        if(this.cookies != null){
            requestContext.getHeaders().add("Cookie", cookies);
        }
    }

}
