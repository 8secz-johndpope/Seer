 /*
  * Copyright (C) 2003 Central Laboratory of the Research Councils
  *
  *  History:
  *     11-MAR-2003 (Peter W. Draper):
  *       Original version.
  */
 package uk.ac.starlink.splat.util;
 
 /**
  * Provides various sorting and searching facilities used in SPLAT.
  *
  * @author Peter W. Draper
  * @version $Id$
  */
 public final class Sort
 {
     /**
      *  It's a class of static methods.
      */
     private Sort()
     {
         // Do nothing.
     }
 
     /**
      * Sort a list of ranges (that is pairs of coordinates) into
      * increasing order and merge any overlapped ranges into single
      * ranges (do this to make sure that ranges define a monotonic set).
      *
      * @param ranges the ranges that require sorting and merging.
      * @return the sorted and merged set of ranges.
      */
     public static double[] sortAndMerge( double[] ranges )
     {
         //  Short number of ranges expected so nothing clever
         //  required, but we do need to retain the lower-upper bounds
         //  pairing for a proper merge when overlapped, cannot just
         //  sort the array and go with that.
         double[] sorted = (double[]) ranges.clone();
         Sort.insertionSort( sorted );
 
         //  Merge any overlapped ranges into single ranges.
         double[] merged = new double[sorted.length];
         int n = 0;
         merged[0] = sorted[0];
         merged[1] = sorted[1];
         for ( int i = 2; i < merged.length; i+=2 ) {
 
             if ( merged[n+1] > sorted[i] ) {
                 //  Current range end overlaps beginning of next
                 //  range, so copy end of next range to be end of this
                 //  range.
                 merged[n+1] = sorted[i+1];
             } else {
 
                 //  No overlap so just copy range.
                 n+=2;
                 merged[n] = sorted[i];
                 merged[n+1] = sorted[i+1];
             }
         }
         n+=2;
 
         if ( n != sorted.length ) {
 
             //  Had some overlaps so trim off unnecessary end.
             double[] trimmed = new double[n];
             System.arraycopy( merged, 0, trimmed, 0, n );
             merged = trimmed;
         }
         return merged;
     }
 
     /**
      * Sort a double precision array, using an insertion sort.
      * This sort is very fast for small numbers of values and gets a
      * boost from pre-sorted arrays. Insertion sort is also stable
      * (which can be important for maintaining the relationship to
      * to other data).
      */
     public static void insertionSort( double[] a )
     {
         // XXX reimplement as merge sort if fast and stable is needed.
         int i;
         int j;
         double v;
 
         for ( i = 1; i < a.length; i++ ) {
             v = a[i];
             j = i;
             while ( ( j > 0 ) && ( a[j-1] > v ) ) {
                 a[j] = a[j-1];
                 j--;
             }
             a[j] = v;
         }
     }
 
     /**
      * Creates an index that sorts a double precision array. On exit a
      * is sorted. A reordering of associated arrays without the need for
      * additional memory can be performed using the 
     * {@link applySortIndex} methods.
      */
     public static int[] insertionSort2( double[] a )
     {
         int size = a.length;
         int[] remap = new int[a.length];
 
         int i;
         int j;
         double v;
         
         for ( i = 1; i < size; i++ ) {
             v = a[i];
             j = i;
             while ( ( j > 0 ) && ( a[j-1] > v ) ) {
                 remap[j] = j-1;
                 a[j] = a[j-1];
                 j--;
             }
             remap[j] = i;
             a[j] = v;
         }
 
         return remap;
     }
     
     //  XXX use PDA_RINP[x] algorithm sometime to re-order in place.
     public static double[] applySortIndex( double[] a, int[] remap, 
                                            boolean incr )
     {
         int size = a.length;
         double[] newa = new double[size];
         if ( incr ) {
             for ( int j = 0; j < size; j++ ) {
                 newa[j] = a[remap[j]];
             }
         }
         else {
             int i = size - 1 ;
             for ( int j = 0; j < size; j++ ) {
                 newa[j] = a[remap[i--]];
             }
         }
         return newa;
     }
     
 
     /**
      * Insertion sort a double precision array into increasing order. Also
      * sorts an associated array doubles. This sort is very fast for
      * small numbers of values and gets a boost from pre-sorted
      * arrays.
      */
     public static void insertionSort2a( double[] a, double[] ia )
     {
         int i;
         int j;
         double v;
         double iv;
 
         for ( i = 1; i < a.length; i++ ) {
             v = a[i];
             iv = ia[i];
             j = i;
             while ( ( j > 0 ) && ( a[j-1] > v ) ) {
                 a[j] = a[j-1];
                 ia[j] = ia[j-1];
                 j--;
             }
             a[j] = v;
             ia[j] = iv;
         }
     }
 
     /**
      * Insertion sort a double precision array into decreasing order. Also
      * sorts an associated array doubles. This sort is very fast for
      * small numbers of values and gets a boost from pre-sorted
      * arrays.
      */
     public static void insertionSort2d( double[] a, double[] ia )
     {
         int i;
         int j;
         double v;
         double iv;
 
         for ( i = 1; i < a.length; i++ ) {
             v = a[i];
             iv = ia[i];
 
             j = i;
             while ( ( j > 0 ) && ( a[j-1] < v ) ) {
                 a[j] = a[j-1];
                 ia[j] = ia[j-1];
                 j--;
             }
             a[j] = v;
             ia[j] = iv;
         }
     }
 
     /**
      * Sort a double precision array, plus array of associated
      * integers using an insertion sort. This sort is very fast for
      * small numbers of values and gets a boost from pre-sorted
      * arrays.
      */
     public static void insertionSort2( double[] a, int[] ia )
     {
         int i;
         int j;
         double v;
         int iv;
 
         for ( i = 1; i < a.length; i++ ) {
             v = a[i];
             iv = ia[i];
             j = i;
             while ( ( j > 0 ) && ( a[j-1] > v ) ) {
                 a[j] = a[j-1];
                 ia[j] = ia[j-1];
                 j--;
             }
             a[j] = v;
             ia[j] = iv;
         }
     }
 
     //  WARNING: these quicksorts are largely untested... Use the
     //  Arrays versions for single array sorts.
 
     /**
      * Sort a double precision array, plus an array of associated
      * integers using a quicksort. This has n*ln(n) sorting time.
      */
     public static void quicksort2( double a[], int[] ia )
     {
         if ( a.length < 10 ) {
             insertionSort2( a, ia );
             return;
         }
         quicksort2( a, ia, 0, a.length );
     }
     private static void quicksort2( double a[], int[] ia, int r, int l )
     {
         int j;
         double v;
         int i = ( r + l ) / 2;
 
         if ( a[l] > a[i] ) swap( a, ia, l, i ); // Tri-Median Method!
         if ( a[l] > a[r] ) swap( a, ia, l, r );
         if ( a[i] > a[r] ) swap( a, ia, i, r );
         j = r - 1;
         swap( a, ia, i, j );
         i = l;
         v = a[j];
         for( ;; ) {
             while( a[++i] < v );
             while( a[--j] > v );
             if ( j < i ) break;
             swap( a, ia, i, j );
             swap( a, ia, i, r - 1 );
             quicksort2( a, ia, l, j );
             quicksort2( a, ia, i + 1, r );
         }
     }
     private static void swap( double a[], int[] ia, int i, int j )
     {
         double d = a[i];
         a[i] = a[j];
         a[j] = d;
 
         int t = ia[i];
         ia[i] = ia[j];
         ia[j] = t;
     }
 
 
     /**
      * Return two indices of the values in an array that lie above and
      * below a given value. If the value doesn't lie within the range
      * the two indices are returned as the nearest end point. The
      * array of values must be increasing or decreasing
      * monotonically.
      *
      * @param array the array of values to be searched
      * @param value the value to be located
      */
     public static int[] binarySearch( double[] array, double value )
     {
         int bounds[] = new int[2];
         int low = 0;
         int high = array.length - 1;
         boolean increases = ( array[low] < array[high] );
 
         // Check off scale.
         if ( ( increases && value < array[low] ) ||
              ( ! increases && value > array[low] ) ) {
             high = low;
         }
         else if ( ( increases && value > array[high] ) ||
                   ( ! increases && value < array[high] ) ) {
             low = high;
         }
         else {
             //  Use a binary search as values should be sorted to increase
             //  in either direction (wavelength, pixel coordinates etc.).
             int mid = 0;
             if ( increases ) {
                 while ( low < high - 1 ) {
                     mid = ( low + high ) / 2;
                     if ( value < array[mid] ) {
                         high = mid;
                     }
                     else if ( value > array[mid] ) {
                         low = mid;
                     }
                     else {
                         // Exact match.
                         low = high = mid;
                         break;
                     }
                 }
             }
             else {
                 while ( low < high - 1 ) {
                     mid = ( low + high ) / 2;
                     if ( value > array[mid] ) {
                         high = mid;
                     }
                     else if ( value < array[mid] ) {
                         low = mid;
                     }
                     else {
                         // Exact match.
                         low = high = mid;
                         break;
                     }
                 }
             }
         }
         bounds[0] = low;
         bounds[1] = high;
         return bounds;
     }
 }
