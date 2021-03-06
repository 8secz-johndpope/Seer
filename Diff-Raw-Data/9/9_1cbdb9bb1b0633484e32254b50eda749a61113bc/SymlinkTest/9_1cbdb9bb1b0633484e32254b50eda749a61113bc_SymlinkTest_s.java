 /*******************************************************************************
  * Copyright (c) 2007 Wind River Systems, Inc. and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  * Martin Oberhuber (Wind River) - initial API and implementation
  *******************************************************************************/
 package org.eclipse.core.tests.filesystem;
 
 import java.io.*;
 import org.eclipse.core.filesystem.*;
 import org.eclipse.core.resources.IWorkspace;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.Platform;
 
 /**
  * 
  */
 public class SymlinkTest extends FileSystemTest {
 	private static String specialCharName = "   "; //$NON-NLS-1$
 
 	protected IFileStore aDir, aFile; //actual Dir, File
 	protected IFileInfo iDir, iFile, ilDir, ilFile, illDir, illFile;
 	protected IFileStore lDir, lFile; //symlink to Dir, File
 	protected IFileStore llDir, llFile; //link to link to Dir, File
 
 	public static IFileSystem getFileSystem() {
 		try {
 			return EFS.getFileSystem(EFS.SCHEME_FILE);
 		} catch (CoreException e) {
 			fail("getFileSystem", e);
 		}
 		return null;
 	}
 
 	public static IWorkspace getWorkspace() {
 		return ResourcesPlugin.getWorkspace();
 	}
 
 	public static boolean isTestablePlatform() {
 		// A Platform is testable if it supports the "ln -s" command.
 		String os = Platform.getOS();
 		//currently we only support linux
 		if (os.equals(Platform.OS_LINUX)
 		//		  ||os.equals(Platform.OS_AIX)
 		//		  ||os.equals(Platform.OS_HPUX)
 		//		  ||os.equals(Platform.OS_SOLARIS)
 		//		  ||isWindowsVista()
 		) {
 			return true;
 		}
 		return false;
 	}
 
 	public static boolean isWindowsVista() {
 		return Platform.getOS().equals(Platform.OS_WIN32) && "6.0".equals(System.getProperty("org.osgi.framework.os.version")); //$NON-NLS-1$ //$NON-NLS-2$
 	}
 
 	protected void fetchFileInfos() {
 		iDir = aDir.fetchInfo();
 		iFile = aFile.fetchInfo();
 		ilDir = lDir.fetchInfo();
 		ilFile = lFile.fetchInfo();
 		illDir = llDir.fetchInfo();
 		illFile = llFile.fetchInfo();
 	}
 
 	public boolean haveSymlinks() {
 		return (getFileSystem().attributes() & EFS.ATTRIBUTE_SYMLINK) != 0;
 	}
 
 	protected void makeLinkStructure() {
 		aDir = baseStore.getChild("aDir");
 		aFile = baseStore.getChild("aFile");
 		lDir = baseStore.getChild("lDir");
 		lFile = baseStore.getChild("lFile");
 		llDir = baseStore.getChild("llDir");
 		llFile = baseStore.getChild("llFile");
 		ensureExists(aDir, true);
 		ensureExists(aFile, false);
 		mkLink(baseStore, "lDir", "aDir", true);
 		mkLink(baseStore, "llDir", "lDir", true);
 		mkLink(baseStore, "lFile", "aFile", false);
 		mkLink(baseStore, "llFile", "lFile", false);
 		fetchFileInfos();
 	}
 
 	protected void mkLink(IFileStore dir, String src, String tgt, boolean isDir) {
 		String[] envp = {};
 		try {
 			Process p;
 			File basedir = baseStore.toLocalFile(EFS.NONE, getMonitor());
 			if (isWindowsVista()) {
 				if (isDir) {
 					String[] cmd = {"mklink", "/d", src, tgt};
 					p = Runtime.getRuntime().exec(cmd, envp, basedir);
 				} else {
 					String[] cmd = {"mklink", src, tgt};
 					p = Runtime.getRuntime().exec(cmd, envp, basedir);
 				}
 			} else {
 				String[] cmd = {"ln", "-s", tgt, src};
 				p = Runtime.getRuntime().exec(cmd, envp, basedir);
 			}
 			int exitcode = p.waitFor();
 			assertEquals(exitcode, 0);
 		} catch (IOException e) {
 			fail("mkLink", e);
 		} catch (CoreException e) {
 			fail("mkLink", e);
 		} catch (InterruptedException e) {
 			fail("mkLink", e);
 		}
 	}
 
 	protected void setUp() throws Exception {
 		baseStore = getFileSystem().getStore(getWorkspace().getRoot().getLocation().append("temp"));
 		baseStore.mkdir(EFS.NONE, null);
 	}
 
 	protected void tearDown() throws Exception {
 		baseStore.delete(EFS.NONE, null);
 	}
 
 	public void testBrokenSymlinkAttributes() {
 		if (!isTestablePlatform()) {
 			return;
 		}
 		makeLinkStructure();
 		//break links by removing actual dir and file
 		ensureDoesNotExist(aDir);
 		ensureDoesNotExist(aFile);
 		fetchFileInfos();
 
 		assertFalse(ilFile.exists());
 		assertFalse(ilFile.isDirectory());
 		assertFalse(illFile.exists());
 		assertFalse(illFile.isDirectory());
 		assertFalse(ilDir.exists());
 		assertFalse(ilDir.isDirectory());
 		assertFalse(illDir.exists());
 		assertFalse(illDir.isDirectory());
 		assertEquals(ilFile.getLastModified(), 0);
 		assertEquals(ilFile.getLength(), 0);
 		assertEquals(ilDir.getLastModified(), 0);
 		assertEquals(ilDir.getLength(), 0);
 		assertEquals(illFile.getLastModified(), 0);
 		assertEquals(illFile.getLength(), 0);
 		assertEquals(illDir.getLastModified(), 0);
 		assertEquals(illDir.getLength(), 0);
 		if (haveSymlinks()) {
 			assertTrue(ilFile.getAttribute(EFS.ATTRIBUTE_SYMLINK));
 			assertEquals(ilFile.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "aFile");
 			assertTrue(ilDir.getAttribute(EFS.ATTRIBUTE_SYMLINK));
 			assertEquals(ilDir.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "aDir");
 			assertTrue(illFile.getAttribute(EFS.ATTRIBUTE_SYMLINK));
 			assertEquals(illFile.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lFile");
 			assertTrue(illDir.getAttribute(EFS.ATTRIBUTE_SYMLINK));
 			assertEquals(illDir.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lDir");
 		}
 	}
 
 	public void testBrokenSymlinkRemove() throws Exception {
 		//removing a broken symlink is possible
 		if (!isTestablePlatform()) {
 			return;
 		}
 		makeLinkStructure();
 		ensureDoesNotExist(aFile);
 		ensureDoesNotExist(aDir);
 		IFileInfo[] infos = baseStore.childInfos(EFS.NONE, getMonitor());
 		assertEquals(infos.length, 4);
 		llFile.delete(EFS.NONE, getMonitor());
 		llDir.delete(EFS.NONE, getMonitor());
 		infos = baseStore.childInfos(EFS.NONE, getMonitor());
 		assertEquals(infos.length, 2);
 		lFile.delete(EFS.NONE, getMonitor());
 		lDir.delete(EFS.NONE, getMonitor());
 		infos = baseStore.childInfos(EFS.NONE, getMonitor());
 		assertEquals(infos.length, 0);
 	}
 
 	public void testRecursiveSymlink() throws Exception {
 		if (!isTestablePlatform())
 			return;
 mkLink(baseStore, "l1", "l2", false);
 		mkLink(baseStore, "l2", "l1", false);
 		IFileStore l1 = baseStore.getChild("l1");
 		IFileInfo i1 = l1.fetchInfo();
 		assertFalse(i1.exists());
 		assertFalse(i1.isDirectory());
 		if (haveSymlinks()) {
 			assertTrue(i1.getAttribute(EFS.ATTRIBUTE_SYMLINK));
 			assertEquals("l2", i1.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET));
 		}
 		IFileInfo[] infos = baseStore.childInfos(EFS.NONE, getMonitor());
 		assertEquals(infos.length, 2);
 		i1.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
 		boolean exceptionThrown = false;
 		try {
 			l1.putInfo(i1, EFS.SET_ATTRIBUTES, getMonitor());
 		} catch (CoreException ce) {
 			exceptionThrown = true;
 		}
 		i1 = l1.fetchInfo();
		//FIXME bug: putInfo neither sets attributes nor throws an exception for broken symbolic links
		//assertTrue(exceptionThrown);
		//assertTrue(i1.getAttribute(EFS.ATTRIBUTE_READ_ONLY));
 		assertFalse(i1.exists());
 
 		i1.setLastModified(12345);
 		exceptionThrown = false;
 		try {
 			l1.putInfo(i1, EFS.SET_LAST_MODIFIED, getMonitor());
 		} catch (CoreException ce) {
 			exceptionThrown = true;
 		}
 		i1 = l1.fetchInfo();
 		//FIXME bug: putInfo neither sets attributes nor throws an exception for broken symbolic links
 		//assertTrue(exceptionThrown);
 		//assertEquals(i1.getLastModified(), 12345);
 		assertFalse(i1.exists());
 
 		l1.delete(EFS.NONE, getMonitor());
 		infos = baseStore.childInfos(EFS.NONE, getMonitor());
 		assertEquals(infos.length, 1);
 	}
 
 	public void testSymlinkAttributes() {
 		if (!isTestablePlatform()) {
 			return;
 		}
 		makeLinkStructure();
 		assertFalse(iFile.getAttribute(EFS.ATTRIBUTE_SYMLINK));
 		assertFalse(iDir.getAttribute(EFS.ATTRIBUTE_SYMLINK));
 		//valid links
 		assertTrue(ilFile.exists());
 		assertFalse(ilFile.isDirectory());
 		assertTrue(illFile.exists());
 		assertFalse(illFile.isDirectory());
 		assertTrue(ilDir.exists());
 		assertTrue(ilDir.isDirectory());
 		assertTrue(illDir.exists());
 		assertTrue(illDir.isDirectory());
 		assertEquals(iFile.getLastModified(), illFile.getLastModified());
 		assertEquals(iFile.getLength(), illFile.getLength());
 		assertEquals(iDir.getLastModified(), illDir.getLastModified());
 		assertEquals(iDir.getLength(), illDir.getLength());
 		if (haveSymlinks()) {
 			assertTrue(ilFile.getAttribute(EFS.ATTRIBUTE_SYMLINK));
 			assertEquals(ilFile.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "aFile");
 			assertTrue(ilDir.getAttribute(EFS.ATTRIBUTE_SYMLINK));
 			assertEquals(ilDir.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "aDir");
 			assertTrue(illFile.getAttribute(EFS.ATTRIBUTE_SYMLINK));
 			assertEquals(illFile.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lFile");
 			assertTrue(illDir.getAttribute(EFS.ATTRIBUTE_SYMLINK));
 			assertEquals(illDir.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lDir");
 		}
 	}
 
 	public void testSymlinkDirRead() throws Exception {
 		//reading from a directory pointed to by a link is possible
 		if (!isTestablePlatform()) {
 			return;
 		}
 		makeLinkStructure();
 		IFileStore childDir = aDir.getChild("subDir");
 		ensureExists(childDir, true);
 		IFileInfo[] infos = llDir.childInfos(EFS.NONE, getMonitor());
 		assertEquals(infos.length, 1);
 		assertTrue(infos[0].isDirectory());
 		assertFalse(infos[0].getAttribute(EFS.ATTRIBUTE_SYMLINK));
 		assertNull(infos[0].getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET));
 		assertEquals(infos[0].getName(), "subDir");
 		ensureDoesNotExist(childDir);
 	}
 
 	public void testSymlinkDirWrite() throws Exception {
 		//writing to symlinked dir
 		if (!isTestablePlatform()) {
 			return;
 		}
 		makeLinkStructure();
 		IFileStore childFile = llDir.getChild("subFile");
 		ensureExists(childFile, false);
 		IFileInfo[] infos = aDir.childInfos(EFS.NONE, getMonitor());
 		assertEquals(infos.length, 1);
 		assertFalse(infos[0].isDirectory());
 		assertFalse(infos[0].getAttribute(EFS.ATTRIBUTE_SYMLINK));
 		assertNull(infos[0].getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET));
 		assertEquals(infos[0].getName(), "subFile");
 
 		//writing to broken symlink
 		ensureDoesNotExist(aDir);
 		childFile = llDir.getChild("subFile");
 		OutputStream out = null;
 		boolean exceptionThrown = false;
 		try {
 			out = childFile.openOutputStream(EFS.NONE, getMonitor());
 		} catch (CoreException ce) {
 			exceptionThrown = true;
 		}
 		if (out != null)
 			out.close();
 		assertNull(out);
 		assertTrue(exceptionThrown);
 	}
 
 	public void testSymlinkEnabled() {
 		if (Platform.OS_LINUX.equals(Platform.getOS())) {
 			assertTrue(haveSymlinks());
 		} else {
 			assertFalse(haveSymlinks());
 		}
 	}
 
 	public void testSymlinkExtendedChars() throws Exception {
 		if (!isTestablePlatform())
 			return;
 		IFileStore childDir = baseStore.getChild(specialCharName);
 		ensureExists(childDir, true);
 		IFileStore childFile = baseStore.getChild("ff" + specialCharName);
 		ensureExists(childFile, false);
 		mkLink(baseStore, "l" + specialCharName, specialCharName, true);
 		mkLink(baseStore, "lf" + specialCharName, "ff" + specialCharName, false);
 		IFileInfo[] infos = baseStore.childInfos(EFS.NONE, getMonitor());
 		assertEquals(infos.length, 4);
 		for (int i = 0; i < infos.length; i++) {
 			assertTrue(infos[i].getName().endsWith(specialCharName));
 			assertTrue(infos[i].exists());
 			if (infos[i].getName().charAt(1) == 'f') {
 				assertFalse(infos[i].isDirectory());
 			} else {
 				assertTrue(infos[i].isDirectory());
 			}
 			if (haveSymlinks() && infos[i].getName().charAt(0) == 'l') {
 				assertTrue(infos[i].getAttribute(EFS.ATTRIBUTE_SYMLINK));
 				assertTrue(infos[i].getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET).endsWith(specialCharName));
 			}
 		}
 	}
 
 	public void testSymlinkPutInfo() throws Exception {
 		if (!isTestablePlatform()) {
 			return;
 		}
 		//check that putInfo() "writes through" the symlink
 		makeLinkStructure();
 		long oldTime = iFile.getLastModified();
 		long timeToSet = oldTime - 100000;
 		illFile.setLastModified(timeToSet);
 		illFile.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
 		llFile.putInfo(illFile, EFS.SET_ATTRIBUTES | EFS.SET_LAST_MODIFIED, getMonitor());
 		iFile = aFile.fetchInfo();
 		assertEquals(iFile.getLastModified(), timeToSet);
 		assertTrue(iFile.getAttribute(EFS.ATTRIBUTE_READ_ONLY));
 
 		oldTime = iDir.getLastModified();
 		timeToSet = oldTime - 100000;
 		illDir.setLastModified(timeToSet);
 		illDir.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
 		llDir.putInfo(illDir, EFS.SET_ATTRIBUTES | EFS.SET_LAST_MODIFIED, getMonitor());
 		iDir = aDir.fetchInfo();
 		assertTrue(iDir.getLastModified() != oldTime);
 		assertEquals(iDir.getLastModified(), timeToSet);
 		assertTrue(iDir.getAttribute(EFS.ATTRIBUTE_READ_ONLY));
 		if (haveSymlinks()) {
 			//check that link properties are maintained even through putInfo
 			illFile = llFile.fetchInfo();
 			illDir = llDir.fetchInfo();
 			assertTrue(illFile.getAttribute(EFS.ATTRIBUTE_SYMLINK));
 			assertTrue(illDir.getAttribute(EFS.ATTRIBUTE_SYMLINK));
 			assertEquals(illFile.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lFile");
 			assertEquals(illDir.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lDir");
 		}
 	}
 
 	public void testSymlinkRemove() throws Exception {
 		//removing a symlink keeps the link target intact.
 		//symlinks being broken due to remove are set to non-existant.
 		if (!isTestablePlatform()) {
 			return;
 		}
 		makeLinkStructure();
 		lFile.delete(EFS.NONE, getMonitor());
 		illFile = lFile.fetchInfo();
 		assertFalse(illFile.exists());
 		iFile = aFile.fetchInfo();
 		assertTrue(iFile.exists());
 
 		lDir.delete(EFS.NONE, getMonitor());
 		illDir = lDir.fetchInfo();
 		assertFalse(illFile.exists());
 		iDir = aDir.fetchInfo();
 		assertTrue(iDir.exists());
 	}
 
 }
