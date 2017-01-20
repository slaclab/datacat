
package org.srs.datacat.model;

/**
 * Data Type interface.
 * 
 * A data type is a special user-defined set of metadata which conveys the
 * semantics of a file or a file's contents. It is not a file format, which
 * typically describes the on-disk layout or encodings.
 *
 * @author bvan
 */
public interface DataTypeModel {
    
    String getName();
    String getDescription();

}