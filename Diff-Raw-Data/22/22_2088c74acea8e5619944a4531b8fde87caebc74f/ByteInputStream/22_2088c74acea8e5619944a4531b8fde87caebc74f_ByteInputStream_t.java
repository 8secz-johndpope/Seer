 package ibis.impl.messagePassing;
 
 import java.io.IOException;
 
 final class ByteInputStream
 	extends java.io.InputStream
 	implements ibis.io.IbisStreamFlags {
 
     private int msgHandle;
     private int msgSize;
     private ReadMessage msg;
 
 
     void setMsgHandle(ReadMessage msg) {
 // manta.runtime.RuntimeSystem.DebugMe(this, msg);
 	this.msg  = msg;
 
 	// Cache msgHandle and msgSize here to save on dereferences later
 	ReadFragment front = msg.fragmentFront;
 	msgHandle = front.msgHandle;
 	msgSize   = front.msgSize;
 
 	if (Ibis.DEBUG) {
 	    System.err.println(Thread.currentThread() + "ByteInputStream.msgHandle := " + Integer.toHexString(msgHandle) + " msgSize := " + msgSize + " msg " + msg);
 	}
     }
 
 
     public int read(byte b[]) throws IOException {
 	return read(b, 0, b.length);
     }
 
     static native boolean getInputStreamMsg(int tags[]);
 
     private native int lockedRead();
 
     public int read() throws IOException {
 	int x = -1;
 	Ibis.myIbis.lock();
 	try {
 	    if (msgSize == 0) {
 		msg.nextFragment();
 	    }
 	    x = lockedRead();
 	    msgSize -= SIZEOF_BYTE;
 	} finally {
 	    Ibis.myIbis.unlock();
 	}
 	return x;
     }
 
    private native long nSkip(long n);
    private native int nAvailable();

    public long skip(long n) {
	Ibis.myIbis.lock();
	try {
	    return nSkip(n);
	} finally {
	    Ibis.myIbis.unlock();
	}
    }

    public int available() {
	Ibis.myIbis.lock();
	try {
	    return nAvailable();
	} finally {
	    Ibis.myIbis.unlock();
	}
    }
 
     native static void resetMsg(int msgHandle);
 
     private native int readBooleanArray(boolean[] array, int off, int len, int msgHandle);
     private native int readByteArray(byte[] array, int off, int len, int msgHandle);
     private native int readCharArray(char[] array, int off, int len, int msgHandle);
     private native int readShortArray(short[] array, int off, int len, int msgHandle);
     private native int readIntArray(int[] array, int off, int len, int msgHandle);
     private native int readLongArray(long[] array, int off, int len, int msgHandle);
     private native int readFloatArray(float[] array, int off, int len, int msgHandle);
     private native int readDoubleArray(double[] array, int off, int len, int msgHandle);
 
     int read(boolean b[], int off, int len) throws IOException {
 	Ibis.myIbis.lock();
 	try {
 	    if (msgSize == 0) {
 		msg.nextFragment();
 	    }
 	    int rd = readBooleanArray(b, off, len, msgHandle);
 	    msgSize -= rd * SIZEOF_BOOLEAN;
 	    if (Ibis.DEBUG && msgSize < 0) {
 		throw new ArrayIndexOutOfBoundsException("read(boolean[]): insufficient data");
 	    }
 	    return rd;
 	} finally {
 	    Ibis.myIbis.unlock();
 	}
     }
 
 
     public int read(byte b[], int off, int len) throws IOException {
 // manta.runtime.RuntimeSystem.DebugMe(msgHandle, this);
 	Ibis.myIbis.lock();
 	try {
 	    if (Ibis.DEBUG) {
 		System.err.println("Now want to read " + len + " bytes, avaible in fragment " + Integer.toHexString(msgHandle) + ": " + msgSize);
 		Thread.dumpStack();
 	    }
 	    if (msgSize == 0) {
 		msg.nextFragment();
 		if (Ibis.DEBUG) {
 		    System.err.println("After nextFragment() want to read " + len + " bytes, avaible in fragment " + Integer.toHexString(msgHandle) + ": " + msgSize);
 		}
 	    }
 	    int rd = readByteArray(b, off, len, msgHandle);
 	    msgSize -= rd * SIZEOF_BYTE;
 	    if (Ibis.DEBUG) {
 		System.err.println("Now msgSize := " + msgSize);
 	    }
 	    if (Ibis.DEBUG && msgSize < 0) {
 		throw new ArrayIndexOutOfBoundsException("read(byte[]): insufficient data");
 	    }
 	    return rd;
 	} finally {
 	    Ibis.myIbis.unlock();
 	}
     }
 
 
     int read(char b[], int off, int len) throws IOException {
 	Ibis.myIbis.lock();
 	try {
 	    if (msgSize == 0) {
 		msg.nextFragment();
 	    }
 	    int rd = readCharArray(b, off, len, msgHandle);
 	    msgSize -= rd * SIZEOF_CHAR;
 	    if (Ibis.DEBUG && msgSize < 0) {
 		throw new ArrayIndexOutOfBoundsException("read(char[]): insufficient data");
 	    }
 	    return rd;
 	} finally {
 	    Ibis.myIbis.unlock();
 	}
     }
 
 
     int read(short b[], int off, int len) throws IOException {
 	Ibis.myIbis.lock();
 	try {
 	    if (msgSize == 0) {
 		msg.nextFragment();
 	    }
 	    int rd = readShortArray(b, off, len, msgHandle);
 	    msgSize -= rd * SIZEOF_SHORT;
 	    if (Ibis.DEBUG && msgSize < 0) {
 		throw new ArrayIndexOutOfBoundsException("read(short[]): insufficient data");
 	    }
 	    return rd;
 	} finally {
 	    Ibis.myIbis.unlock();
 	}
     }
 
 
     int read(int b[], int off, int len) throws IOException {
 	Ibis.myIbis.lock();
 	try {
 	    if (msgSize == 0) {
 		msg.nextFragment();
 	    }
 	    int rd = readIntArray(b, off, len, msgHandle);
 	    msgSize -= rd * SIZEOF_INT;
 	    if (Ibis.DEBUG && msgSize < 0) {
 		throw new ArrayIndexOutOfBoundsException("read(int[]): insufficient data");
 	    }
 	    return rd;
 	} finally {
 	    Ibis.myIbis.unlock();
 	}
     }
 
 
     int read(long b[], int off, int len) throws IOException {
 	Ibis.myIbis.lock();
 	try {
 	    if (msgSize == 0) {
 		msg.nextFragment();
 	    }
 	    int rd = readLongArray(b, off, len, msgHandle);
 	    msgSize -= rd * SIZEOF_LONG;
 	    if (Ibis.DEBUG && msgSize < 0) {
 		throw new ArrayIndexOutOfBoundsException("read(long[]): insufficient data");
 	    }
 	    return rd;
 	} finally {
 	    Ibis.myIbis.unlock();
 	}
     }
 
 
     int read(float b[], int off, int len) throws IOException {
 	Ibis.myIbis.lock();
 	try {
 	    if (msgSize == 0) {
 		msg.nextFragment();
 	    }
 	    int rd = readFloatArray(b, off, len, msgHandle);
 	    msgSize -= rd * SIZEOF_FLOAT;
 	    if (Ibis.DEBUG && msgSize < 0) {
 		throw new ArrayIndexOutOfBoundsException("read(float[]): insufficient data");
 	    }
 	    return rd;
 	} finally {
 	    Ibis.myIbis.unlock();
 	}
     }
 
 
     int read(double b[], int off, int len) throws IOException {
 	Ibis.myIbis.lock();
 	try {
 	    if (msgSize == 0) {
 		msg.nextFragment();
 	    }
 	    int rd = readDoubleArray(b, off, len, msgHandle);
 	    msgSize -= rd * SIZEOF_DOUBLE;
 	    if (Ibis.DEBUG && msgSize < 0) {
 		throw new ArrayIndexOutOfBoundsException("read(double[]): insufficient data");
 	    }
 	    return rd;
 	} finally {
 	    Ibis.myIbis.unlock();
 	}
     }
 
 
 
     public void close() {
 	// Ibis.myIbis.checkLockNotOwned();
     }
 
 
     public synchronized void mark(int readlimit) {
     }
 
 
     public synchronized void reset() throws IOException {
 	throw new IOException("mark/reset not supported");
     }
 
 
     public boolean markSupported() {
 	return false;
     }
 
 }
