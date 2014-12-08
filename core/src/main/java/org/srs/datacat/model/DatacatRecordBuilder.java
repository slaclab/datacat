
package org.srs.datacat.model;

/**
 *
 * @author bvan
 */
public interface DatacatRecordBuilder<U extends DatacatRecordBuilder> {

    DatacatRecord build();

    U path(String val);

    U pk(Long val);

    U type(RecordType dType);
    
}
