 package org.owasp.webscarab.io;
 
 import java.io.FilterInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 
 /**
  * CopyInputStream writes a copy of everything that is read through it to one or more
  * OutputStreams. This can be used to copy what is read from a SocketInputStream to a SocketOutputStream,
  * while keeping a copy of what was read in a ByteArrayOutputStream, for example.
  * 
  * Any OutputStreams that throw Exceptions when being written to are not written to again. This can be
  * detected by using the constructor that passes an array of OutputStream. By keeping a reference to that array,
  * one can check if any of the OutputStreams have been replaced with null at any point. This is not a big
  * deal in practice, especially in the intended usage scenario of a HTTP proxy, since the OutputStream will
  * be either a ByteArrayOutputStream which never throws Exceptions, or a SocketOutputStream connected to the 
  * browser, which may be closed on user action (cancelling the page view).
  * 
  * @author rogan
  *
  */
 public class CopyInputStream extends FilterInputStream {
 
 	private OutputStream[] copy;
 	
 	public CopyInputStream(InputStream in, OutputStream copy) {
 		this(in, new OutputStream[] { copy });
 	}
 
 	public CopyInputStream(InputStream in, OutputStream[] copy) {
 		super(in);
 		if (copy == null || copy.length == 0)
 			throw new IllegalArgumentException("copy may not be null or empty");
		for (int i=0; i<copy.length; i++)
			if (copy[i] == null)
				throw new IllegalArgumentException("copy may not contain a null element [" + i + "]");
 		this.copy = copy;
 	}
 	
 	/* (non-Javadoc)
 	 * @see java.io.FilterInputStream#markSupported()
 	 */
 	@Override
 	public boolean markSupported() {
 		return false;
 	}
 
 	/* (non-Javadoc)
 	 * @see java.io.FilterInputStream#read()
 	 */
 	@Override
 	public int read() throws IOException {
 		int ret = super.read();
 		if (ret > -1)
 			for (int i=0; i<copy.length; i++) {
 				if (copy[i] == null)
 					continue;
 				try {
 					copy[i].write(ret);
 				} catch (IOException ioe) {
 					copy[i] = null;
 				}
 			}
 		return ret;
 	}
 
 	/* (non-Javadoc)
 	 * @see java.io.FilterInputStream#read(byte[], int, int)
 	 */
 	@Override
 	public int read(byte[] b, int off, int len) throws IOException {
 		int ret = super.read(b, off, len);
 		if (ret > 0)
 			for (int i=0; i<copy.length; i++) {
 				if (copy[i] == null)
 					continue;
 				try {
					copy[i].write(b, 0, ret);
 				} catch (IOException ioe) {
 					copy[i] = null;
 				}
 			}
 		return ret;
 	}
 
 	/* (non-Javadoc)
 	 * @see java.io.FilterInputStream#read(byte[])
 	 */
 	@Override
 	public int read(byte[] b) throws IOException {
 		return this.read(b, 0, b.length);
 	}
 	
 	/**
 	 * a method to read a line from the stream up to and including the CR or CRLF.
 	 * 
 	 * We read character by character so that we don't read further than we
 	 * should i.e. into the next line, which could be a message body, or the next message!
 	 * 
 	 * @param is
 	 *            The InputStream to read the line from
 	 * @throws IOException
 	 *             if an IOException occurs while reading from the supplied
 	 *             InputStream
 	 * @return the line that was read, WITHOUT the CR or CRLF
 	 */
 	public String readLine() throws IOException {
 		StringBuffer line = new StringBuffer();
 		int i;
 		char c = 0x00;
 		i = read();
 		if (i == -1)
 			return null;
 		while (i > -1 && i != 10 && i != 13) {
 			// Convert the int to a char
 			c = (char) (i & 0xFF);
 			line = line.append(c);
 			i = read();
 		}
 		if (i == 13) { // 10 is unix LF, but DOS does 13+10, so read the 10 if
 			// we got 13
 			if ((i = read()) != 10)
 				System.out.println("Unexpected character "
 						+ Integer.toHexString(i) + ". Expected 0x0d");
 		}
 		return line.toString();
 	}
 
 }
