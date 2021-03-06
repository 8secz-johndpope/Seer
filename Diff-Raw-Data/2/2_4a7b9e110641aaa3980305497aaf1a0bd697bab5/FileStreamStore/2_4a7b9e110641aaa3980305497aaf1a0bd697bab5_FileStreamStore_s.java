 /*
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  *
  */
 package org.kvstore.io;
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.RandomAccessFile;
 import java.nio.ByteBuffer;
 import java.nio.channels.FileChannel;
 
 /**
  * File based Stream Storage
  * This class is NOT Thread-Safe
  *
  * @author Guillermo Grandes / guillermo.grandes[at]gmail.com
  */
 public final class FileStreamStore {
 	private final static short MAGIC = 0x754C;
 	private static final boolean DEBUG = false;
 
 	/**
 	 * File associated to this store
 	 */
 	private File file = null;
 	/**
 	 * Size/Power-of-2 for size of buffers/align
 	 * ^9=512 ^12=4096 ^16=65536
 	 */
 	private final int bits;
 
 	/**
 	 * RamdomAccessFile for Input this store
 	 */
 	private RandomAccessFile rafInput = null;
 	/**
 	 * FileChannel for Input this store
 	 */
 	private FileChannel fcInput = null;
 	/**
 	 * ByteBuffer for Input (internal used)
 	 */
 	private final ByteBuffer bufInput;
 
 	/**
 	 * FileOutputStream for Output this store
 	 */
 	private FileOutputStream osOutput = null;
 	/**
 	 * FileChannel for Output this store
 	 */
 	private FileChannel fcOutput = null;
 	/**
 	 * Current output offset for blocks (commited to disk)
 	 */
 	private long offsetOutputCommited = 0;
 	/**
 	 * Current output offset for blocks (uncommited to disk) 
 	 */
 	private long offsetOutputUncommited = 0;
 	/**
 	 * ByteBuffer for Output (internal used)
 	 */
 	private final ByteBuffer bufOutput;
 	/**
 	 * In Valid State?
 	 */
 	private boolean validState = false;
 
 	/**
 	 * Instantiate FileStreamStore
 	 * @param file name of file to open
 	 * @param size for buffer to reduce context switching (minimal is 512bytes) 
 	 */
 	public FileStreamStore(final String file, final int bufferSize) {
 		this(new File(file), bufferSize);
 	}
 
 	/**
 	 * Instantiate FileStreamStore
 	 * @param file file to open
 	 * @param size for buffer to reduce context switching (minimal is 512bytes) 
 	 */
 	public FileStreamStore(final File file, final int bufferSize) {
 		this.file = file;
		this.bits = ((int)Math.ceil(Math.log(Math.min(bufferSize, 512))/Math.log(2))); // round to power of 2
 		this.bufInput = ByteBuffer.allocate(512); // default HDD sector size
 		this.bufOutput = ByteBuffer.allocate(1 << bits);
 	}
 
 	// ========= Open / Close =========
 
 	/**
 	 * Open file
 	 * @return true if valid state
 	 */
 	public boolean open() {
 		if (isOpen()) {
 			close();
 		}
 		if (DEBUG) System.out.println("open("+file+")");
 		try {
 			osOutput = new FileOutputStream(file, true);
 			fcOutput = osOutput.getChannel();
 			rafInput = new RandomAccessFile(file, "r");
 			fcInput = rafInput.getChannel();
 			offsetOutputUncommited = offsetOutputCommited = fcOutput.position();
 		}
 		catch(Exception e) {
 			e.printStackTrace();
 			try { close(); } catch(Exception ign) {}
 		}
 		validState = isOpen();
 		return validState;
 	}
 
 	/**
 	 * Close file
 	 */
 	public void close() {
 		if (validState) sync();
 		try { fcInput.close(); } catch(Exception ign) {}
 		try { rafInput.close(); } catch(Exception ign) {}
 		try { osOutput.close(); } catch(Exception ign) {}
 		try { fcOutput.close(); } catch(Exception ign) {}
 		rafInput = null;
 		fcInput = null;
 		osOutput = null;
 		fcOutput = null;
 		//
 		validState = false;
 	}
 
 	// ========= Info =========
 
 	/**
 	 * @return true if file is open
 	 */
 	public boolean isOpen() {
 		try { 
 			if ((fcInput != null) && (fcOutput != null)) {
 				return (fcInput.isOpen() && fcOutput.isOpen());
 			}
 		} catch(Exception ign) {}
 		return false;
 	}
 
 	/**
 	 * @return size of file in bytes
 	 * @see #getBlockSize()
 	 */
 	public long size() {
 		try {
 			return file.length();
 		}
 		catch(Exception e) {
 			e.printStackTrace();
 		}
 		return -1;
 	}
 
 	// ========= Destroy =========
 
 	/**
 	 * Truncate file
 	 */
 	public void clear() {
 		if (!validState) throw new InvalidStateException();
 		try {
 			bufOutput.clear();
 			fcOutput.position(0);
 			fcOutput.truncate(0);
 			close();
 			open();
 		}
 		catch(Exception e) {
 			e.printStackTrace();
 		}
 	}
 
 	/**
 	 * Delete file
 	 */
 	public void delete() {
 		bufOutput.clear();
 		close();
 		try { file.delete(); } catch(Exception ign) {}
 	}
 
 	// ========= Operations =========
 
 	/**
 	 * Read block from file
 	 * @param offset of block
 	 * @param ByteBuffer
 	 * @return length of data
 	 */
 	public int read(final long offset, final ByteBuffer buf) {
 		if (!validState) throw new InvalidStateException();
 		try {
 			if (offset >= offsetOutputCommited) {
 				System.out.println("WARN: autosync forced");
 				sync();
 			}
 			fcInput.position(offset);
 			bufInput.clear();
 			final int readed = fcInput.read(bufInput); // Read 1 sector
 			if (readed < 4) { // int 4 bytes
 				return -1;
 			}
 			bufInput.flip();
 			final short magic = bufInput.getShort(); 	// Header - Magic (short, 2 bytes)
 			if (magic != MAGIC) {
 				System.out.println("MAGIC fake=" + Integer.toHexString(magic) + " expected=" + Integer.toHexString(MAGIC));
 				return -1;
 			}
 			final int datalen = bufInput.getInt(); 	// Header - Data Size (int, 4 bytes)
 			buf.put(bufInput);
 			if (datalen > readed) {
 				System.out.println("datalen=" + datalen);
 				buf.limit(datalen);
 				fcInput.read(buf);
 			}
 			return datalen;
 		}
 		catch(Exception e) {
 			e.printStackTrace();
 		}
 		return -1;
 	}
 
 	/**
 	 * Write from bufInput to file
 	 * 
 	 * @param offset of block
 	 * @param buf ByteBuffer to write
 	 * @return long offset where buffer begin was write or -1 if error
 	 */
 	public long write(final ByteBuffer buf) {
 		if (!validState) throw new InvalidStateException();
 		// Sanity check
 		final int packet_size = (2 + 4 + buf.limit()); // short + int + data
 		if (packet_size > (1<<bits)) {
 			System.err.println("ERROR: packet size is greater ("+packet_size+") than file buffer (" + bufOutput.capacity() + ")");
 			return -1L;
 		}
 		try {
 			// Align output
 			final int diffOffset = nextBlockBoundary(offsetOutputUncommited);
 			if (packet_size > diffOffset) {
 				//System.err.println("WARN: aligning offset=" + offsetOutputUncommited + " to=" + (offsetOutputUncommited+diffOffset) + " needed=" + packet_size + " allowed=" + diffOffset);
 				alignBuffer(diffOffset);
 				offsetOutputUncommited += diffOffset;
 			}
 			// Remember current offset
 			final long offset = offsetOutputUncommited;
 			// Write pending buffered data to disk
 			if (bufOutput.remaining() < packet_size) {
 				flushBuffer();
 			}
 			// Write new data to buffer
 			bufOutput.putShort(MAGIC); 		// Header - Magic (short, 2 bytes)
 			bufOutput.putInt(buf.limit()); 	// Header - Data Size (int, 4 bytes)
 			bufOutput.put(buf); 			// Data Body
 			// Increment offset of buffered data (header + user-data)
 			offsetOutputUncommited += packet_size;
 			//
 			return offset;
 		}
 		catch(Exception e) {
 			e.printStackTrace();
 		}
 		return -1L;
 	}
 
 	/**
 	 * How much bytes left to next block boundary
 	 * @param offset
 	 * @return bytes left
 	 */
 	private final int nextBlockBoundary(final long offset) {
 		return (int)((((offset >> bits) + 1) << bits) - offset);
 	}
 
 	/**
 	 * Pad output buffer with NULL to complete alignment
 	 * @param diff bytes
 	 * @throws IOException
 	 */
 	private final void alignBuffer(final int diff) throws IOException {
 		if (bufOutput.remaining() < diff) {
 			flushBuffer();
 		}
 		int i = 0; 
 		for (; i+8 <= diff; i+=8) {
 			bufOutput.putLong(0L);
 		}
 		for (; i+4 <= diff; i+=4) {
 			bufOutput.putInt(0);
 		}
 		switch(diff-i) {
 		case 3: 
 			bufOutput.put((byte)0);
 		case 2: 
 			bufOutput.putShort((short)0);
 			break;
 		case 1: 
 			bufOutput.put((byte)0);
 		}
 	}
 
 	/**
 	 * Write uncommited data to disk
 	 * @throws IOException
 	 */
 	private final void flushBuffer() throws IOException {
 		if (bufOutput.position() > 0) {
 			bufOutput.flip();
 			fcOutput.write(bufOutput);
 			bufOutput.clear();
 		}
 	}
 
 	/**
 	 * Forces any updates to this file to be written to the storage device that contains it.
 	 * @return false if exception occur 
 	 */
 	public boolean sync() {
 		if (!validState) throw new InvalidStateException();
 		try { 
 			flushBuffer();
 			fcOutput.force(false);
 			offsetOutputUncommited = offsetOutputCommited = fcOutput.position();
 			return true;
 		}
 		catch(Exception e) {
 			e.printStackTrace();
 		}
 		return false;
 	}
 
 	// ========= Exceptions =========
 
 	/**
 	 * Exception throwed when store is in invalid state (closed) 
 	 */
 	public static class InvalidStateException extends RuntimeException {
 		private static final long serialVersionUID = 42L;
 	}
 
 	// ========= END =========
 
 }
