 package org.mobicents.servlet.sip.annotations;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.InputStream;
/**
 * Utils for parwing SipApplication annotation without a classloader
 * 
 * @author Vladimir Ralev
 *
 */
 public class SipApplicationAnnotationUtils {
 	
 	private final static byte[] SIP_APPLICATION_BYTES = "SipApplication".getBytes();
 	private final static byte[] ANNOTATION_BYTES = "annotation".getBytes();
	
 	private static boolean contains(byte[] text, byte[] subtext) {
		if(text.length<subtext.length) return false;
 		for(int q=0; q<text.length-subtext.length; q++) {
 			boolean found = true;
 			for(int w=0; w<subtext.length; w++) {
 				if(text[q+w] != subtext[w]) {
 					found = false; break;
 				}
 			}
 			if(found) return true;
 		}
 		return false;
 	}
 
 	/**
 	 * Determine if there is a sip application in this folder.
 	 * 
 	 * TODO: HACK: FIXME: This method reads raw class file trying to determine if it
 	 * uses the SIpApplication annotation. This seems to be reliable and a lot faster
 	 * than using a classloader, but can be reviewed in the future especially when
 	 * JBoss AS 5.0 is available with the new deployer.
 	 */
 	public static boolean findPackageInfo(File file) {
 		if(file.getName().equals("package-info.class")) {
 			FileInputStream stream = null;
 			try {
 				stream = new FileInputStream (file);
 				if(findPackageInfo(stream)) return true;
 			} catch (Exception e) {}
 			finally {
 				try {
 					stream.close();
 				} catch (IOException e) {
 				}
 			}
 		}
 		if(file.isDirectory()) {
 			for(File subFile:file.listFiles()) {
 				if(findPackageInfo(subFile)) return true;
 			}
 		}
 		return false;
 	}
 
 	/**
 	 * Determine if this stream contains SipApplication annotations
 	 * 
 	 * TODO: HACK: FIXME: This method reads raw class file trying to determine if it
 	 * uses the SIpApplication annotation. This seems to be reliable and a lot faster
 	 * than using a classloader, but can be reviewed in the future especially when
 	 * JBoss AS 5.0 is available with the new deployer.
 	 */
 	public static boolean findPackageInfo(InputStream stream) {
 		try {
 			byte[] rawClassBytes;
 			rawClassBytes = new byte[stream.available()];
 			stream.read(rawClassBytes);
 			boolean one = contains(rawClassBytes, SIP_APPLICATION_BYTES);
 			boolean two = contains(rawClassBytes, ANNOTATION_BYTES);
 			if(one && two) 
 				return true;
 		} catch (Exception e) {}
 		return false;
 	}
 }
