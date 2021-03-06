 /**
  * Copyright (C) 2011 The Serval Project
  *
  * This file is part of Serval Software (http://www.servalproject.org)
  *
  * Serval Software is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 3 of the License, or
  * (at your option) any later version.
  *
  * This source code is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this source code; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  */
 
 package org.servalproject.system;
 
 import java.io.BufferedReader;
 import java.io.BufferedWriter;
 import java.io.DataInputStream;
 import java.io.DataOutputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.FileReader;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.InputStream;
 import java.net.HttpURLConnection;
 import java.net.URL;
 import java.net.URLConnection;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Scanner;
 import java.util.Set;
 import java.util.TreeSet;
 
 import org.apache.http.Header;
 import org.apache.http.HttpEntity;
 import org.apache.http.HttpResponse;
 import org.apache.http.client.ClientProtocolException;
 import org.apache.http.client.HttpClient;
 import org.apache.http.client.methods.HttpGet;
 import org.apache.http.impl.client.DefaultHttpClient;
 import org.apache.http.impl.cookie.DateParseException;
 import org.apache.http.impl.cookie.DateUtils;
 import org.apache.http.protocol.BasicHttpContext;
 import org.apache.http.protocol.HttpContext;
 import org.servalproject.LogActivity;
 import org.servalproject.ServalBatPhoneApplication;
 import org.servalproject.WifiApControl;
 
 import android.os.Build;
 import android.util.Log;
 
 public class ChipsetDetection {
 	private static final String strMustExist = "exists";
 	private static final String strMustNotExist = "missing";
 	private static final String strandroid = "androidversion";
 	private static final String strCapability = "capability";
 	private static final String strExperimental = "experimental";
	private static final String strNoWirelessExtensions = "nowirelessextensions";
 	private static final String strAh_on_tag = "#Insert_Adhoc_on";
 	private static final String strAh_off_tag = "#Insert_Adhoc_off";
 
 	private String logFile;
 	private String detectPath;
 	private String edifyPath;
 	private String edifysrcPath;
 
 	private ServalBatPhoneApplication app;
 	private Chipset wifichipset;
 
 	private String manufacturer;
 	private String brand;
 	private String model;
 	private String name;
 
 	private ChipsetDetection(boolean allowExperimentalP) {
 		this.app = ServalBatPhoneApplication.context;
 		this.logFile = app.coretask.DATA_FILE_PATH + "/var/wifidetect.log";
 		this.detectPath = app.coretask.DATA_FILE_PATH + "/conf/wifichipsets/";
 		this.edifyPath = app.coretask.DATA_FILE_PATH + "/conf/adhoc.edify";
 		this.edifysrcPath = app.coretask.DATA_FILE_PATH
 				+ "/conf/adhoc.edify.src";
 
 		manufacturer = app.coretask.getProp("ro.product.manufacturer");
 		brand = app.coretask.getProp("ro.product.brand");
 		model = app.coretask.getProp("ro.product.model");
 		name = app.coretask.getProp("ro.product.name");
 	}
 
 	private static ChipsetDetection detection;
 
 	public static ChipsetDetection getDetection() {
 		if (detection == null)
 			detection = new ChipsetDetection(false);
 		return detection;
 	}
 
 	private HashMap<String, Boolean> existsTests = new HashMap<String, Boolean>();
 
 	// Check if the corresponding file exists
 	private boolean fileExists(String filename) {
 		// Check if the specified file exists during wifi chipset detection.
 		// Record the result in a dictionary or similar structure so that if
 		// we fail to detect a phone, we can create a bundle of information
 		// that can be sent back to the Serval Project developers to help them
 		// add support for the phone.
 		Boolean result = existsTests.get(filename);
 		if (result == null) {
 			result = (new File(filename)).exists();
 			existsTests.put(filename, result);
 		}
 		return result;
 	}
 
 	public Set<Chipset> getChipsets() {
 		Set<Chipset> chipsets = new TreeSet<Chipset>();
 
 		File detectScripts = new File(detectPath);
 		if (!detectScripts.isDirectory())
 			return null;
 
 		for (File script : detectScripts.listFiles()) {
 			if (!script.getName().endsWith(".detect"))
 				continue;
 			chipsets.add(new Chipset(script));
 		}
 		return chipsets;
 	}
 
 	private void scan(File folder, List<File> results,
 			Set<String> insmodCommands) {
 		File files[] = folder.listFiles();
 		if (files == null)
 			return;
 		for (File file : files) {
 			try {
 				if (file.isDirectory()) {
 					scan(file, results, insmodCommands);
 				} else {
 					String path = file.getCanonicalPath();
 					if (path.contains("wifi") || path.endsWith(".ko")) {
 						results.add(file);
 					}
 					// Only look in small files, and stop looking if a file is
 					// binary
 					if (insmodCommands != null && file.length() < 16384
 							&& ((file.getName().endsWith(".so") == false))
 							&& ((file.getName().endsWith(".ttf") == false))
 							&& ((file.getName().endsWith(".ogg") == false))
 							&& ((file.getName().endsWith(".odex") == false))
 							&& ((file.getName().endsWith(".apk") == false))) {
 						BufferedReader b = new BufferedReader(new FileReader(
 								file));
 						try {
 							String line = null;
 							String dmp = null;
 							while ((line = b.readLine()) != null) {
 								// Stop looking if the line seems to be binary
 								if (line.length() > 0
 										&& (line.charAt(0) > 0x7d || line
 												.charAt(0) < 0x09)) {
 									// LogActivity.logMessage("guess", file
 									// + " seems to be binary", false);
 									break;
 								}
 								if (line.startsWith("DRIVER_MODULE_PATH="))
 									dmp = line.substring(19);
 								if (dmp != null
 										&& line
 												.startsWith("DRIVER_MODULE_ARG=")) {
 									insmodCommands.add("insmod " + dmp + " \""
 											+ line.substring(18) + "\"");
 									dmp = null;
 								}
 								if (line.contains("insmod ")) {
 									// Ooh, an insmod command.
 									// Let's see if it is interesting.
 									insmodCommands.add(line);
 								}
 							}
 							b.close();
 						} catch (IOException e) {
 							b.close();
 						} finally {
 							b.close();
 						}
 					}
 				}
 			} catch (IOException e) {
 				continue;
 			}
 		}
 	}
 
 	private List<File> interestingFiles = null;
 
 	private List<File> findModules(Set<String> insmodCommands) {
 		if (interestingFiles == null || insmodCommands != null) {
 			interestingFiles = new ArrayList<File>();
 			scan(new File("/system"), interestingFiles, insmodCommands);
 			scan(new File("/lib"), interestingFiles, insmodCommands);
 			scan(new File("/wifi"), interestingFiles, insmodCommands);
 			scan(new File("/etc"), interestingFiles, insmodCommands);
 		}
 		return interestingFiles;
 	}
 
 	private final static String BASE_URL = "http://developer.servalproject.org/";
 
 	private boolean downloadIfModified(String url, File destination)
 			throws IOException {
 
 		HttpClient httpClient = new DefaultHttpClient();
 		HttpContext httpContext = new BasicHttpContext();
 		HttpGet httpGet = new HttpGet(url);
 		if (destination.exists()) {
 			httpGet.addHeader("If-Modified-Since", DateUtils
 					.formatDate(new Date(destination.lastModified())));
 		}
 
 		try {
 			Log.v("BatPhone", "Fetching: " + url);
 			HttpResponse response = httpClient.execute(httpGet, httpContext);
 			int code = response.getStatusLine().getStatusCode();
 			switch (code - code % 100) {
 			case 200:
 				HttpEntity entity = response.getEntity();
 				FileOutputStream output = new FileOutputStream(destination);
 				entity.writeTo(output);
 				output.close();
 
 				Header modifiedHeader = response
 						.getFirstHeader("Last-Modified");
 				if (modifiedHeader != null) {
 					try {
 						destination.setLastModified(DateUtils.parseDate(
 								modifiedHeader.getValue()).getTime());
 					} catch (DateParseException e) {
 						Log.v("BatPhone", e.toString(), e);
 					}
 				}
 				Log.v("BatPhone", "Saved to " + destination);
 				return true;
 			case 300:
 				Log.v("BatPhone", "Not Changed");
 				// not changed
 				return false;
 			default:
 				throw new IOException(response.getStatusLine().toString());
 			}
 		} catch (ClientProtocolException e) {
 			throw new IOException(e.toString());
 		}
 	}
 
 	public boolean downloadNewScripts() {
 		try {
 			File f = new File(app.coretask.DATA_FILE_PATH + "/conf/chipset.zip");
 			if (this.downloadIfModified(BASE_URL + "chipset.zip", f)) {
 				Log.v("BatPhone", "Extracting archive");
 				app.coretask.extractZip(new FileInputStream(f),
 						app.coretask.DATA_FILE_PATH + "/conf/wifichipsets");
 				return true;
 			}
 		} catch (IOException e) {
 			Log.e("BatPhone", e.toString(), e);
 		}
 		return false;
 	}
 
 	private String getUrl(URL url) throws IOException {
 		Log.v("BatPhone", "Fetching " + url);
 		URLConnection conn = url.openConnection();
 		InputStream in = conn.getInputStream();
 		return new Scanner(in).useDelimiter("\\A").next();
 	}
 
 	private String uploadFile(File f, String name, URL url) throws IOException {
 		final String boundary = "*****";
 
 		Log.v("BatPhone", "Uploading file " + f.getName() + " to " + url);
 
 		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
 
 		conn.setDoInput(true);
 		conn.setDoOutput(true);
 		conn.setUseCaches(false);
 		conn.setRequestMethod("POST");
 		conn.setRequestProperty("Connection", "Keep-Alive");
 		conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="
 				+ boundary);
 
 		DataOutputStream out = new DataOutputStream(conn.getOutputStream());
 		out.writeBytes("--" + boundary + "\n");
 		out
 				.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\""
 						+ name + "\"\nContent-Type: text/plain\n\n");
 		{
 			FileInputStream in = new FileInputStream(f);
 			try {
 				byte buff[] = new byte[4 * 1024];
 				int read;
 				while ((read = in.read(buff)) > 0) {
 					out.write(buff, 0, read);
 				}
 			} finally {
 				in.close();
 			}
 			out.writeBytes("\n--" + boundary + "\n");
 			out.flush();
 		}
 
 		InputStream in = conn.getInputStream();
 		return new Scanner(in).useDelimiter("\\A").next();
 	}
 
 	public boolean needUpload = false;
 	private String logName;
 
 	private void testLog() {
 		// PGS - Disabling uploading of logs for now until we make a UI to ask
 		// the
 		// user if they are willing, and show them what will be sent.
 		// try {
 		// needUpload = true;
 		// logName = manufacturer + "_" + brand + "_" + model + "_"
 		// + name;
 		//
 		// String result = getUrl(new URL(BASE_URL
 		// + "upload_v1_exists.php?name=" + logName));
 		// Log.v("BatPhone", result);
 		// if (result.equals("Ok."))
 		// needUpload = false;
 		// } catch (Exception e) {
 		// Log.e("BatPhone", e.toString(), e);
 		// }
 	}
 
 	public void uploadLog() {
 		if (!app.settings.getBoolean("dataCollection", false))
 			return;
 
 		// PGS - Disabling uploading of logs for now until we make a UI to ask
 		// the
 		// user if they are willing, and show them what will be sent.
 		return;
 		//
 		// try {
 		// testLog();
 		// if (needUpload) {
 		// String result = uploadFile(new File(this.logFile), logName,
 		// new URL(BASE_URL + "upload_v1_log.php"));
 		// Log.v("BatPhone", result);
 		// }
 		// } catch (Exception e) {
 		// Log.e("BatPhone", e.toString(), e);
 		// }
 	}
 
 	public List<Chipset> detected_chipsets = null;
 
 	public Chipset detect(boolean allowExperimentalP) {
 		int count = 0;
 		Chipset detected = null;
 
 		detected_chipsets = new ArrayList<Chipset>();
 
 		// start a new log file
 		new File(logFile).delete();
 
 		LogActivity.logErase("detect");
 
 		for (Chipset chipset : getChipsets()) {
 			// skip experimental chipset
 			if (testForChipset(chipset, allowExperimentalP)) {
 				count++;
 				detected_chipsets.add(chipset);
 				detected = chipset;
 			}
 		}
 
 		if (count != 1)
 			return null;
 		return detected;
 	}
 
 	public boolean updateChipset() {
 		if (!downloadNewScripts())
 			return false;
 
 		setChipset(detect(false));
 		return true;
 	}
 
 	/* Function to identify the chipset and log the result */
 	public boolean identifyChipset() {
 		Chipset detected = null;
 		do {
 			detected = detect(false);
 			if (detected != null)
 				break;
 
 			if (!downloadNewScripts())
 				break;
 
 		} while (true);
 
 		if (detected == null) {
 			logMore();
 			uploadLog();
 		}
 
 		setChipset(detected);
 		if (detected != null)
 			return true;
 		else
 			return false;
 	}
 
 	public Chipset getWifiChipset() {
 		return wifichipset;
 	}
 
 	public String getChipset() {
 		if (wifichipset == null)
 			return null;
 		return wifichipset.chipset;
 	}
 
 	public boolean testAndSetChipset(String value, boolean reportExperimentalP) {
 		for (Chipset chipset : this.getChipsets()) {
 			if (chipset.chipset.equals(value)) {
 				if (detection.testForChipset(chipset, true)) {
 					detection.setChipset(chipset);
 					return true;
 				}
 				break;
 			}
 		}
 		return false;
 	}
 
 	/* Check if the chipset matches with the available chipsets */
 	public boolean testForChipset(Chipset chipset, boolean reportExperimentalP) {
 		// Read
 		// /data/data/org.servalproject/conf/wifichipsets/"+chipset+".detect"
 		// and see if we can meet the criteria.
 		// This method needs to interpret the lines of that file as test
 		// instructions
 		// that can do everything that the old big hairy if()else() chain did.
 		// This largely consists of testing for the existence of files.
 
 		// use fileExists() to test for the existence of files so that we can
 		// generate
 		// a report for this phone in case it is not supported.
 
 		// XXX Stub}
 		try {
 			BufferedWriter writer = new BufferedWriter(new FileWriter(logFile,
 					true), 256);
 
 			writer.write("trying " + chipset + "\n");
 
 			boolean reject = false;
 			int matches = 0;
 			chipset.supportedModes.clear();
 			chipset.detected = false;
 			chipset.experimental = false;
 
 			try {
 				FileInputStream fstream = new FileInputStream(
 						chipset.detectScript);
 				// Get the object of DataInputStream
 				DataInputStream in = new DataInputStream(fstream);
 				String strLine;
 				// Read File Line By Line
 				while ((strLine = in.readLine()) != null) {
 					if (strLine.startsWith("#") || strLine.equals(""))
 						continue;
 
 					writer.write("# " + strLine + "\n");
 					String arChipset[] = strLine.split(" ");
 
 					if (arChipset[0].equals(strCapability)) {
 						for (String mode : arChipset[1].split(",")) {
 							try {
 								WifiMode m = WifiMode.valueOf(mode);
 								if (m != null)
 									chipset.supportedModes.add(m);
 							} catch (IllegalArgumentException e) {
 							}
 						}
 						if (arChipset.length >= 3)
 							chipset.adhocOn = arChipset[2];
 						if (arChipset.length >= 4)
 							chipset.adhocOff = arChipset[3];
 					} else if (arChipset[0].equals(strExperimental)) {
 						chipset.experimental = true;
					} else if (arChipset[0].equals(strNoWirelessExtensions)) {
						chipset.noWirelessExtensions = true;
 					} else {
 
 						boolean lineMatch = false;
 
 						if (arChipset[0].equals(strMustExist)
 								|| arChipset[0].equals(strMustNotExist)) {
 							boolean exist = fileExists(arChipset[1]);
 							boolean wanted = arChipset[0].equals(strMustExist);
 							writer.write((exist ? "exists" : "missing") + " "
 									+ arChipset[1] + "\n");
 							lineMatch = (exist == wanted);
 						} else if (arChipset[0].equals(strandroid)) {
 							int sdkVersion = Build.VERSION.SDK_INT;
 							writer.write(strandroid + " = "
 									+ Build.VERSION.SDK_INT + "\n");
 							int requestedVersion = Integer
 									.parseInt(arChipset[2]);
 
 							if (arChipset[1].indexOf('!') >= 0) {
 								lineMatch = (sdkVersion != requestedVersion);
 							} else
 								lineMatch = ((arChipset[1].indexOf('=') >= 0 && sdkVersion == requestedVersion)
 										|| (arChipset[1].indexOf('<') >= 0 && sdkVersion < requestedVersion) || (arChipset[1]
 										.indexOf('>') >= 0 && sdkVersion > requestedVersion));
 						} else {
 							Log.v("BatPhone", "Unhandled line in " + chipset
 									+ " detect script " + strLine);
 							continue;
 						}
 
 						if (lineMatch)
 							matches++;
 						else
 							reject = true;
 					}
 				}
 
 				in.close();
 
 				if (matches < 1)
 					reject = true;
 
 				// Return our final verdict
 				if (!reject) {
 					Log.i("BatPhone", "identified chipset " + chipset
 							+ (chipset.experimental ? " (experimental)" : ""));
 					writer.write("is " + chipset + "\n");
 					chipset.detected = true;
 					if (chipset.experimental) {
 						LogActivity.logMessage("detect",
 								"Guessing how to control the chipset using script "
 										+ chipset, false);
 					} else
 						LogActivity.logMessage("detect",
 								"Detected this handset as a " + chipset, false);
 					return true;
 				}
 
 			} catch (IOException e) {
 				Log.i("BatPhone", e.toString(), e);
 				writer.write("Exception Caught in testForChipset" + e + "\n");
 				reject = true;
 			}
 
 			writer.write("isnot " + chipset + "\n");
 
 			writer.close();
 			return !reject;
 		} catch (IOException e) {
 			Log.e("BatPhone", e.toString(), e);
 			return false;
 		}
 	}
 
 	private void appendFile(FileOutputStream out, String path)
 			throws IOException {
 		DataInputStream input = new DataInputStream(new FileInputStream(path));
 		String strLineinput;
 		while ((strLineinput = input.readLine()) != null) {
 			out.write((strLineinput + "\n").getBytes());
 		}
 		input.close();
 	}
 
 	public void inventSupport() {
 		// Make a wild guess for a script that MIGHT work
 		// Start with list of kernel modules
 		// XXX we should search for files containing insmod to see if there are
 		// any parameters that might be needed (as is the case on the IDEOS
 		// U8150)
 
 		Set<String> insmodCommands = new HashSet<String>();
 
 		List<String> knownModules = getList(this.detectPath
 				+ "known-wifi.modules");
 		List<String> knownNonModules = getList(this.detectPath
 				+ "non-wifi.modules");
 		List<File> candidatemodules = findModules(insmodCommands);
 		List<File> modules = new ArrayList<File>();
 		int guesscount = 0;
 
 		// First, let's just try only known modules.
 		// XXX - These are the wrong search methods
 		for (File module : candidatemodules) {
 			if (module.getName().endsWith(".ko"))
 				if (!knownNonModules.contains(module.getName()))
 					if (knownModules.contains(module.getName()))
 						modules.add(module);
 		}
 
 		if (modules.isEmpty()) {
 			// We didn't find any on our strict traversal, so try again
 			// allowing any non-black-listed modules
 			for (File module : candidatemodules) {
 				if (module.getName().endsWith(".ko"))
 					if (!knownNonModules.contains(module.getName()))
 						modules.add(module);
 			}
 		}
 
 		if (modules.isEmpty()) {
 			// Blast. Couldn't find any modules.
 			// Well, let's just try ifconfig and iwconfig anyway, as they
 			// might just work.
 		}
 
 		// Now that we have the list of modules, we could have a look to see
 		// if there are any sample insmod commands
 		// that we can find in any system files for clues on what parameters
 		// to pass when loading the module, e.g.,
 		// any firmware blobs or nvram.txt or other options.
 		// XXX - Rather obviously we have not implemented this yet.
 
 		LogActivity.logErase("guess");
 
 		String profilename = "failed";
 
 		for (File m : modules) {
 			String path = m.getPath();
 			insmodCommands.add("insmod " + path + " \"\"");
 
 		}
 
 		for (String s : insmodCommands) {
 			String module = null;
 			String args = null;
 			String modname = "noidea";
 			int i;
 
 			i = s.indexOf("insmod ");
 			if (i == -1)
 				continue;
 			i += 7;
 			module = getNextShellArg(s.substring(i));
 			i += module.length() + 1;
 			if (i < s.length())
 				args = getNextShellArg(s.substring(i));
 			else
 				args = "\"\"";
 			if (args.charAt(0) != '\"')
 				args = "\"" + args + "\"";
 
 			modname = module;
 			if (modname.lastIndexOf(".") > -1)
 				modname = modname.substring(1, modname.lastIndexOf("."));
 			if (modname.lastIndexOf("/") > -1)
 				modname = modname.substring(1 + modname.lastIndexOf("/"));
 
 			guesscount++;
 			profilename = "guess-" + guesscount + "-" + modname + "-"
 					+ args.length();
 
 			// Now write out a detect script for this device.
 			// Mark it experimental because we can't be sure that it will be any
 			// good. This means that users will have to actively choose it from
 			// the
 			// wifi settings menu. We could offer it if no non-experimental
 			// chipsets match, but that is best done as a general
 			// policy in the way the chipset selection works.
 			BufferedWriter writer;
 			try {
 				writer = new BufferedWriter(new FileWriter(this.detectPath
 						+ profilename + ".detect", false), 256);
 				writer.write("capability Adhoc " + profilename
 						+ ".adhoc.edify " + profilename + ".off.edify\n");
 				writer.write("experimental\n");
 				if (module.contains("/")) {
 					// XXX We have a problem if we don't know the full path to
 					// the module
 					// for ensuring specificity for choosing this option.
 					// Will think about a nice solution later.
 					writer.write("exists " + module + "\n");
 				}
 				writer.close();
 			} catch (IOException e) {
 				Log.e("BatPhone", e.toString(), e);
 			}
 
 			// The actual edify script consists of the insmod commands followed
 			// by templated content
 			// that does all the ifconfig/iwconfig stuff.
 			// Thus this code does not work with unusual chipsets like the
 			// tiwlan drivers that use
 			// funny configuration commands. Oh well. One day we might add some
 			// cleverness for that.
 
 			try {
 				writer = new BufferedWriter(new FileWriter(this.detectPath
 						+ profilename + ".adhoc.edify", false), 256);
 
 				// Write out edify command to load the module
 				writer.write("module_loaded(\"" + modname
 						+ "\") || log(insmod(\"" + module + "\"," + args
 						+ "),\"Loading " + module + " module\");\n");
 
 				// Write templated adhoc.edify script
 				String line;
 				BufferedReader template = new BufferedReader(new FileReader(
 						this.detectPath + "adhoc.edify.template"));
 				while ((line = template.readLine()) != null) {
 					writer.write(line + "\n");
 				}
 
 				writer.close();
 			} catch (IOException e) {
 				Log.e("BatPhone", e.toString(), e);
 			}
 
 			// Finally to turn off wifi let's just unload all the modules we
 			// loaded earlier.
 			// Crude but fast and effective.
 			try {
 				writer = new BufferedWriter(new FileWriter(this.detectPath
 						+ profilename + ".off.edify", false), 256);
 
 				// Write out edify command to load the module
 				writer.write("module_loaded(\"" + modname + "\") && rmmod(\""
 						+ modname + "\");\n");
 				writer.close();
 			} catch (IOException e) {
 				Log.e("BatPhone", e.toString(), e);
 			}
 
 			LogActivity
 					.logMessage("guess", "Creating best-guess support scripts "
 							+ profilename + " based on kernel module "
 							+ modname + ".", false);
 
 		}
 	}
 
 	private String getNextShellArg(String s) {
 		int i = 0;
 		boolean quoteMode = false;
 		boolean escMode = false;
 
 		// Skip leading white space
 		while (i < s.length() && s.charAt(i) <= ' ')
 			i++;
 		// Get arg
 		while (i < s.length()) {
 			if (escMode)
 				escMode = false;
 			if (quoteMode) {
 				if (s.charAt(i) == '"')
 					quoteMode = false;
 				else if (s.charAt(i) == '\\')
 					escMode = true;
 			} else if (s.charAt(i) <= ' ') {
 				// End of arg
 				return s.substring(0, i);
 			} else if (s.charAt(i) == '\"')
 				quoteMode = true;
 			else if (s.charAt(i) == '\\')
 				escMode = true;
 			i++;
 		}
 		// No word breaks, so return whole thing
 		return s;
 	}
 
 	public static List<String> getList(String filename) {
 		// Read lines from file into a list
 		List<String> l = new ArrayList<String>();
 		String line;
 		try {
 			BufferedReader f = new BufferedReader(new FileReader(filename));
 			while ((line = f.readLine()) != null) {
 				l.add(line);
 			}
 			f.close();
 		} catch (IOException e) {
 			Log.e("BatPhone", e.toString(), e);
 		}
 		return l;
 	}
 
 	private void logMore() {
 		// log other interesting modules/files
 		try {
 			BufferedWriter writer = new BufferedWriter(new FileWriter(logFile,
 					true), 256);
 
 			writer.write("\nHandset Type;\n");
 			writer.write("Manufacturer: " + manufacturer + "\n");
 			writer.write("Brand: " + brand + "\n");
 			writer.write("Model: " + model + "\n");
 			writer.write("Name: " + name + "\n");
 			writer.write("Software Version: " + app.getVersionName() + "\n");
 			writer.write("Android Version: " + Build.VERSION.RELEASE + " (API "
 					+ Build.VERSION.SDK_INT + ")\n");
 			writer.write("Kernel Version: " + app.coretask.getKernelVersion()
 					+ "\n");
 
 			writer.write("\nInteresting modules;\n");
 			for (File path : findModules(null)) {
 				writer.write(path.getCanonicalPath() + "\n");
 			}
 			writer.close();
 		} catch (IOException e) {
 			Log.e("BatPhone", e.toString(), e);
 		}
 	}
 
 	// set chipset configuration
 	public void setChipset(Chipset chipset) {
 		if (chipset == null) {
 			chipset = new Chipset();
 
 			if (detected_chipsets == null || detected_chipsets.size() == 0)
 				chipset.chipset = "Unsupported - " + brand + " " + model + " "
 						+ name;
 			else
 				chipset.chipset = "one of several possible options";
 			chipset.unknown = true;
 
 		}
 
 		// add support for modes via SDK if available
 		if (!chipset.supportedModes.contains(WifiMode.Ap)
 				&& WifiApControl.isApSupported())
 			chipset.supportedModes.add(WifiMode.Ap);
 		if (!chipset.supportedModes.contains(WifiMode.Client))
 			chipset.supportedModes.add(WifiMode.Client);
 		if (!chipset.supportedModes.contains(WifiMode.Off))
 			chipset.supportedModes.add(WifiMode.Off);
 
 		// make sure we have root permission for adhoc support
 		if (chipset.supportedModes.contains(WifiMode.Adhoc)) {
 			if (!app.coretask.hasRootPermission()) {
 				chipset.supportedModes.remove(WifiMode.Adhoc);
 				Log.v("BatPhone",
 						"Unable to support adhoc mode without root permission");
 			}
 		}
 
 		wifichipset = chipset;
 
 		try {
 			FileOutputStream out = new FileOutputStream(edifyPath);
 			FileInputStream fstream = new FileInputStream(edifysrcPath);
 			// Get the object of DataInputStream
 			DataInputStream in = new DataInputStream(fstream);
 			String strLine;
 			// Read File Line By Line
 			while ((strLine = in.readLine()) != null) {
 				if (strLine.startsWith(strAh_on_tag)) {
 					if (chipset.adhocOn != null)
 						appendFile(out, detectPath + chipset.adhocOn);
 				} else if (strLine.startsWith(strAh_off_tag)) {
 					if (chipset.adhocOff != null)
 						appendFile(out, detectPath + chipset.adhocOff);
 				} else
 					out.write((strLine + "\n").getBytes());
 			}
 			in.close();
 			out.close();
 		} catch (IOException exc) {
 			Log.e("Exception caught at set_Adhoc_mode", exc.toString(), exc);
 		}
 	}
 
 	public boolean isModeSupported(WifiMode mode) {
 		if (mode == null)
 			return true;
 		if (wifichipset == null)
 			return false;
 		return wifichipset.supportedModes.contains(mode);
 	}
 
 }
