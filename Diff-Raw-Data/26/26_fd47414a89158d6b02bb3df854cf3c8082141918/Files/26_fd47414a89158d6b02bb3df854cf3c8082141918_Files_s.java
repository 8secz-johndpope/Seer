 /**
  * Copyright (c) 2011, SOCIETIES Consortium (WATERFORD INSTITUTE OF TECHNOLOGY (TSSG), HERIOT-WATT UNIVERSITY (HWU), SOLUTA.NET 
  * (SN), GERMAN AEROSPACE CENTRE (Deutsches Zentrum fuer Luft- und Raumfahrt e.V.) (DLR), Zavod za varnostne tehnologije
  * informacijske družbe in elektronsko poslovanje (SETCCE), INSTITUTE OF COMMUNICATION AND COMPUTER SYSTEMS (ICCS), LAKE
  * COMMUNICATIONS (LAKE), INTEL PERFORMANCE LEARNING SOLUTIONS LTD (INTEL), PORTUGAL TELECOM INOVAÇÃO, SA (PTIN), IBM Corp., 
  * INSTITUT TELECOM (ITSUD), AMITEC DIACHYTI EFYIA PLIROFORIKI KAI EPIKINONIES ETERIA PERIORISMENIS EFTHINIS (AMITEC), TELECOM 
  * ITALIA S.p.a.(TI),  TRIALOG (TRIALOG), Stiftelsen SINTEF (SINTEF), NEC EUROPE LTD (NEC))
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
  * conditions are met:
  *
  * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
  *
  * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
  *    disclaimer in the documentation and/or other materials provided with the distribution.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
  * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT 
  * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package org.societies.integration.test.bit.policynegotiate;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 
 /**
  * Class copied from Domain Authority Webapp REST. TODO: merge both and move to internal API.
  *
  * @author Mitja Vardjan
  *
  */
 public class Files {
 	
 	/** Size of buffer */
 	private static final int BUFFER_SIZE = 1024 * 1024;
 	
 	private static byte[] bytes = new byte[BUFFER_SIZE];  
 
 	/**
 	 * Read a file
 	 * 
 	 * @param path File name, including path
 	 * @return contents of the file
 	 * @throws IOException
 	 */
 	public static byte[] getBytesFromFile(String path) throws IOException {
 		
 		File file = new File(path);
 		InputStream is = new FileInputStream(file);
 		
         long length = file.length();
 
         if (length > Integer.MAX_VALUE) {
         	throw new IOException("File is too large " + file.getName());
         }
     
         // Create the byte array to hold the data
         byte[] bytes = new byte[(int)length];
     
         // Read in the bytes
         int offset = 0;
         int numRead = 0;
         while (offset < bytes.length
                && (numRead = is.read(bytes, offset, bytes.length-offset)) >= 0) {
             offset += numRead;
         }
     
         // Ensure all the bytes have been read in
         if (offset < bytes.length) {
             throw new IOException("Could not completely read file " + file.getName());
         }
     
         // Close the input stream and return bytes
         is.close();
         return bytes;
 	}
 	
 	/**
 	 * Write to file
 	 * 
 	 * @param is The source to read file contents from. The content will be written to given file.
 	 * @param path File name, including path
 	 * @throws IOException
 	 */
 	public static void writeFile(InputStream is, String path) throws IOException {
 
 		File file;
 		File directory;
 		
 		file = new File(path);
 		directory = file.getParentFile();
 		
 		if (directory != null) {
 			directory.mkdirs();
 		}
		
 		// Create the byte array to hold the data
 		FileOutputStream os = new FileOutputStream(file);
 
 		// Read in the bytes and write on the fly
 		int numRead = 0;
		long totalRead = 0;
 		while ((bytes.length > 0) && (numRead = is.read(bytes, 0, bytes.length)) >= 0) {
 			os.write(bytes, 0, numRead);
			totalRead += numRead;
 		}
 
 		// Close input and output streams
 		is.close();
 		os.close();
 
 		// Ensure all the bytes have been read in
 //		if (totalRead < bytes.length) {  // WRONG!
 //			throw new IOException("Could not completely read the whole contents");
 //		}
 	}  
 }
