 package com.thinkaurelius.titan.diskstorage.util;
 
 import com.google.common.base.Preconditions;
 
 import java.nio.ByteBuffer;
 import java.util.Arrays;
 import java.util.Comparator;
 
 /**
  * Utility methods for dealing with {@link ByteBuffer}.
  *
  */
 public class ByteBufferUtil {
 
     public static final int longSize = 8;
     public static final int intSize = 4;
 
     public static final ByteBuffer getIntByteBuffer(int id) {
         ByteBuffer buffer = ByteBuffer.allocate(intSize);
         buffer.putInt(id);
         buffer.flip();
         return buffer;
     }
 
     public static final ByteBuffer getLongByteBuffer(long id) {
         ByteBuffer buffer = ByteBuffer.allocate(longSize);
         buffer.putLong(id);
         buffer.flip();
         return buffer;
     }
 
     public static final ByteBuffer getLongByteBuffer(long[] ids) {
         ByteBuffer buffer = ByteBuffer.allocate(longSize * ids.length);
         for (int i = 0; i < ids.length; i++)
             buffer.putLong(ids[i]);
         buffer.flip();
         return buffer;
     }
 
     public static final ByteBuffer nextBiggerBuffer(ByteBuffer buffer) {
         assert buffer.position() == 0;
         int len = buffer.remaining();
         ByteBuffer next = ByteBuffer.allocate(len);
         boolean carry = true;
         for (int i = len - 1; i >= 0; i--) {
             byte b = buffer.get(i);
             if (carry) {
                 b++;
                 if (b != 0) carry = false;
             }
             next.put(i, b);
         }
         Preconditions.checkArgument(!carry, "Buffer overflow");
         next.position(0);
         next.limit(len);
         return next;
     }
 
     public static final ByteBuffer zeroByteBuffer(int len) {
         ByteBuffer res = ByteBuffer.allocate(len);
         for (int i = 0; i < len; i++) res.put((byte) 0);
         res.flip();
         return res;
     }
 
     public static final ByteBuffer oneByteBuffer(int len) {
         ByteBuffer res = ByteBuffer.allocate(len);
         for (int i = 0; i < len; i++) res.put((byte) -1);
         res.flip();
         return res;
     }
 
     /**
      * Compares two {@link java.nio.ByteBuffer}s and checks whether the first ByteBuffer is smaller than the second.
      *
      * @param a First ByteBuffer
      * @param b Second ByteBuffer
      * @return true if the first ByteBuffer is smaller than the second
      */
     public static final boolean isSmallerThan(ByteBuffer a, ByteBuffer b) {
         return compare(a, b)<0;
     }
 
     /**
      * Compares two {@link java.nio.ByteBuffer}s and checks whether the first ByteBuffer is smaller than or equal to the second.
      *
      * @param a First ByteBuffer
      * @param b Second ByteBuffer
      * @return true if the first ByteBuffer is smaller than or equal to the second
      */
     public static final boolean isSmallerOrEqualThan(ByteBuffer a, ByteBuffer b) {
         return compare(a,b)<=0;
     }
 
     /**
      * Compares two {@link java.nio.ByteBuffer}s according to their byte order (and not the byte value).
      * <p/>
      *
      * @param a             First ByteBuffer
      * @param b             Second ByteBuffer
      * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
      */
     public static final int compare(ByteBuffer a, ByteBuffer b) {
         if (a == b) {
             return 0;
         }
         int ia = 0;
         int ib = 0;
         int result;
         while (true) {
            boolean aHasRemaining = ia + a.position() < a.limit();
            boolean bHasRemaining = ib + b.position() < b.limit();
             
             if (!aHasRemaining && bHasRemaining) {
                 result = -1;
                 break;
             } else if (aHasRemaining && bHasRemaining) {
                byte ca = a.get(a.position() + ia), cb = b.get(b.position() + ib);
                 if (ca != cb) {
                     if (ca >= 0 && cb >= 0) {
                         if (ca < cb) {
                             result = -1;
                             break;
                         } else if (ca > cb) {
                             result = 1;
                             break;
                         }
                     } else if (ca < 0 && cb < 0) {
                         if (ca < cb) {
                             result = -1;
                             break;
                         } else if (ca > cb) {
                             result = 1;
                             break;
                         }
                     } else if (ca >= 0 && cb < 0) {
                         result = -1;
                         break;
                     } else {
                         result = 1;
                         break;
                     }
                 }
             } else if (aHasRemaining && !bHasRemaining) {
                 result = 1;
                 break;
             } else { //!aHasRemaining && !bHasRemaining
                 result = 0;
                 break;
             }
             
             ia++;
             ib++;
         }
         return result;
     }
 
     public static final String toBitString(ByteBuffer b, String byteSeparator) {
         StringBuilder s = new StringBuilder();
         while (b.hasRemaining()) {
             byte n = b.get();
             String bn = Integer.toBinaryString(n);
             if (bn.length() > 8) bn = bn.substring(bn.length() - 8);
             else if (bn.length() < 8) {
                 while (bn.length() < 8) bn = "0" + bn;
             }
             s.append(bn).append(byteSeparator);
         }
         b.rewind();
         return s.toString();
     }
 
     public static String bytesToHex(ByteBuffer bytes) {
         final int offset = bytes.position();
         final int size = bytes.remaining();
         final char[] c = new char[size * 2];
         for (int i = 0; i < size; i++) {
             final int bint = bytes.get(i + offset);
             c[i * 2] = Hex.byteToChar[(bint & 0xf0) >> 4];
             c[1 + i * 2] = Hex.byteToChar[bint & 0x0f];
         }
         return Hex.wrapCharArray(c);
     }
 
     public static byte[] getArray(ByteBuffer buffer)
     {
         int length = buffer.remaining();
 
         if (buffer.hasArray()) {
             int boff = buffer.arrayOffset() + buffer.position();
             if (boff == 0 && length == buffer.array().length)
                 return buffer.array();
             else
                 return Arrays.copyOfRange(buffer.array(), boff, boff + length);
         } else {
             // else, DirectByteBuffer.get() is the fastest route
             byte[] bytes = new byte[length];
             buffer.mark();
             buffer.get(bytes);
             buffer.reset();
             return bytes;
         }
     }
 }
