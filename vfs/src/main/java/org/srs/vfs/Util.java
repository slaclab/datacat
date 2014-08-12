
package org.srs.vfs;

/**
 *
 * @author bvan
 */
public class Util {
    
    
  private static final int C1 = 0xcc9e2d51;
  private static final int C2 = 0x1b873593;

  /*
   * This method was rewritten in Java from an intermediate step of the Murmur hash function in
   * http://code.google.com/p/smhasher/source/browse/trunk/MurmurHash3.cpp, which contained the
   * following header:
   *
   * MurmurHash3 was written by Austin Appleby, and is placed in the public domain. The author
   * hereby disclaims copyright to this source code.
   */
  static int smearHash(int hashCode) {
    return C2 * Integer.rotateLeft(hashCode * C1, 15);
  }


}
