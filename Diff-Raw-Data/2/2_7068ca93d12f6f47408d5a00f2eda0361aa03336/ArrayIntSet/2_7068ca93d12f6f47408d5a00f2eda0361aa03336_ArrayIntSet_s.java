 //
 // $Id: ArrayIntSet.java,v 1.15 2003/11/27 02:10:34 ray Exp $
 //
 // samskivert library - useful routines for java programs
 // Copyright (C) 2001 Michael Bayne
 // 
 // This library is free software; you can redistribute it and/or modify it
 // under the terms of the GNU Lesser General Public License as published
 // by the Free Software Foundation; either version 2.1 of the License, or
 // (at your option) any later version.
 //
 // This library is distributed in the hope that it will be useful,
 // but WITHOUT ANY WARRANTY; without even the implied warranty of
 // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 // Lesser General Public License for more details.
 //
 // You should have received a copy of the GNU Lesser General Public
 // License along with this library; if not, write to the Free Software
 // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 
 package com.samskivert.util;
 
 import java.io.Serializable;
 
 import java.util.AbstractSet;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.NoSuchElementException;
 
 /**
  * Provides an {@link IntSet} implementation using a sorted array of
  * integers to maintain the contents of the set.
  */
 public class ArrayIntSet extends AbstractSet
     implements IntSet, Cloneable, Serializable
 {
     /**
      * Construct an ArrayIntSet with the specified starting values.
      */
     public ArrayIntSet (int[] values)
     {
         this(values.length);
         add(values);
     }
 
     /**
      * Construct an ArrayIntSet of the specified initial capacity.
      */
     public ArrayIntSet (int initialCapacity)
     {
         _values = new int[initialCapacity];
     }
 
     /**
      * Constructs an empty set with the default initial capacity.
      */
     public ArrayIntSet ()
     {
         this(16);
     }
 
     // documentation inherited from interface
     public int size ()
     {
         return _size;
     }
 
     /**
      * Returns the element at the specified index. Note that the elements
      * in the set are unordered and could change order after insertion or
      * removal. This method is useful only for accessing elements of a
      * static set (and has the desirable property of allowing access to
      * the values in this set without having to create integer objects).
      */
     public int get (int index)
     {
         return _values[index];
     }
 
     // documentation inherited from interface
     public boolean isEmpty ()
     {
         return _size == 0;
     }
 
     // documentation inherited from interface
     public boolean contains (Object o)
     {
         return contains(((Integer)o).intValue());
     }
 
     // documentation inherited from interface
     public boolean contains (int value)
     {
         return (binarySearch(value) >= 0);
     }
 
     // documentation inherited from interface IntSet
     public Interator interator ()
     {
         return new Interator() {
             public boolean hasNext () {
                 return (_pos < _size);
             }
 
             public int nextInt () {
                 if (_pos == _size) {
                     throw new NoSuchElementException();
                 } else {
                     return _values[_pos++];
                 }
             }
 
             public Object next () {
                 return new Integer(nextInt());
             }
 
             public void remove () {
                 if (_pos == 0) {
                     throw new IllegalStateException();
                 }
                 // does not correctly return IllegalStateException if
                 // remove() is called twice in a row...
                 System.arraycopy(_values, _pos, _values, _pos - 1,
                     _size - _pos);
                 _pos--;
                 _values[--_size] = 0;
             }
 
             protected int _pos;
         };
     }
 
     // documentation inherited from interface
     public Iterator iterator ()
     {
         return interator();
     }
 
     // documentation inherited from interface
     public Object[] toArray ()
     {
         return toArray(new Integer[_size]);
     }
 
     // documentation inherited from interface
     public Object[] toArray (Object[] a)
     {
         for (int i = 0; i < _size; i++) {
             a[i] = new Integer(_values[i]);
         }
         return a;
     }
 
     // documentation inherited from interface
     public int[] toIntArray ()
     {
         int[] values = new int[_size];
         System.arraycopy(_values, 0, values, 0, _size);
         return values;
     }
 
     /**
      * Serializes this int set into an array at the specified offset. The
      * array must be large enough to hold all the integers in our set at
      * the offset specified.
      *
      * @return the array passed in.
      */
     public int[] toIntArray (int[] target, int offset)
     {
         System.arraycopy(_values, 0, target, offset, _size);
         return target;
     }
 
     /**
      * Creates an array of shorts from the contents of this set. Any
      * values outside the range of a short will be truncated by way of a
      * cast.
      */
     public short[] toShortArray ()
     {
         short[] values = new short[_size];
         for (int ii = 0; ii < _size; ii++) {
             values[ii] = (short)_values[ii];
         }
         return values;
     }
 
     // documentation inherited from interface
     public boolean add (Object o)
     {
         return add(((Integer)o).intValue());
     }
 
     // documentation inherited from interface
     public boolean add (int value)
     {
         int index = binarySearch(value);
         if (index >= 0) {
             return false;
         }
 
         // convert the return value into the insertion point
         index += 1;
         index *= -1;
 
         // expand the values array if necessary, leaving room for the
         // newly added element
         int valen = _values.length;
         int[] source = _values;
         if (valen == _size) {
             _values = new int[valen*2];
             System.arraycopy(source, 0, _values, 0, index);
         }
 
         // shift and insert
         if (_size > index) {
             System.arraycopy(source, index, _values, index+1, _size-index);
         }
         _values[index] = value;
 
         // increment our size
         _size += 1;
 
         return true;
     }
 
     /**
      * Add all of the values in the supplied array to the set.
      *
      * @param values elements to be added to this set.
      *
      * @return <tt>true</tt> if this set did not already contain all of
      * the specified elements.
      */
     public boolean add (int[] values)
     {
         boolean modified = false;
         int vlength = values.length;
         for (int i = 0; i < vlength; i++) {
             modified = (add(values[i]) || modified);
         }
         return modified;
     }
 
     // documentation inherited from interface
     public boolean remove (Object o)
     {
         return remove(((Integer)o).intValue());
     }
 
     // documentation inherited from interface
     public boolean remove (int value)
     {
         int index = binarySearch(value);
         if (index >= 0) {
             System.arraycopy(_values, index+1, _values, index, --_size-index);
             _values[_size] = 0;
             return true;
         }
         return false;
     }
 
     /**
      * Removes all values in the supplied array from the set. Any values
      * that are in the array but not in the set are simply ignored.
      *
      * @param values elements to be removed from the set.
      *
      * @return <tt>true</tt> if this set contained any of the specified
      * elements (which will have been removed).
      */
     public boolean remove (int[] values)
     {
         boolean modified = false;
         int vcount = values.length;
         for (int i = 0; i < vcount; i++) {
             modified = (remove(values[i]) || modified);
         }
         return modified;
     }
 
     // documentation inherited from interface
     public boolean containsAll (Collection c)
     {
         if (c instanceof Interable) {
             Interator inter = ((Interable) c).interator();
             while (inter.hasNext()) {
                 if (!contains(inter.nextInt())) {
                     return false;
                 }
             }
             return true;
 
         } else {
             return super.containsAll(c);
         }
     }
 
     // documentation inherited from interface
     public boolean addAll (Collection c)
     {
         if (c instanceof Interable) {
             Interator inter = ((Interable) c).interator();
             boolean modified = false;
             while (inter.hasNext()) {
                if (add(other._values[ii])) {
                     modified = true;
                 }
             }
             return modified;
 
         } else {
             return super.addAll(c);
         }
     }
 
     // documentation inherited from interface
     public boolean retainAll (Collection c)
     {
         if (c instanceof IntSet) {
             IntSet other = (IntSet)c;
             int removals = 0;
 
             // go through our array sliding all elements in the union
             // toward the front; overwriting any elements that were to be
             // removed
             for (int ii = 0; ii < _size; ii++) {
                 if (other.contains(_values[ii])) {
                     if (removals != 0) {
                         _values[ii - removals] = _values[ii];
                     }
                 } else {
                     removals++;
                 }
             }
 
             _size = _size - removals;
             return (removals > 0);
 
         } else {
             return super.retainAll(c);
         }
     }
 
     // documentation inherited from interface
     public void clear ()
     {
         Arrays.fill(_values, 0);
         _size = 0;
     }
 
     // documentation inherited from interface
     public boolean equals (Object o)
     {
         if (o instanceof ArrayIntSet) {
             ArrayIntSet other = (ArrayIntSet)o;
             if (other._size == _size) {
                 for (int ii = 0; ii < _size; ii++) {
                     // we can't use Arrays.equals() because we only want to
                     // compare the first _size values
                     if (_values[ii] != other._values[ii]) {
                         return false;
                     }
                 }
                 return true;
             }
         }
 
         return false;
     }
 
     // documentation inherited from interface
     public int hashCode ()
     {
         int hashCode = 0;
         for (int i = 0; i < _size; i++) {
             hashCode ^= _values[i];
         }
         return hashCode;
     }
 
     // documentation inherited from interface
     public Object clone ()
     {
         try {
             ArrayIntSet nset = (ArrayIntSet)super.clone();
             nset._values = _values.clone();
             return nset;
 
         } catch (CloneNotSupportedException cnse) {
             throw new RuntimeException("Internal clone error.");
         }
     }
 
     /**
      * Returns a string representation of this instance.
      */
     public String toString ()
     {
         return StringUtil.toString(iterator());
     }
 
     /**
      * Performs a binary search on our values array, looking for the
      * specified value. Swiped from <code>java.util.Arrays</code> because
      * those wankers didn't provide a means by which to perform a binary
      * search on a subset of an array.
      */
     protected int binarySearch (int key)
     {
 	int low = 0;
 	int high = _size-1;
 
 	while (low <= high) {
 	    int mid = (low + high) >> 1;
 	    int midVal = _values[mid];
 
 	    if (midVal < key) {
 		low = mid + 1;
 	    } else if (midVal > key) {
 		high = mid - 1;
 	    } else {
 		return mid; // key found
             }
 	}
 
 	return -(low + 1);  // key not found.
     }
 
     /** An array containing the values in this set. */
     protected int[] _values;
 
     /** The number of elements in this set. */
     protected int _size;
 
     /** Change this if the fields or inheritance hierarchy ever changes
      * (which is extremely unlikely). We override this because I'm tired
      * of serialized crap not working depending on whether I compiled with
      * jikes or javac. */
     private static final long serialVersionUID = 1;
 }
