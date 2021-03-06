 /*
  *  This file is part of the X10 project (http://x10-lang.org).
  *
  *  This file is licensed to You under the Eclipse Public License (EPL);
  *  You may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *      http://www.opensource.org/licenses/eclipse-1.0.php
  *
  *  (C) Copyright IBM Corporation 2006-2011.
  */
 
 package x10.core;
 
 import x10.rtt.Type;
 import x10.rtt.Types;
 
 /**
  * Represents a boxed UInt value. Boxed representation is used when casting
  * a UInt value into type Any or parameter type T.
  */
 final public class UInt extends x10.core.Struct implements java.lang.Comparable<UInt>,
     x10.lang.Arithmetic<UInt>, x10.lang.Bitwise<UInt>, x10.util.Ordered<UInt>
 {
     private static final long serialVersionUID = 1L;
     
     public static final x10.rtt.RuntimeType<?> $RTT = Types.UINT;
     public x10.rtt.RuntimeType<?> $getRTT() {return $RTT;}
     public x10.rtt.Type<?> $getParam(int i) {return null;}
 
     final int $value;
 
     private UInt(int value) {
         this.$value = value;
     }
 
     public static UInt $box(int value) {
         return new UInt(value);
     }
 
     public static int $unbox(UInt obj) {
         return obj.$value;
     }
     
     // make $box/$unbox idempotent
     public static UInt $box(UInt obj) {
         return obj;
     }
 
     public static int $unbox(int value) {
         return value;
     }
     
     public boolean _struct_equals$O(Object obj) {
         if (obj instanceof UInt && ((UInt) obj).$value == $value)
             return true;
         return false;
     }
     
     @Override
 	public int hashCode() {
     	return $value;
 	}
 
 	@Override
     public java.lang.String toString() {
         if ($value >= 0)
             return java.lang.Integer.toString($value);
         else
             return java.lang.Long.toString((long)$value & 0xFFFFffffL);
     }
 	
 	// implements Comparable<UInt>
     public int compareTo(UInt o) {
         int a = Unsigned.inject($value);
         int b = Unsigned.inject(o.$value);
         if (a > b) return 1;
         else if (a < b) return -1;
         return 0;
     }
     
     // implements Arithmetic<UInt>
     public UInt $plus$G() { return this; }
     public UInt $minus$G() { return UInt.$box(-$value); }
     public UInt $plus(UInt a, Type t) { return UInt.$box($value + a.$value); }
     public UInt $minus(UInt a, Type t) { return UInt.$box($value - a.$value); }
     public UInt $times(UInt a, Type t) { return UInt.$box($value * a.$value); }
    public UInt $over(UInt a, Type t) { return UInt.$box($value / a.$value); }
     
     // implements Bitwise<UInt>
     public UInt $tilde$G() { return UInt.$box(~$value); }
     public UInt $ampersand(UInt a, Type t) { return UInt.$box($value & a.$value); }
     public UInt $bar(UInt a, Type t) { return UInt.$box($value | a.$value); }
     public UInt $caret(UInt a, Type t) { return UInt.$box($value ^ a.$value); }
     public UInt $left$G(final int count) { return UInt.$box($value << count); }
    public UInt $right$G(final int count) { return UInt.$box($value >> count); }
     public UInt $unsigned_right$G(final int count) { return UInt.$box($value >>> count); }        
     
     // implements Ordered<UInt>. Rely on autoboxing of booleans
    public Object $lt(UInt a, Type t) { return $value < a.$value; }
    public Object $gt(UInt a, Type t) { return $value > a.$value; }
    public Object $le(UInt a, Type t) { return $value <= a.$value; }
    public Object $ge(UInt a, Type t) { return $value >= a.$value; }
 }
