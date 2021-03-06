 /*
  * Copyright (C) 2012 - 2012 NHN Corporation
  * All rights reserved.
  *
  * This file is part of The nGrinder software distribution. Refer to
  * the file LICENSE which is part of The nGrinder distribution for
  * licensing details. The nGrinder distribution is available on the
  * Internet at http://nhnopensource.org/ngrinder
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
  * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
  * COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
  * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
  * OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package org.ngrinder.sm;
 
 import java.io.File;
 import java.io.FileDescriptor;
 import java.io.IOException;
 import java.net.InetAddress;
 import java.security.Permission;
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * nGrinder security manager.
  * 
  * @author JunHo Yoon
  * @author Tobi
  * @since 3.0
  */
 public class NGrinderSecurityManager extends SecurityManager {
 
 	private String workDirectory = System.getProperty("user.dir");
 	private String logDirectory = null;
	
 	private String agentExecDirectory = System.getProperty("ngrinder.exec.path", workDirectory);
 	private String javaHomeDirectory = System.getenv("JAVA_HOME");
 	private String jreHomeDirectory = null;
 	private String javaExtDirectory = System.getProperty("java.ext.dirs");
 	private String etcHosts = System.getProperty("ngrinder.etc.hosts", "");
 	private String consoleIP = System.getProperty("ngrinder.console.ip", "127.0.0.1");
 	private List<String> allowedHost = new ArrayList<String>();
 	private List<String> readAllowedDirectory = new ArrayList<String>();
 	private List<String> writeAllowedDirectory = new ArrayList<String>();
 	private List<String> deleteAllowedDirectory = new ArrayList<String>();
 
 	{
 		this.initAccessOfDirectories();
 		this.initAccessOfHosts();
 	}
 
 	/**
 	 * Set default accessed of directories. <br>
 	 */
 	private void initAccessOfDirectories() {
 		workDirectory = normalize(new File(workDirectory).getAbsolutePath(), null);
 		logDirectory = workDirectory.substring(0, workDirectory.lastIndexOf(File.separator));
 		logDirectory = logDirectory.substring(0, workDirectory.lastIndexOf(File.separator)) + File.separator + "log";
 		agentExecDirectory = normalize(new File(agentExecDirectory).getAbsolutePath(), null);
 		javaHomeDirectory = normalize(new File(javaHomeDirectory).getAbsolutePath(), null);
 		jreHomeDirectory = javaHomeDirectory.substring(0, javaHomeDirectory.lastIndexOf(File.separator))
 						+ File.separator + "jre";
 
 		readAllowedDirectory.add(workDirectory);
 		readAllowedDirectory.add(logDirectory);
 		readAllowedDirectory.add(agentExecDirectory);
 		readAllowedDirectory.add(javaHomeDirectory);
 		readAllowedDirectory.add(jreHomeDirectory);
 		readAllowedDirectory.add(getTempDirectoryPath());
 
 		String[] jed = javaExtDirectory.split(";");
 		for (String je : jed) {
 			je = normalize(new File(je).getAbsolutePath(), null);
 			readAllowedDirectory.add(je);
 		}
 
 		writeAllowedDirectory.add(workDirectory);
 		writeAllowedDirectory.add(logDirectory);
 		writeAllowedDirectory.add(getTempDirectoryPath());
 		deleteAllowedDirectory.add(workDirectory);
 	}
 
 	// -----------------------------------------------------------------------
 	/**
 	 * Returns the path to the system temporary directory.
 	 * 
 	 * @return the path to the system temporary directory.
 	 * 
 	 * @since Commons IO 2.0
 	 */
 	public static String getTempDirectoryPath() {
 		return System.getProperty("java.io.tmpdir");
 	}
 
 	/**
 	 * Get ip address of target hosts. <br>
 	 * if target hosts 'a.com:1.1.1.1' add 'a.com' & '1.1.1.1' <br>
 	 * if target hosts ':1.1.1.1' add : '1.1.1.1' <br>
 	 * if target hosts '1.1.1.1' add : '1.1.1.1' <br>
 	 * <br>
 	 * Add controller host<br>
 	 */
 	private void initAccessOfHosts() {
 		String[] hostsList = etcHosts.split(",");
 		for (String hosts : hostsList) {
 			String[] addresses = hosts.split(":");
 			if (addresses.length > 1) {
 				allowedHost.add(addresses[0]);
 				allowedHost.add(addresses[addresses.length - 1]);
 			} else {
 				allowedHost.add(hosts);
 			}
 		}
 
 		// add controller host
 		allowedHost.add(consoleIP);
 	}
 
 	@Override
 	public void checkPermission(Permission permission) {
 		if (permission instanceof java.lang.RuntimePermission) {
 			// except setSecurityManager
 			String permissionName = permission.getName();
 			if ("setSecurityManager".equals(permissionName)) {
 				throw new SecurityException("java.lang.RuntimePermission: setSecurityManager is not allowed.");
 			}
 		} else if (permission instanceof java.security.UnresolvedPermission) {
 			throw new SecurityException("java.security.UnresolvedPermission is not allowed.");
 		} else if (permission instanceof java.awt.AWTPermission) {
 			throw new SecurityException("java.awt.AWTPermission is not allowed.");
 		} else if (permission instanceof javax.security.auth.AuthPermission) {
 			throw new SecurityException("javax.security.auth.AuthPermission is not allowed.");
 		} else if (permission instanceof javax.security.auth.PrivateCredentialPermission) {
 			throw new SecurityException("javax.security.auth.PrivateCredentialPermission is not allowed.");
 		} else if (permission instanceof javax.security.auth.kerberos.DelegationPermission) {
 			throw new SecurityException("javax.security.auth.kerberos.DelegationPermission is not allowed.");
 		} else if (permission instanceof javax.security.auth.kerberos.ServicePermission) {
 			throw new SecurityException("javax.security.auth.kerberos.ServicePermission is not allowed.");
 		} else if (permission instanceof javax.sound.sampled.AudioPermission) {
 			throw new SecurityException("javax.sound.sampled.AudioPermission is not allowed.");
 		}
 	}
 
 	@Override
 	public void checkPermission(Permission permission, Object context) {
 		this.checkPermission(permission);
 	}
 
 	@Override
 	public void checkRead(String file) {
 		fileAccessReadAllowed(file);
 	}
 
 	@Override
 	public void checkRead(String file, Object context) {
 		fileAccessReadAllowed(file);
 	}
 
 	@Override
 	public void checkRead(FileDescriptor fd) {
 	}
 
 	@Override
 	public void checkWrite(String file) {
 		this.fileAccessWriteAllowed(file);
 
 	}
 
 	public String getCanonicalPath(String file) {
 		try {
 			return new File(file).getCanonicalPath();
 		} catch (IOException e) {
 			return null;
 		}
 	}
 
 	@Override
 	public void checkDelete(String file) {
 		this.fileAccessDeleteAllowed(file);
 	}
 
 	@Override
 	public void checkExec(String cmd) {
 		throw new SecurityException("Cmd execution of " + cmd + " is not allowed.");
 	}
 
 	/**
 	 * File read access is allowed on <br>
 	 * "agent.exec.folder" and "agent.exec.folder".
 	 * 
 	 * @param file
 	 *            file path
 	 */
 	private void fileAccessReadAllowed(String file) {
 		String filePath = normalize(file, workDirectory);
 		for (String dir : readAllowedDirectory) {
 			if (filePath != null && filePath.startsWith(dir)) {
 				return;
 			}
 		}
 		// Dirty Hack
		if (filePath.contains("/WEB-INF/lib/") || filePath.contains("\\WEB-INF\\lib\\") ) {
 			return;
 		}
 		throw new SecurityException("File Read access on " + file + "(" + filePath + ") is not allowed.");
 	}
 
 	/**
 	 * File write access is allowed <br>
 	 * on "agent.exec.folder".
 	 * 
 	 * @param file
 	 *            file path
 	 */
 	private void fileAccessWriteAllowed(String file) {
 		String filePath = normalize(file, workDirectory);
 		for (String dir : writeAllowedDirectory) {
 			if (filePath != null && filePath.startsWith(dir)) {
 				return;
 			}
 		}
 		throw new SecurityException("File write access on " + file + "(" + filePath + ") is not allowed.");
 	}
 
 	/**
 	 * File delete access is allowed <br>
 	 * on "agent.exec.folder".
 	 * 
 	 * @param file
 	 *            file path
 	 */
 	private void fileAccessDeleteAllowed(String file) {
 		String filePath = normalize(file, workDirectory);
 		for (String dir : deleteAllowedDirectory) {
 			if (filePath != null && filePath.startsWith(dir)) {
 				return;
 			}
 		}
 		throw new SecurityException("File delete access on " + file + "(" + filePath + ") is not allowed.");
 	}
 
 	@Override
 	public void checkMulticast(InetAddress maddr) {
 		throw new SecurityException("Multicast on " + maddr.toString() + " is not always allowed.");
 	}
 
 	@Override
 	public void checkConnect(String host, int port) {
 		this.netWorkAccessAllowed(host);
 	}
 
 	@Override
 	public void checkConnect(String host, int port, Object context) {
 		this.netWorkAccessAllowed(host);
 	}
 
 	public String normalize(String filename, String workingDirectory) {
 		if (getPrefixLength(filename) == 0 && workingDirectory != null) {
 			filename = workingDirectory + File.separator + filename;
 		}
 		return doNormalize(filename, SYSTEM_SEPARATOR, true);
 	}
 
 	/**
 	 * NetWork access is allowed on "ngrinder.etc.hosts".
 	 * 
 	 * @param host
 	 *            host name
 	 */
 	private void netWorkAccessAllowed(String host) {
 		if (allowedHost.contains(host)) {
 			return;
 		}
 		throw new SecurityException("NetWork access on " + host + " is not allowed. Please add " + host
 						+ " on the target host setting.");
 	}
 
 	/**
 	 * The system separator character.
 	 */
 	private static final char SYSTEM_SEPARATOR = File.separatorChar;
 
 	/**
 	 * The Unix separator character.
 	 */
 	private static final char UNIX_SEPARATOR = '/';
 
 	/**
 	 * The Windows separator character.
 	 */
 	private static final char WINDOWS_SEPARATOR = '\\';
 
 	/**
 	 * The separator character that is the opposite of the system separator.
 	 */
 	private static final char OTHER_SEPARATOR;
 	static {
 		if (isSystemWindows()) {
 			OTHER_SEPARATOR = UNIX_SEPARATOR;
 		} else {
 			OTHER_SEPARATOR = WINDOWS_SEPARATOR;
 		}
 	}
 
 	// -----------------------------------------------------------------------
 	/**
 	 * Determines if Windows file system is in use.
 	 * 
 	 * @return true if the system is Windows
 	 */
 	static boolean isSystemWindows() {
 		return SYSTEM_SEPARATOR == WINDOWS_SEPARATOR;
 	}
 
 	/**
 	 * Internal method to perform the normalization.
 	 * 
 	 * @param filename
 	 *            the filename
 	 * @param separator
 	 *            The separator character to use
 	 * @param keepSeparator
 	 *            true to keep the final separator
 	 * @return the normalized filename
 	 */
 	private static String doNormalize(String filename, char separator, boolean keepSeparator) {
 		if (filename == null) {
 			return null;
 		}
 		int size = filename.length();
 		if (size == 0) {
 			return filename;
 		}
 		int prefix = getPrefixLength(filename);
 		if (prefix < 0) {
 			return null;
 		}
 
 		char[] array = new char[size + 2]; // +1 for possible extra slash, +2 for arraycopy
 		filename.getChars(0, filename.length(), array, 0);
 
 		// fix separators throughout
 		char otherSeparator = (separator == SYSTEM_SEPARATOR ? OTHER_SEPARATOR : SYSTEM_SEPARATOR);
 		for (int i = 0; i < array.length; i++) {
 			if (array[i] == otherSeparator) {
 				array[i] = separator;
 			}
 		}
 
 		// add extra separator on the end to simplify code below
 		boolean lastIsDirectory = true;
 		if (array[size - 1] != separator) {
 			array[size++] = separator;
 			lastIsDirectory = false;
 		}
 
 		// adjoining slashes
 		for (int i = prefix + 1; i < size; i++) {
 			if (array[i] == separator && array[i - 1] == separator) {
 				System.arraycopy(array, i, array, i - 1, size - i);
 				size--;
 				i--;
 			}
 		}
 
 		// dot slash
 		for (int i = prefix + 1; i < size; i++) {
 			if (array[i] == separator && array[i - 1] == '.' && (i == prefix + 1 || array[i - 2] == separator)) {
 				if (i == size - 1) {
 					lastIsDirectory = true;
 				}
 				System.arraycopy(array, i + 1, array, i - 1, size - i);
 				size -= 2;
 				i--;
 			}
 		}
 
 		// double dot slash
 		outer: for (int i = prefix + 2; i < size; i++) {
 			if (array[i] == separator && array[i - 1] == '.' && array[i - 2] == '.'
 							&& (i == prefix + 2 || array[i - 3] == separator)) {
 				if (i == prefix + 2) {
 					return null;
 				}
 				if (i == size - 1) {
 					lastIsDirectory = true;
 				}
 				int j;
 				for (j = i - 4; j >= prefix; j--) {
 					if (array[j] == separator) {
 						// remove b/../ from a/b/../c
 						System.arraycopy(array, i + 1, array, j + 1, size - i);
 						size -= (i - j);
 						i = j + 1;
 						continue outer;
 					}
 				}
 				// remove a/../ from a/../c
 				System.arraycopy(array, i + 1, array, prefix, size - i);
 				size -= (i + 1 - prefix);
 				i = prefix + 1;
 			}
 		}
 
 		if (size <= 0) { // should never be less than 0
 			return "";
 		}
 		if (size <= prefix) { // should never be less than prefix
 			return new String(array, 0, size);
 		}
 		if (lastIsDirectory && keepSeparator) {
 			return new String(array, 0, size); // keep trailing separator
 		}
 		return new String(array, 0, size - 1); // lose trailing separator
 	}
 
 	// -----------------------------------------------------------------------
 	/**
 	 * Returns the length of the filename prefix, such as <code>C:/</code> or <code>~/</code>.
 	 * <p>
 	 * This method will handle a file in either Unix or Windows format.
 	 * <p>
 	 * The prefix length includes the first slash in the full filename if applicable. Thus, it is
 	 * possible that the length returned is greater than the length of the input string.
 	 * 
 	 * <pre>
 	 * Windows:
 	 * a\b\c.txt           --> ""          --> relative
 	 * \a\b\c.txt          --> "\"         --> current drive absolute
 	 * C:a\b\c.txt         --> "C:"        --> drive relative
 	 * C:\a\b\c.txt        --> "C:\"       --> absolute
 	 * \\server\a\b\c.txt  --> "\\server\" --> UNC
 	 * 
 	 * Unix:
 	 * a/b/c.txt           --> ""          --> relative
 	 * /a/b/c.txt          --> "/"         --> absolute
 	 * ~/a/b/c.txt         --> "~/"        --> current user
 	 * ~                   --> "~/"        --> current user (slash added)
 	 * ~user/a/b/c.txt     --> "~user/"    --> named user
 	 * ~user               --> "~user/"    --> named user (slash added)
 	 * </pre>
 	 * <p>
 	 * The output will be the same irrespective of the machine that the code is running on. ie. both
 	 * Unix and Windows prefixes are matched regardless.
 	 * 
 	 * @param filename
 	 *            the filename to find the prefix in, null returns -1
 	 * @return the length of the prefix, -1 if invalid or null
 	 */
 	public static int getPrefixLength(String filename) {
 		if (filename == null) {
 			return -1;
 		}
 		int len = filename.length();
 		if (len == 0) {
 			return 0;
 		}
 		char ch0 = filename.charAt(0);
 		if (ch0 == ':') {
 			return -1;
 		}
 		if (len == 1) {
 			if (ch0 == '~') {
 				return 2; // return a length greater than the input
 			}
 			return (isSeparator(ch0) ? 1 : 0);
 		} else {
 			if (ch0 == '~') {
 				int posUnix = filename.indexOf(UNIX_SEPARATOR, 1);
 				int posWin = filename.indexOf(WINDOWS_SEPARATOR, 1);
 				if (posUnix == -1 && posWin == -1) {
 					return len + 1; // return a length greater than the input
 				}
 				posUnix = (posUnix == -1 ? posWin : posUnix);
 				posWin = (posWin == -1 ? posUnix : posWin);
 				return Math.min(posUnix, posWin) + 1;
 			}
 			char ch1 = filename.charAt(1);
 			if (ch1 == ':') {
 				ch0 = Character.toUpperCase(ch0);
 				if (ch0 >= 'A' && ch0 <= 'Z') {
 					if (len == 2 || isSeparator(filename.charAt(2)) == false) {
 						return 2;
 					}
 					return 3;
 				}
 				return -1;
 
 			} else if (isSeparator(ch0) && isSeparator(ch1)) {
 				int posUnix = filename.indexOf(UNIX_SEPARATOR, 2);
 				int posWin = filename.indexOf(WINDOWS_SEPARATOR, 2);
 				if ((posUnix == -1 && posWin == -1) || posUnix == 2 || posWin == 2) {
 					return -1;
 				}
 				posUnix = (posUnix == -1 ? posWin : posUnix);
 				posWin = (posWin == -1 ? posUnix : posWin);
 				return Math.min(posUnix, posWin) + 1;
 			} else {
 				return (isSeparator(ch0) ? 1 : 0);
 			}
 		}
 	}
 
 	// -----------------------------------------------------------------------
 	/**
 	 * Checks if the character is a separator.
 	 * 
 	 * @param ch
 	 *            the character to check
 	 * @return true if it is a separator character
 	 */
 	private static boolean isSeparator(char ch) {
 		return (ch == UNIX_SEPARATOR) || (ch == WINDOWS_SEPARATOR);
 	}
 
 }
