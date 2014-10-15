
package org.srs.datacat.rest;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author bvan
 */
@XmlRootElement(name="resource")
public class RestResource {
    @XmlElement
    public String path;
    
    @XmlElementWrapper
    @XmlElement(name="method")
    public List<RestMethod> methods;
    
    // Default no-arg constructor needed for jaxb
    public RestResource(){}
    
    public RestResource(String path, List<RestMethod> methods) {
            this.path = path;
            this.methods = methods;
        }

}
