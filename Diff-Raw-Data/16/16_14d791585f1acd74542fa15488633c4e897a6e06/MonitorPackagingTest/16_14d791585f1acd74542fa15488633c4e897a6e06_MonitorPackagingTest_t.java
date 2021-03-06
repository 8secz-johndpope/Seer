 /*******************************************************************************
  * Copyright (c) 2004 - 2006 University Of British Columbia and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     University Of British Columbia - initial API and implementation
  *******************************************************************************/
 /*
  * Created on Jun 13, 2005
  */
 package org.eclipse.mylar.monitor.tests;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Enumeration;
 import java.util.List;
 import java.util.zip.ZipEntry;
 import java.util.zip.ZipFile;
 
 import org.eclipse.mylar.context.core.ContextCorePlugin;
import org.eclipse.mylar.context.tests.AbstractContextTest;
 import org.eclipse.mylar.internal.context.core.util.ZipFileUtil;
 import org.eclipse.mylar.monitor.MylarMonitorPlugin;
 import org.eclipse.mylar.monitor.usage.MylarUsageMonitorPlugin;
 
 /**
  * @author Mik Kersten
  */
 public class MonitorPackagingTest extends AbstractContextTest {
 
 	public void testCreateUploadPackage() throws IOException, InterruptedException {
 		MylarUsageMonitorPlugin.getDefault().getInteractionLogger().stopMonitoring();
 		// MylarMonitorPlugin.getDefault().stopLog();
 
 		File monitorFile = MylarUsageMonitorPlugin.getDefault().getMonitorLogFile();
 		// File logFile = MylarMonitorPlugin.getDefault().getLogFile();
 
 		List<File> files = new ArrayList<File>();
 		files.add(monitorFile);
 		// files.add(logFile);
 
 		File zipFile = new File(ContextCorePlugin.getDefault().getContextStore().getRootDirectory() + "/mylarUpload.zip");
 
 		ZipFileUtil.createZipFile(zipFile, files);
 
 		// MylarMonitorPlugin.getDefault().startLog();
 		MylarUsageMonitorPlugin.getDefault().getInteractionLogger().startMonitoring();
 
 		// pretend to upload
 		Thread.sleep(1000);
 
 		zipFile = new File(ContextCorePlugin.getDefault().getContextStore().getRootDirectory() + "/mylarUpload.zip");
 
 		// Open the ZIP file
 		ZipFile zf = new ZipFile(zipFile);
 
 		int numEntries = 0;
 
 		// Enumerate each entry
 		for (Enumeration entries = zf.entries(); entries.hasMoreElements();) {
 			numEntries++;
 			String zipEntryName = ((ZipEntry) entries.nextElement()).getName();
 			assertTrue("Unknown Entry: " + zipEntryName, zipEntryName.compareTo(monitorFile.getName()) == 0);// ||
 			// zipEntryName.compareTo(logFile.getName())
 			// ==
 			// 0);
 		}
 		assertEquals("Results not correct size", 1, numEntries);
 
 		// check the length of the zip
 		// long fileLength = monitorFile.length() + logFile.length();
 		// if(monitorFile.length() != 0 || logFile.length() != 0)
 		// assertTrue("Zip didn't help", fileLength > zipFile.length());
 
 		// delete it
 		zipFile.delete();
 	}
 
 	public void testCreateLargeUploadPackage() throws IOException, InterruptedException {
 
 		for (int i = 0; i < 20000; i++) {
 			MylarMonitorPlugin.getDefault().notifyInteractionObserved(mockSelection());
 		}
 		testCreateUploadPackage();
 	}
 
 }
