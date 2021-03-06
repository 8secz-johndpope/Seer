 package de.lmu.ifi.dbs.utilities;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Locale;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 public class Arrays2 {
 
     private static final Logger log = Logger.getLogger(Arrays2.class.getName());
 
     private Arrays2() {
     }
 
     public static void abs(byte[] data) {
         for (int i = 0; i < data.length; i++) {
             data[i] = (byte) Math.abs(data[i]);
         }
     }
 
     public static void abs(int[] data) {
         for (int i = 0; i < data.length; i++) {
             data[i] = Math.abs(data[i]);
         }
     }
 
     public static void abs(float[] data) {
         for (int i = 0; i < data.length; i++) {
             data[i] = Math.abs(data[i]);
         }
     }
 
     public static void abs(short[] data) {
         for (int i = 0; i < data.length; i++) {
             data[i] = (short) Math.abs(data[i]);
         }
     }
 
     public static void abs(double[] data) {
         for (int i = 0; i < data.length; i++) {
             data[i] = Math.abs(data[i]);
         }
     }
 
     public static void reverse(Object[] o) {
         int left = 0; // index of leftmost element
         int right = o.length - 1; // index of rightmost element
 
         Object temp;
         while (left < right) {
             temp = o[left];
             o[left] = o[right];
             o[right] = temp;
 
             left++;
             right--;
         }
     }
 
     public static void reverse(double[] o) {
         int left = 0; // index of leftmost element
         int right = o.length - 1; // index of rightmost element
 
         double temp;
         while (left < right) {
             temp = o[left];
             o[left] = o[right];
             o[right] = temp;
 
             left++;
             right--;
         }
     }
 
     public static void reverse(byte[] o) {
         int left = 0; // index of leftmost element
         int right = o.length - 1; // index of rightmost element
 
         byte temp;
         while (left < right) {
             temp = o[left];
             o[left] = o[right];
             o[right] = temp;
 
             left++;
             right--;
         }
     }
 
     public static void reverse(int[] o) {
         int left = 0; // index of leftmost element
         int right = o.length - 1; // index of rightmost element
 
         int temp;
         while (left < right) {
             temp = o[left];
             o[left] = o[right];
             o[right] = temp;
 
             left++;
             right--;
         }
     }
 
     public static void reverse(float[] o) {
         int left = 0; // index of leftmost element
         int right = o.length - 1; // index of rightmost element
 
         float temp;
         while (left < right) {
             temp = o[left];
             o[left] = o[right];
             o[right] = temp;
 
             left++;
             right--;
         }
     }
 
     /**
      * Returns a new Array with unique elements from the source array. The length of the returned array is in [0;
      * arr.length]
      *
      * @param arr
      * @return array with unique elements from arr
      */
     @SuppressWarnings("unchecked")
     public static Object[] unique(Object[] arr) {
         ArrayList list = new ArrayList(arr.length);
         for (Object elem : arr) {
             if (!list.contains(elem)) {
                 list.add(elem);
             }
         }
 
         return list.toArray();
     }
 
     /**
      * Returns a new array with unique elements from the source array. The length of the returned array is in [0;
      * arr.length]
      *
      * @param arr
      * @return array with unique elements from arr
      */
     @SuppressWarnings("unchecked")
     public static double[] unique(double[] arr) {
         double[] uniques = arr.clone();
         Arrays.sort(uniques);
         int last = arr.length;
         for (int i = 0; i + 1 < last; i++) {
             if (uniques[i] == uniques[i + 1]) {
                 last--;
                 System.arraycopy(uniques, i + 1, uniques, i, last - i);
                 i--; // recheck this position
             }
         }
         return Arrays.copyOf(uniques, last);
     }
 
     public static int[] unique(int[] arr) {
         int[] uniques = arr.clone();
         Arrays.sort(uniques);
         int last = arr.length;
         for (int i = 0; i + 1 < last; i++) {
             if (uniques[i] == uniques[i + 1]) {
                 last--;
                 System.arraycopy(uniques, i + 1, uniques, i, last - i);
                 i--; // recheck this position
             }
         }
         return Arrays.copyOf(uniques, last);
     }
 
     /**
      * Returns a new Array with unique elements from the source array. The length of the returned array is in [0;
      * arr.length].
      *
      * @param arr
      * @param accuracy defines the allowed distance up to which 2 doubles are equal
      * @return array with unique elements from arr
      */
     @SuppressWarnings("unchecked")
     public static double[] unique(double[] arr, double accuracy) {
         if (accuracy < 0) {
             throw new IllegalArgumentException("accuracy must be >= 0 but was "
                     + accuracy);
         }
 
         double[] uniques = arr.clone();
         Arrays.sort(uniques);
        int length = arr.length;
        for (int i = 0; i + 1 < length; i++) {
             if (Math.abs(uniques[i] - uniques[i + 1]) < accuracy) {
                length--;
                System.arraycopy(uniques, i + 2, uniques, i+1, length - i-1);
                 i--; // recheck this position
             }
         }
        return Arrays.copyOf(uniques, length);
     }
 
     /**
      * join array of objects by using a glue string and the Object's toString methods.
      *
      * @param arr
      * @param glue
      * @return string of joined toString values
      */
     public static String join(Object[] arr, String glue) {
         StringBuilder buf = new StringBuilder(arr.length * 2);
         for (int i = 0; i < arr.length; i++) {
             buf.append(arr[i]);
             if (i < arr.length - 1) {
                 buf.append(glue);
             }
         }
         return buf.toString();
     }
 
     public static String join(Object[] arr, Character glue) {
         return join(arr, glue.toString());
     }
 
     public static String join(long[] arr, String glue) {
         StringBuilder buf = new StringBuilder(arr.length * 5);
         for (int i = 0; i < arr.length; i++) {
             buf.append(arr[i]);
             if (i < arr.length - 1) {
                 buf.append(glue);
             }
         }
         return buf.toString();
     }
 
     public static String join(long[] arr, Character glue) {
         return join(arr, glue.toString());
     }
 
     /**
      * return first index of Object o in array a or -1 if the object was not found
      *
      * @param src source array
      * @param o object to search
      * @return index of the object [0, a.length] or -1 if not found
      */
     public static int indexOf(Object[] src, Object o) {
         if (src.length == 0) {
             return -1;
         }
         for (int i = 0; i < src.length; i++) {
             if (o == null && src[i] == null) {
                 return i;
             } else if (o.equals(src[i])) {
                 return i;
             }
         }
 
         return -1;
     }
 
     /**
      * return first index of integer o in array a or -1 if the integer was not found
      *
      * @param src source array
      * @param o object to search
      * @return index of the object [0, a.length] or -1 if not found
      */
     public static int indexOf(int[] src, int o) {
         for (int i = 0; i < src.length; i++) {
             if (src[i] == o) {
                 return i;
             }
         }
 
         return -1;
     }
 
     /**
      * @see #join(java.lang.Object[], java.lang.String)
      * @param data
      * @param glue
      * @return A string containing the objects of <code>data</code> separated by <code>glue</code>
      */
     public static String join(byte[] data, String glue) {
         StringBuilder sb = new StringBuilder(data.length * 2);
         for (int i = 0; i < data.length; i++) {
             sb.append(data[i]);
             if (i < data.length - 1) {
                 sb.append(glue);
             }
         }
         return sb.toString();
     }
 
     /**
      * @see #join(java.lang.Object[], java.lang.String)
      * @param data
      * @param glue
      * @return A string containing the objects of <code>data</code> separated by <code>glue</code>
      */
     public static String join(int[] data, String glue) {
         StringBuilder sb = new StringBuilder(data.length * 2);
         for (int i = 0; i < data.length; i++) {
             sb.append(data[i]);
             if (i < data.length - 1) {
                 sb.append(glue);
             }
         }
         return sb.toString();
     }
 
     public static String join(int[] data, Character glue) {
         return join(data, glue.toString());
     }
 
     /**
      * @see #join(java.lang.Object[], java.lang.String)
      * @param data
      * @param glue
      * @return A string containing the objects of <code>data</code> separated by <code>glue</code>, rounded to the
      * <code>dec</code><sup>th</sup> decimale place
      */
     public static String join(float[] data, String glue, int dec) {
         StringBuilder sb = new StringBuilder(data.length * 2);
         for (int i = 0; i < data.length; i++) {
             sb.append(String.format("%." + dec + "f", data[i]));
             if (i < data.length - 1) {
                 sb.append(glue);
             }
         }
         return sb.toString();
     }
 
     /**
      * @see #join(java.lang.Object[], java.lang.String)
      * @param data
      * @param glue
      * @return A string containing the objects of <code>data</code> separated by <code>glue</code>, rounded to the
      * <code>dec</code><sup>th</sup> decimale place
      */
     public static String join(float[] data, String glue) {
         StringBuilder sb = new StringBuilder(data.length * 2);
         for (int i = 0; i < data.length; i++) {
             sb.append(data[i]);
             if (i < data.length - 1) {
                 sb.append(glue);
             }
         }
         return sb.toString();
     }
 
     /**
      * @see #join(java.lang.Object[], java.lang.String)
      * @param data
      * @param glue
      * @return A string containing the objects of <code>data</code> separated by <code>glue</code>
      */
     public static String join(double[] data, String glue) {
         StringBuilder sb = new StringBuilder(data.length * 2);
         for (int i = 0; i < data.length; i++) {
             sb.append(data[i]);
             if (i < data.length - 1) {
                 sb.append(glue);
             }
         }
         return sb.toString();
     }
 
     /**
      * Join array of objects by using a glue string and the Object's toString methods.
      *
      * @param data
      * @param glue
      * @param formatString which formats a single element of the array
      * @param locale Locale setting, may be null (defaults to US then)
      * @return string of joined toString values
      */
     public static String join(double[] data, String glue, String formatString,
             Locale locale) {
         if (locale == null) {
             locale = Locale.US;
         }
         StringBuilder buf = new StringBuilder(data.length * 2);
         for (int i = 0; i < data.length; i++) {
             buf.append(String.format(locale, formatString, data[i]));
             if (i < data.length - 1) {
                 buf.append(glue);
             }
         }
         return buf.toString();
     }
 
     public static String join(double[] data, String glue, String formatString) {
         return join(data, glue, formatString, Locale.getDefault());
     }
 
     /**
      * @param data
      * @return squared length of array
      * @todo Remove as it is not really a thin of arrays but of vectors
      * @deprecated as the semantic is not clear: vector length, componentwise sum, etc
      */
     public static double lengthSquared(double[] data) {
         double l = 0;
         for (double d : data) {
             l += d * d;
         }
         return l;
     }
 
     /**
      * @see #lengthSquared(double[])
      * @param data
      * @return squarerooted length
      * @todo Remove as it is not really a thin of arrays but of vectors
      * @deprecated as the semantic is not clear: vector length, componentwise sum, etc
      */
     public static double length(double[] data) {
         return Math.sqrt(lengthSquared(data));
     }
 
     /**
      * multilpy array with factor
      *
      * @param data source array
      * @param factor
      */
     public static void mul(double[] data, double factor) {
         if (Double.isNaN(factor) && log.isLoggable(Level.WARNING)) {
             log.warning("multiplying by NaN.");
         }
         for (int i = 0; i < data.length; i++) {
             data[i] *= factor;
         }
     }
 
     /**
      * multilpy array with factors from other array: data[i] *= factors[i]
      *
      * @param data source array
      * @param factors
      */
     public static void mul(double[] data, double[] factors) {
         if (data.length != factors.length) {
             throw new IllegalArgumentException("different lengths "
                     + data.length + ":" + factors.length);
         }
         for (int i = 0; i < data.length; i++) {
             double factor = factors[i];
             if (Double.isNaN(factor) && log.isLoggable(Level.WARNING)) {
                 log.warning("multiplying by NaN.");
             }
             data[i] *= factor;
         }
     }
 
     /**
      * multilpy array with factors from other array: data[i] *= factors[i]
      *
      * @param data source array
      * @param factor
      */
     public static void mul(float[] data, float factor) {
         for (int i = 0; i < data.length; i++) {
             if (log.isLoggable(Level.WARNING) && Float.isNaN(factor)) {
                 log.warning("multiplying by NaN.");
             }
             data[i] *= factor;
         }
     }
 
     /**
      * multilpy array with factors from other array: data[i] *= factors[i]
      *
      * @param data source array
      * @param factors
      */
     public static void mul(float[] data, float[] factors) {
         if (data.length != factors.length) {
             throw new IllegalArgumentException("different lengths "
                     + data.length + ":" + factors.length);
         }
         for (int i = 0; i < data.length; i++) {
             float factor = factors[i];
             if (log.isLoggable(Level.WARNING) && Float.isNaN(factor)) {
                 log.warning("multiplying by NaN.");
             }
             data[i] *= factor;
         }
     }
 
     /**
      * divide an int array by a given value and write the result into a double array: target[i] = 1d * data[i] / factor;
      *
      * @param data source array
      * @param factor
      * @param target the target array (may be null)
      */
     public static double[] div(int[] data, double factor, double[] target) {
         if (factor == 0 || Double.isNaN(factor)) {
             log.log(Level.WARNING, "Attempting to divide an array by zero or a NaN: "
                     + factor);
             throw new ArithmeticException("upcoming division by zero or div by NaN. Factor: "
                     + factor);
         }
         if (target != null) {
             if (target.length != data.length) { // check length
                 throw new IllegalArgumentException("arrays with different length! "
                         + data.length + " <> " + target.length);
             }
         } else {
             target = new double[data.length];
         }
         for (int i = 0; i < data.length; i++) {
             target[i] = data[i] / factor;
         }
         return target;
     }
 
     /**
      * divide array by a given value: data[i] /= factor
      *
      * @param data source array
      * @param factor
      */
     public static void div(float[] data, float factor) {
         mul(data, 1 / factor);
     }
 
     /**
      * divide array with values from other array: data[i] /= factors[i]
      *
      * @param data source array
      * @param factors
      */
     public static void div(float[] data, float[] factors) {
         if (data.length != factors.length) {
             throw new IllegalArgumentException("different lengths "
                     + data.length + ":" + factors.length);
         }
         for (int i = 0; i < data.length; i++) {
             if (Float.isNaN(factors[i]) && log.isLoggable(Level.WARNING)) {
                 log.warning("div by NaN.");
             }
             data[i] /= factors[i];
         }
     }
 
     /**
      * divide array by a given value: data[i] /= factor
      *
      * @param data source array
      * @param factor
      */
     public static void div(double[] data, double factor) {
         mul(data, 1 / factor);
     }
 
     /**
      * divide array with values from other array: data[i] /= factors[i]
      *
      * @param data source array
      * @param factors
      */
     public static void div(double[] data, double[] factors) {
         if (data.length != factors.length) {
             throw new IllegalArgumentException("different lengths "
                     + data.length + ":" + factors.length);
         }
         for (int i = 0; i < data.length; i++) {
             if (Double.isNaN(factors[i]) && log.isLoggable(Level.WARNING)) {
                 log.warning("div by NaN.");
             }
             data[i] /= factors[i];
         }
     }
 
     /**
      * adds b to a: a[i] += b[i]
      *
      * Values of b won't be changed.
      *
      * @param a
      * @param b
      */
     public static void add(byte[] a, byte[] b) {
         for (int i = 0; i < b.length; i++) {
             a[i] += b[i];
         }
     }
 
     /**
      * adds b to a: a[i] += b[i]
      *
      * Values of b won't be changed
      *
      * @param a
      * @param b
      */
     public static void add(int[] a, int[] b) {
         for (int i = 0; i < b.length; i++) {
             a[i] += b[i];
         }
     }
 
     /**
      * adds b to a: a[i] += b[i]
      *
      * Values of b won't be changed
      *
      * @param a
      * @param b
      */
     public static void add(float[] a, float[] b) {
         for (int i = 0; i < b.length; i++) {
             a[i] += b[i];
         }
     }
 
     /**
      * adds b to a: a[i] += b[i]
      *
      * Values of b won't be changed
      *
      * @param a
      * @param b
      */
     public static void add(double[] a, double[] b) {
         for (int i = 0; i < b.length; i++) {
             a[i] += b[i];
         }
     }
 
     /**
      * adds b to a: a[i] += b
      *
      * @param a
      * @param b
      */
     public static void add(double[] a, double b) {
         for (int i = 0; i < a.length; i++) {
             a[i] += b;
         }
     }
 
     public static void add(byte[] a, byte b) {
         for (int i = 0; i < a.length; i++) {
             a[i] += b;
         }
     }
 
     public static void add(int[] a, double b) {
         for (int i = 0; i < a.length; i++) {
             a[i] += b;
         }
     }
 
     public static void add(float[] a, double b) {
         for (int i = 0; i < a.length; i++) {
             a[i] += b;
         }
     }
 
     /**
      * Returns the sum of all elements in this array
      *
      * @param a
      * @return sum of all a_i
      */
     public static double sum(double[] a) {
         double sum = 0;
         for (double i : a) {
             sum += i;
         }
         return sum;
     }
 
     /**
      * Returns the sum of all elements in this array
      *
      * @param a
      * @return sum of all a_i
      */
     public static double sum(int[] a) {
         int sum = 0;
         for (double i : a) {
             sum += i;
         }
         return sum;
     }
 
     /**
      * Returns the sum of all elements in this array
      *
      * @param a
      * @return sum of all a_i
      */
     public static float sum(float[] a) {
         float sum = 0;
         for (double i : a) {
             sum += i;
         }
         return sum;
     }
 
     /**
      * Returns the sum of all elements in this array
      *
      * @param a
      * @return sum of all a_i
      */
     public static int sum(byte[] a) {
         int sum = 0;
         for (double i : a) {
             sum += i;
         }
         return sum;
     }
 
     /**
      * Returns the sum of a subarray
      *
      * @param arr the input array
      * @param a start index inclusive
      * @param b end index inclusive
      * @return sum of the elements between [a,b]
      */
     public static double sum(double[] arr, int a, int b) {
         double sum = 0;
         for (int i = a; i <= b; i++) {
             sum += arr[i];
         }
         return sum;
     }
 
     /**
      * Returns the sum of a subarray
      *
      * @param arr the input array
      * @param a start index inclusive
      * @param b end index inclusive
      * @return sum of the elements between [a,b]
      */
     public static double sum(int[] arr, int a, int b) {
         double sum = 0;
         for (int i = a; i <= b; i++) {
             sum += arr[i];
         }
         return sum;
     }
 
     /**
      * Returns the sum of a subarray
      *
      * @param arr the input array
      * @param a start index inclusive
      * @param b end index inclusive
      * @return sum of the elements between [a,b]
      */
     public static double sum(float[] arr, int a, int b) {
         float sum = 0;
         for (int i = a; i <= b; i++) {
             sum += arr[i];
         }
         return sum;
     }
 
     /**
      * Delegates to {@link #add(double[], double)}
      *
      * @param a
      * @param b
      * @Deprecated since April 2010
      * @see #add(double[], double[])
      */
     @Deprecated
     public static void sum(double[] a, double[] b) {
         add(a, b);
     }
 
     public static void sub(int[] a, double b) {
         add(a, -b);
     }
 
     public static void sub(double[] a, double b) {
         add(a, -b);
     }
 
     public static void sub(float[] a, float b) {
         add(a, -b);
     }
 
     /**
      * Performs a[i] = a[i]-b[i]
      *
      * @param a
      * @param b
      */
     public static void sub(byte[] a, byte[] b) {
         for (int i = 0; i < a.length; i++) {
             a[i] = (byte) (a[i] - b[i]);
         }
     }
 
     /**
      * Performs a[i] = a[i]-b[i]
      *
      * @param a
      * @param b
      */
     public static void sub(double[] a, double[] b) {
         for (int i = 0; i < a.length; i++) {
             a[i] = a[i] - b[i];
         }
     }
 
     /**
      * Performs a[i] = a[i]-b[i]
      *
      * @param a
      * @param b
      */
     public static void sub(float[] a, float[] b) {
         for (int i = 0; i < a.length; i++) {
             a[i] = a[i] - b[i];
         }
     }
 
     /**
      * Performs out[i] = a[i]-b[i]
      *
      * @param a
      * @param b
      * @param out containing a-b
      * @return out
      */
     public static double[] sub(double[] a, double[] b, double[] out) {
         if (out == null) {
             out = new double[a.length];
         }
         for (int i = 0; i < a.length; i++) {
             out[i] = a[i] - b[i];
         }
         return out;
     }
 
     /**
      * Performs out[i] = a[i]-b[i]
      *
      * @param a
      * @param b
      * @param out containing a-b
      * @return out
      */
     public static float[] sub(float[] a, float[] b, float[] out) {
         if (out == null) {
             out = new float[a.length];
         }
         for (int i = 0; i < a.length; i++) {
             out[i] = a[i] - b[i];
         }
         return out;
     }
 
     /**
      * Normalizes the values of an array by mutliplying each value with 1/length(array). Length is the euclidean length.
      *
      * BE AWARE that the euclidean length is the dot product with itself!
      *
      * @param data normalized array or array of NaNs if length was zero
      * @see #length(double[])
      * @see #mul(double[], double)
      * @todo Remove as it is not really a thin of arrays but of vectors
      * @deprecated normalize according to what?
      */
     public static void normalize(double[] data) {
         double length = length(data);
         if (length == 0) {
             log.log(Level.WARNING, "Attempting to normalize an array with zero length: {0}", join(data, " ", "%.5f"));
             throw new ArithmeticException("upcoming division by zero");
         }
         div(data, length);
     }
 
     /**
      * computes the dot product
      *
      * @param o1
      * @param o2
      * @return dot product
      * @throws IllegalArgumentException if o1 and o2 have different dimensions
      * @todo Remove as it is not really a thin of arrays but of vectors
      * @deprecated
      * @see Vectors#dot(double[], double[])
      */
     public static double dot(double[] o1, double[] o2) {
         return Vectors.dot(o1, o2);
     }
 
     /**
      * convolve an array with the specified kernel and write output into out
      *
      * @param in input array which will NOT be changed
      * @param out output array with convolved values
      * @param kernel mask (odd size!)
      */
     public static int[] convolve(int[] in, int[] out, double[] kernel) {
         if (out == null) {
             out = new int[in.length];
         }
         if (in == out) {
             throw new IllegalArgumentException("input array must not be output array");
         }
         assert in.length == out.length : "in.length != out.length";
         assert kernel.length % 2 == 1 : "mask's size must be odd";
 
         // Arrays.fill(out, 0);
         int offset = (int) Math.floor(kernel.length / 2);
         for (int i = 0; i < in.length; i++) {
             double v = 0;
             for (int j = 0; j < kernel.length; j++) {
                 int iSrc = i - offset + j;
                 // avoid OutOfBoundsExceptions
                 if (Math2.isIn(0, iSrc, in.length - 1)) {
                     v += in[iSrc] * kernel[j];
                 }
             }
             out[i] = (int) Math.round(v);
         }
         return out;
     }
 
     /**
      * convolve an array with the specified kernel and write output into out
      *
      * @param in input array which will NOT be changed
      * @param out output array with convolved values (may be null)
      * @param kernel mask (odd size!)
      * @return convolved array (if out was NOT null, out==returned array)
      */
     public static double[] convolve(double[] in, double[] out, double[] kernel) {
         if (out == null) {
             out = new double[in.length];
         }
         if (in == out) {
             throw new IllegalArgumentException("input array must not be output array");
         }
         if (in.length != out.length) {
             throw new IllegalArgumentException("in.length " + in.length
                     + " != out.length " + out.length);
         }
         if (kernel.length % 2 == 0) {
             throw new IllegalArgumentException("kernel size size must be odd but was "
                     + kernel.length);
         }
 
         // Arrays.fill(out, 0);
         int offset = (int) Math.floor(kernel.length / 2);
         for (int i = 0; i < in.length; i++) {
             double v = 0;
             for (int j = 0; j < kernel.length; j++) {
                 int iSrc = i - offset + j;
                 // avoid OutOfBoundsExceptions
                 if (Math2.isIn(0, iSrc, in.length - 1)) {
                     v += in[iSrc] * kernel[j];
                 }
             }
             out[i] = v;
         }
         return out;
     }
 
     /**
      * convolve n array with the specified kernel and write output into out
      *
      * @param in input array which will NOT be changed
      * @param out output array with convolved values
      * @param kernel mask (odd size!)
      */
     public static float[] convolve(float[] in, float[] out, double[] kernel) {
         if (out == null) {
             out = new float[in.length];
         }
         if (in == out) {
             throw new IllegalArgumentException("input array must not be output array");
         }
         assert in.length == out.length : "in.length != out.length";
         assert kernel.length % 2 == 1 : "mask's size must be odd";
 
         // Arrays.fill(out, 0);
         int offset = (int) Math.floor(kernel.length / 2);
         for (int i = 0; i < in.length; i++) {
             double v = 0;
             for (int j = 0; j < kernel.length; j++) {
                 int iSrc = i - offset + j;
                 // avoid OutOfBoundsExceptions
                 if (Math2.isIn(0, iSrc, in.length - 1)) {
                     v += in[iSrc] * kernel[j];
                 }
             }
             out[i] = (float) v;
         }
         return out;
     }
 
     public static double[] append(byte[] a, byte... b) {
         if (a == null) {
             a = new byte[0];
         }
         if (b == null) {
             b = new byte[0];
         }
 
         double c[] = new double[a.length + b.length];
         System.arraycopy(a, 0, c, 0, a.length);
         System.arraycopy(b, 0, c, a.length, b.length);
         return c;
     }
 
     public static double[] append(double[] a, double... b) {
         if (a == null) {
             a = new double[0];
         }
         if (b == null) {
             b = new double[0];
         }
 
         double c[] = new double[a.length + b.length];
         System.arraycopy(a, 0, c, 0, a.length);
         System.arraycopy(b, 0, c, a.length, b.length);
         return c;
     }
 
     public static int[] append(int[] a, int... b) {
         if (a == null) {
             a = new int[0];
         }
         if (b == null) {
             b = new int[0];
         }
 
         int c[] = new int[a.length + b.length];
         System.arraycopy(a, 0, c, 0, a.length);
         System.arraycopy(b, 0, c, a.length, b.length);
         return c;
     }
 
     public static float[] append(float[] a, float... b) {
         if (a == null) {
             a = new float[0];
         }
         if (b == null) {
             b = new float[0];
         }
 
         float c[] = new float[a.length + b.length];
         System.arraycopy(a, 0, c, 0, a.length);
         System.arraycopy(b, 0, c, a.length, b.length);
         return c;
     }
 
     /**
      *
      * @param a
      * @return the statistical mean
      * @deprecated
      * @see Statistics#mean(double[])
      */
     @Deprecated
     public static double average(double[] a) {
         return Statistics.mean(a);
     }
 
     /**
      * Computes the median of an array which is already sorted.
      *
      * @param a A <em>sorted</em> array.
      * @return The median value of the sorted array <code>a</code>.
      * @deprecated Replaced by {@link Statistics#median(double[])}
      * @see Statistics#median(double[])
      */
     @Deprecated
     public static double median(double[] a) {
         return Statistics.median(a);
     }
 
     /**
      *
      * @param a
      * @return
      * @deprecated
      * @see Statistics#var(double[])
      */
     @Deprecated
     public static double var(double[] a) {
         return Statistics.var(a);
     }
 
     /**
      *
      * @param a
      * @return
      * @deprecated
      * @see Statistics#stdev(double[])
      */
     @Deprecated
     public static double stdev(double[] a) {
         return Statistics.stdev(a);
     }
 
     /**
      * Find and return the index with the greatest value. If there are >1 maxima with the same value, only the first
      * index will be returned. NaN entries will be ignored.
      *
      * @param a
      * @return max array index of max value or -1 if array is empty or filled with NaNs
      */
     public static int max(double[] a) {
         double max = -Double.MAX_VALUE;
         int maxIndex = -1;
 
         for (int i = 0; i < a.length; i++) {
             if (a[i] > max) {
                 max = a[i];
                 maxIndex = i;
             }
         }
         return maxIndex;
     }
 
     /**
      * returns the greatest value
      *
      * @param a
      * @return
      */
     public static double maxValue(double... a) {
         if (a.length == 0) {
             throw new IllegalArgumentException("at least one argument must be given");
         }
         return a[max(a)];
     }
 
     /**
      * returns the largest value
      *
      * @param a
      * @return
      */
     public static double maxValue(int... a) {
         if (a.length == 0) {
             throw new IllegalArgumentException("at least one argument must be given");
         }
         return a[max(a)];
     }
 
     /**
      * Find and return the INDEX with the greatest value. If there are >1 maxima with the same value, only the first
      * index will be returned.
      *
      * @param a
      * @return max array index of max value or -1 if array is empty
      */
     public static int max(int[] a) {
         int max = Integer.MIN_VALUE;
         int maxIndex = -1;
 
         for (int i = 0; i < a.length; i++) {
             if (a[i] > max) {
                 max = a[i];
                 maxIndex = i;
             }
         }
         return maxIndex;
     }
 
     /**
      * Find and return the INDEX with the greatest value. If there are >1 maxima with the same value, only the first
      * index will be returned.
      *
      * @param a
      * @return max array index of max value or -1 if array is empty
      */
     public static int max(byte[] a) {
         byte max = Byte.MIN_VALUE;
         int maxIndex = -1;
 
         for (int i = 0; i < a.length; i++) {
             if (a[i] > max) {
                 max = a[i];
                 maxIndex = i;
             }
         }
         return maxIndex;
     }
 
     /**
      * Find and return the index with the greatest value. If there are >1 maxima with the same value, only the first
      * index will be returned. NaN entries will be ignored.
      *
      * @param a
      * @return max array index of max value or -1 if array is empty or filled with NaNs
      */
     public static int max(float[] a) {
         float max = Float.MIN_VALUE;
         int maxIndex = -1;
 
         for (int i = 0; i < a.length; i++) {
             if (a[i] > max) {
                 max = a[i];
                 maxIndex = i;
             }
         }
         return maxIndex;
     }
 
     /**
      * Finds and returns the INDEX with the least value. If there are >1 minima with the same value, only the first
      * index will be returned. NaN entries will be ignored.
      *
      * @param a
      * @return min array index of min value or -1 if array is empty or filled with NaNs
      */
     public static int min(byte[] a) {
         byte min = Byte.MAX_VALUE;
         int minIndex = -1;
 
         for (int i = 0; i < a.length; i++) {
             if (a[i] < min) {
                 min = a[i];
                 minIndex = i;
             }
         }
         return minIndex;
     }
 
     /**
      * Finds and returns the INDEX with the least value. If there are >1 minima with the same value, only the first
      * index will be returned. NaN entries will be ignored.
      *
      * @param a
      * @return min array index of min value or -1 if array is empty or filled with NaNs
      */
     public static int min(double[] a) {
         double min = Double.MAX_VALUE;
         int minIndex = -1;
 
         for (int i = 0; i < a.length; i++) {
             if (a[i] < min) {
                 min = a[i];
                 minIndex = i;
             }
         }
         return minIndex;
     }
 
     /**
      * returns the smallest value
      *
      * @param a
      * @return
      */
     public static double minValue(double... a) {
         if (a.length == 0) {
             throw new IllegalArgumentException("at least one argument must be given");
         }
         return a[min(a)];
     }
 
     /**
      * returns the smallest value
      *
      * @param a
      * @return
      */
     public static double minValue(int... a) {
         if (a.length == 0) {
             throw new IllegalArgumentException("at least one argument must be given");
         }
         return a[min(a)];
     }
 
     /**
      * Finds and returns the index with the least value. If there are >1 minima with the same value, only the first
      * index will be returned. NaN entries will be ignored.
      *
      * @param a
      * @return min array index of min value or -1 if array is empty or filled with NaNs
      */
     public static int min(int[] a) {
         double min = Double.MAX_VALUE;
         int minIndex = -1;
 
         for (int i = 0; i < a.length; i++) {
             if (a[i] < min) {
                 min = a[i];
                 minIndex = i;
             }
         }
         return minIndex;
     }
 
     /**
      * Finds and returns the index with the least value. If there are >1 minima with the same value, only the first
      * index will be returned. NaN entries will be ignored.
      *
      * @param a
      * @return min array index of min value or -1 if array is empty or filled with NaNs
      */
     public static int min(float[] a) {
         double min = Float.MAX_VALUE;
         int minIndex = -1;
 
         for (int i = 0; i < a.length; i++) {
             if (a[i] < min) {
                 min = a[i];
                 minIndex = i;
             }
         }
         return minIndex;
     }
 
     /**
      * replaces all NaNs and Infs with r
      *
      * @param arr
      * @param r
      */
     public static void replaceNaN(double[] arr, double r) {
         for (int i = 0; i < arr.length; i++) {
             if (arr[i] != arr[i]) {
                 arr[i] = r;
             }
         }
     }
 
     /**
      * checks whether an object o is contained in arr
      *
      * @see #indexOf(java.lang.Object[], java.lang.Object)
      * @param arr
      * @param o
      * @return true if o is in arr
      */
     public static boolean contains(Object[] arr, Object o) {
         return indexOf(arr, o) >= 0;
     }
 
     /**
      * Finds the first NaN value in the given array.
      *
      * @param in the array to be checked
      * @return the index of the first Double.NaN or -1 if no NaN was found
      */
     public static int findNaN(double[] in) {
         for (int i = 0; i < in.length; i++) {
             if (Double.isNaN(in[i])) {
                 return i;
             }
         }
         return -1;
     }
 
     //<editor-fold defaultstate="collapsed" desc="convert to double[]">
     public static double[] convertToDouble(byte[] in) {
         double[] dst = new double[in.length];
         for (int i = 0; i < dst.length; i++) {
             dst[i] = in[i];
         }
         return dst;
     }
 
     public static double[] convertToDouble(float[] in) {
         double[] dst = new double[in.length];
         for (int i = 0; i < dst.length; i++) {
             dst[i] = in[i];
         }
         return dst;
     }
 
     public static double[] convertToDouble(int[] in) {
         double[] dst = new double[in.length];
         for (int i = 0; i < dst.length; i++) {
             dst[i] = in[i];
         }
         return dst;
     }
 
     public static double[] convertToDouble(short[] in) {
         double[] dst = new double[in.length];
         for (int i = 0; i < dst.length; i++) {
             dst[i] = in[i];
         }
         return dst;
     }
 
     /**
      * Convert string array to doubles - replaces all non numerics with a NaN and throws a warning message to the logger
      *
      * @param src
      * @return a <code>double[]</code> for each entry in <code>src</code>
      */
     public static double[] convertToDouble(String[] src) {
         return convertToDouble(src, new double[src.length]);
     }
 
     public static double[] convertToDouble(String[] src, double[] dst) {
         if (src.length != dst.length) {
             throw new IllegalArgumentException("src.length != dst.length ");
         }
 
         for (int i = 0; i < dst.length; i++) {
             try {
                 dst[i] = Double.parseDouble(src[i]);
             } catch (NumberFormatException e) {
                 if (log.isLoggable(Level.WARNING)) {
                     log.fine("Non numeric detected in array: '" + src[i]
                             + "' Replacing by NaN.");
                 }
                 dst[i] = Double.NaN;
             }
         }
         return dst;
     }
     //</editor-fold>
 
     //<editor-fold defaultstate="collapsed" desc="convert to float[]">
     public static float[] convertToFloat(byte[] in) {
         float[] dst = new float[in.length];
         for (int i = 0; i < dst.length; i++) {
             dst[i] = in[i];
         }
         return dst;
     }
 
     public static float[] convertToFloat(double[] in) {
         float[] dst = new float[in.length];
         for (int i = 0; i < dst.length; i++) {
             dst[i] = (float) in[i];
         }
         return dst;
     }
 
     public static float[] convertToFloat(int[] in) {
         float[] dst = new float[in.length];
         for (int i = 0; i < dst.length; i++) {
             dst[i] = in[i];
         }
         return dst;
     }
 
     /**
      * Convert string array to doubles - replaces all non numerics with a NaN and throws a warning message to the logger
      *
      * @param src
      * @return a <code>double[]</code> for each entry in <code>src</code>
      */
     public static float[] convertToFloat(String[] src) {
         return convertToFloat(src, new float[src.length]);
     }
 
     public static float[] convertToFloat(String[] src, float[] dst) {
         if (src.length != dst.length) {
             throw new IllegalArgumentException("src.length != dst.length ");
         }
 
         for (int i = 0; i < dst.length; i++) {
             try {
                 dst[i] = Float.parseFloat(src[i]);
             } catch (NumberFormatException e) {
                 if (log.isLoggable(Level.WARNING)) {
                     log.fine("Non numeric detected in array: '" + src[i]
                             + "' Replacing by NaN.");
                 }
                 dst[i] = Float.NaN;
             }
         }
         return dst;
     }
     //</editor-fold>
 
     //<editor-fold defaultstate="collapsed" desc="convert to int[]">
     public static int[] convertToInt(byte[] in) {
         int[] dst = new int[in.length];
         for (int i = 0; i < dst.length; i++) {
             dst[i] = in[i];
         }
         return dst;
     }
 
     public static int[] convertToInt(double[] in) {
         int[] dst = new int[in.length];
         for (int i = 0; i < dst.length; i++) {
             dst[i] = (int) in[i];
         }
         return dst;
     }
 
     public static int[] convertToInt(float[] in) {
         int[] dst = new int[in.length];
         for (int i = 0; i < dst.length; i++) {
             dst[i] = (int) in[i];
         }
         return dst;
     }
 
     /**
      * Convert string array to doubles - replaces all non numerics with a 0 and throws a warning message to the logger
      *
      * @param src
      * @return a <code>double[]</code> for each entry in <code>src</code>
      */
     public static int[] convertToInt(String[] src) {
         return convertToInt(src, new int[src.length]);
     }
 
     public static int[] convertToInt(String[] src, int[] dst) {
         if (src.length != dst.length) {
             throw new IllegalArgumentException("src.length != dst.length ");
         }
 
         for (int i = 0; i < dst.length; i++) {
             try {
                 dst[i] = Integer.parseInt(src[i]);
             } catch (NumberFormatException e) {
                 if (log.isLoggable(Level.WARNING)) {
                     log.fine("Non numeric detected in array: '" + src[i]
                             + "' Replacing by 0.");
                 }
                 dst[i] = 0;
             }
         }
         return dst;
     }
     //</editor-fold>
 }
