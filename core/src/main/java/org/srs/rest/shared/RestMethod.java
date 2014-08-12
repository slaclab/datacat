    
package org.srs.rest.shared;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author bvan
 */
@XmlRootElement(name="method")
public class RestMethod {
    
    @XmlElement
    public String path;
    
    @XmlElement
    public String returns;
    
    @XmlList
    public List<String> httpMethods;
    
    @XmlList
    @XmlElement(nillable = true) 
    public List<String> queryParams;
    
    @XmlList
    public List<String> produces;
    
    public RestMethod() {}
    
    public RestMethod (List<String> methods, List<String> queryParams,
            String returns, List accepts ){
        this.httpMethods = methods;
        this.queryParams = queryParams;
        this.returns = returns;
        for(int i = 0; i < accepts.size(); i++){
            accepts.set( i, accepts.get( i ).toString() );
        }
        this.produces = accepts;
    }
    
}
