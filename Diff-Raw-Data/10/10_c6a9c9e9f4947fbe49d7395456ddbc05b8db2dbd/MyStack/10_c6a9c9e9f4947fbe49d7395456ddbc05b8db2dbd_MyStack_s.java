/** A surprisingly lax Stack implementation. */
 public class MyStack<T> {
 	private int ix = 0;
	public final int MAX = 10;
 	private T[] data = (T[])new Object[MAX];
 
 	public void push(T obj) {
 		data[ix++] = obj;
 	}
 
 	public boolean hasNext() {
 		return ix > 0;
 	}
 
 	public boolean hasRoom() {
		return ix < (MAX - 1);
 	}
 
 	public T pop() {
 		if (hasNext()) {
 			return data[--ix];
 		}
 		throw new ArrayIndexOutOfBoundsException(-1);
 	}
 }
