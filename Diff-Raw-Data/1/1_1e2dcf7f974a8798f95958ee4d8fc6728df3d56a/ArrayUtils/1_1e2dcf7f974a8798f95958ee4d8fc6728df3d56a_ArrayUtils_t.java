 package common;
 
 public class ArrayUtils {
     public static Integer[] autoBox(int[] array) {
 	Integer[] converted = new Integer[array.length];
 	for (int index = 0; index < array.length; index++) {
 	    converted[index] = array[index];
 	}
 
 	return converted;
     }
 
     public static <T> String arrayToString(T[] array) {
 	StringBuffer buffer = new StringBuffer();
 	int index = 0;
 	buffer.append("[");
 	buffer.append(array[index].toString());
 	index++;
 	for (; index < array.length; index++) {
 	    buffer.append(", ");
 	    buffer.append(array[index].toString());
 	}
 	buffer.append("]");
 
 	return buffer.toString();
     }
 

     public static void swap(int[] array, int fooIndex, int barIndex) {
 	int temp = array[fooIndex];
 	array[fooIndex] = array[barIndex];
 	array[barIndex] = temp;
     }
 
     public static <T> void swap(T[] array, int fooIndex, int barIndex) {
 	T temp = array[fooIndex];
 	array[fooIndex] = array[barIndex];
 	array[barIndex] = temp;
     }
 }
