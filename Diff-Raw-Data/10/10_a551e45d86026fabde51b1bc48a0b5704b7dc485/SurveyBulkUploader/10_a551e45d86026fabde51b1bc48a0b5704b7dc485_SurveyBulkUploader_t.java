 package org.waterforpeople.mapping.dataexport;
 
 import java.io.BufferedReader;
 import java.io.ByteArrayOutputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.net.HttpURLConnection;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.StringTokenizer;
 
 import javax.swing.SwingUtilities;
 
 import com.gallatinsystems.common.util.FileUtil;
 import com.gallatinsystems.common.util.ImageUtil;
 import com.gallatinsystems.common.util.UploadUtil;
 import com.gallatinsystems.common.util.ZipUtil;
 import com.gallatinsystems.framework.dataexport.applet.DataImporter;
 import com.gallatinsystems.framework.dataexport.applet.ProgressDialog;
 
 /**
  * qUtility to recursively search the file system for all zip and jpg files to
  * upload. Some ignore paths are hard coded based on the data upload by WSP in
  * Liberia.
  * 
  * This utility will also combine all zip files in a single directory into a
  * single ZIP taking care not to have any duplicate survey instances.
  * 
  * After uploading a zip file, this utility will call the server to kick off
  * processing
  * 
  * 
  * 
  * @author Christopher Fagiani
  * 
  */
 public class SurveyBulkUploader implements DataImporter {
 
 	private static final String NOTIFICATION_PATH = "/processor?action=submit&fileName=";
 	private static final String UPLOAD_IMAGE_MODE = "uploadImageOnly";
 	private static final String ZIP_ONLY_MODE = "processZipOnly";
 	private static final String MODE_KEY = "mode";
 	private static final String PROGRESS_FILE_NAME = "progress.txt";
 
 	private static final String IMAGE_TEMP_DIR = "resized";
 
 	private static final String IMAGE_POLICY_KEY = "imagePolicy";
 	private static final String IMAGE_SIG_KEY = "imageSig";
 	private static final String DATA_POLICY_KEY = "dataPolicy";
 	private static final String DATA_SIG_KEY = "dataSig";
 	private static final String AWS_ID_KEY = "awsId";
 	private static final String UPLOAD_BASE_KEY = "uploadBase";
 	private static final String DEFAULT_LOCALE = "en";
 	private static Map<String, String> UPLOADING;
 	private static Map<String, String> COMPLETE;
 
 	static {
 		UPLOADING = new HashMap<String, String>();
 		UPLOADING.put("en", "Uploading");
 
 		COMPLETE = new HashMap<String, String>();
 		COMPLETE.put("en", "Complete");
 	}
 
 	private String locale = DEFAULT_LOCALE;
 	private ProgressDialog progressDialog;
 
 	@Override
 	public Map<Integer, String> validate(File file) {
 		// TODO: add validation
 		Map<Integer, String> errorMap = new HashMap<Integer, String>();
 		return errorMap;
 	}
 
 	@Override
 	public void executeImport(File sourceDirectory, String serverBase,
 			Map<String, String> criteria) {
 
 		int i = 0;
 		boolean uploadImage = true;
 		boolean processZip = true;
 		List<String> processedList = new ArrayList<String>();
 		FileUtil futil = new FileUtil();
 
 		String progressFileName = sourceDirectory + File.separator
 				+ PROGRESS_FILE_NAME;
 		try {
 
 			File progressFile = new File(progressFileName);
 			if (progressFile.exists()) {
 				String allData = futil.readFromFile(progressFileName);
 				StringTokenizer strTok = new StringTokenizer(allData, "|");
 				while (strTok.hasMoreTokens()) {
 					processedList.add(strTok.nextToken());
 				}
 			}
 		} catch (IOException e1) {
 			System.err.println("Could not process progress file: " + e1);
 			e1.printStackTrace(System.err);
 		}
 		List<List<File>> filesInDir = addFilesInDirectory(sourceDirectory,
 				processedList, true);
 
 		progressDialog = new ProgressDialog(filesInDir.size(), locale);
 		progressDialog.setVisible(true);
 
 		List<File> filesToUpload = filesInDir.get(0);
 		if (UPLOAD_IMAGE_MODE.equalsIgnoreCase(criteria.get(MODE_KEY))) {
 			processZip = false;
 		} else if (ZIP_ONLY_MODE.equalsIgnoreCase(criteria.get(MODE_KEY))) {
 			uploadImage = false;
 		}
 		File tempDir = new File(sourceDirectory, IMAGE_TEMP_DIR);
 		tempDir.mkdirs();
 
 		for (File fx : filesToUpload) {
 			if (!processedList.contains(fx.getName())) {
 				try {
 					System.out.println("uploading " + fx.getCanonicalPath()
 							+ " file " + (i + 1) + " of "
 							+ filesToUpload.size());
 
 					if (fx.getName().endsWith(".jpg")) {
 						if (uploadImage) {
 							File resizedFile = ImageUtil.resizeImage(fx,
 									tempDir.getAbsolutePath(), 500, 500);
 							UploadUtil.upload(futil.readFileBytes(resizedFile),
 									resizedFile.getName(), "images",
 									criteria.get(UPLOAD_BASE_KEY),
 									criteria.get(AWS_ID_KEY),
 									criteria.get(IMAGE_POLICY_KEY),
 									criteria.get(IMAGE_SIG_KEY), "image/jpeg",
 									null);
 							// now delete the temp file
 							resizedFile.delete();
 						}
 					} else {
 						if (processZip) {
 							UploadUtil.upload(futil.readFileBytes(fx),
 									fx.getName(), "devicezip",
 									criteria.get(UPLOAD_BASE_KEY),
 									criteria.get(AWS_ID_KEY),
 									criteria.get(DATA_POLICY_KEY),
 									criteria.get(DATA_SIG_KEY),
 									"application/zip", null);
 
 							// now notify the server that a new file is there
 							// for processing
 							sendFileNotification(serverBase, fx.getName());
 							// delete the merged zip
 							fx.delete();
 						}
 					}
 					processedList.add(fx.getName());
 					i++;
 				} catch (Exception e) {
 					// TODO report error to ui
 					e.printStackTrace();
 				}
 			}
 			SwingUtilities.invokeLater(new StatusUpdater(i, UPLOADING
 					.get(locale)));
 		}
 		StringBuilder buf = new StringBuilder();
 		for (String s : processedList) {
 			buf.append(s).append("|");
 		}
 		for (File fn : filesInDir.get(1)) {
 			buf.append(fn.getName()).append("|");
 		}
 		futil.writeToFile(buf.toString(), progressFileName);
 		SwingUtilities.invokeLater(new StatusUpdater(i, COMPLETE.get(locale),
 				true));
 
 	}
 
 	public static void sendFileNotification(String serverBase, String fileName)
 			throws Exception {
 		URL url = new URL(serverBase + NOTIFICATION_PATH + fileName);
 		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
 		conn.setRequestMethod("GET");
 		conn.setDoOutput(true);
 		String line;
 		BufferedReader reader = new BufferedReader(new InputStreamReader(
 				conn.getInputStream()));
 		while ((line = reader.readLine()) != null) {
 			System.out.println(line);
 		}
 		reader.close();
 	}
 
 	private static List<List<File>> addFilesInDirectory(File dir,
 			List<String> ignoreList, boolean hasUUID) {
 		List<File> fileList = new ArrayList<File>();
 		List<File> collapsedZips = new ArrayList<File>();
 		List<File> zipFileList = new ArrayList<File>();
 		if (dir.isDirectory()) {
 			File[] files = dir.listFiles();
 			if (files != null) {
 				for (int i = 0; i < files.length; i++) {
 					if (files[i].isFile()
 							&& !ignoreList.contains(files[i].getName())) {
 						if (files[i].getName().endsWith(".jpg")) {
 							if (!files[i].getName().endsWith(" - Copy.jpg")) {
					
 								fileList.add(files[i]);
 							}
 						} else if (files[i].getName().endsWith(".zip")) {
 							if (!files[i].getName().contains("wfpGenerated")) {
 								zipFileList.add(files[i]);
 							}
 						}
 					} else if (files[i].isDirectory()
 							&& !files[i].getName().endsWith("fieldsurvey")
 							&& !files[i].getName().endsWith(".thumbnails")
 							&& !files[i].getName().endsWith("processed")
 							&& !files[i].getName().endsWith(IMAGE_TEMP_DIR)) {
 						List<List<File>> added = addFilesInDirectory(files[i],
 								ignoreList, hasUUID);
 						fileList.addAll(added.get(0));
 						collapsedZips.addAll(added.get(1));
 					}
 				}
 				// now handle possible duplicates within the zip files by
 				// reading all zip files in this directory into memory and then
 				// writing a new, single file
 				if (zipFileList.size() > 0) {
 					StringBuilder allContent = new StringBuilder();
 					for (File fx : zipFileList) {
 						try {
 							FileInputStream fis;
 							fis = new FileInputStream(fx);
 							long length = fx.length();
 							byte[] bytes = new byte[(int) length];
 							int offset = 0;
 							int numRead = 0;
 							while (offset < bytes.length
 									&& (numRead = fis.read(bytes, offset,
 											bytes.length - offset)) >= 0) {
 								offset += numRead;
 							}
 							fis.close();
 							allContent.append(ZipUtil.unZip(bytes, "data.txt"));
 							collapsedZips.add(fx);
 						} catch (Exception e) {
 
 						}
 					}
 					// now we have all the data, so iterate through and build
 					// the master list
 
 					StringTokenizer strTok = new StringTokenizer(
 							allContent.toString(), "\n");
 
 					StringBuilder newContent = new StringBuilder();
 
 					Set<String> keySet = new HashSet<String>();
 					String lastKey = "";
 					while (strTok.hasMoreTokens()) {
 						String line = strTok.nextToken();
 
 						String[] fields = line.split("\t");
 						// handle "old" version that use comma delimiters
 						if (fields.length < 4) {
 							fields = line.split(",");
 						}
 						if (fields.length >= 6) {
 							String key = null;
 							if (hasUUID) {
 								key = fields[fields.length - 1];
 							} else {
 								// if we don't have a UUID, then use the
 								// username + the response id
 								key = fields[1] + fields[5];
 							}
 							if (!keySet.contains(key)) {
 								keySet.add(key);
 								lastKey = key;
 							}
 							if (key.equals(lastKey)) {
 								newContent.append(line + "\n");
 							}
 
 						}
 					}
 					// now write the new file
 					try {
 						ByteArrayOutputStream stream = ZipUtil.generateZip(
 								newContent.toString(), "data.txt");
 						File f = new File(dir, "wfpGenerated"
 								+ System.currentTimeMillis() + ".zip");
 						FileOutputStream foStream = new FileOutputStream(f);
 						stream.writeTo(foStream);
 						foStream.close();
 						stream.close();
 						fileList.add(f);
 					} catch (Exception e) {
 						e.printStackTrace();
 					}
 				}
 			}
 		}
 		List<List<File>> result = new ArrayList<List<File>>();
 		result.add(fileList);
 		result.add(collapsedZips);
 		return result;
 	}
 
 	/**
 	 * Private class to handle updating of the UI thread from our worker thread
 	 */
 	private class StatusUpdater implements Runnable {
 
 		private int step;
 		private String msg;
 		private boolean isComplete;
 
 		public StatusUpdater(int step, String message) {
 			this(step, message, false);
 		}
 
 		public StatusUpdater(int step, String message, boolean isComplete) {
 			msg = message;
 			this.step = step;
 			this.isComplete = isComplete;
 		}
 
 		public void run() {
 			progressDialog.update(step, msg, isComplete);
 		}
 	}
 }
