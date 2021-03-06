 /*******************************************************************************
  * Copyright (c) 2011 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.orion.server.git.servlets;
 
 import java.io.File;
 import org.eclipse.core.filesystem.EFS;
 import org.eclipse.core.filesystem.IFileStore;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.jgit.lib.Constants;
 import org.eclipse.jgit.lib.RepositoryCache;
 import org.eclipse.jgit.util.FS;
 import org.eclipse.orion.internal.server.servlets.file.NewFileServlet;
 
 public class GitUtils {
 	public static File getGitDir(IPath path, String authority) throws CoreException {
 		IPath p = path.removeFirstSegments(1);
 		IFileStore fileStore = NewFileServlet.getFileStore(p, authority);
 		File file = fileStore.toLocalFile(EFS.NONE, null);
 		if (file.exists()) {
 			while (file != null) {
 				if (RepositoryCache.FileKey.isGitRepository(file, FS.DETECTED)) {
 					return file;
 				} else if (RepositoryCache.FileKey.isGitRepository(new File(file, Constants.DOT_GIT), FS.DETECTED)) {
 					return new File(file, Constants.DOT_GIT);
 				}
 				file = file.getParentFile();
 			}
 		}
 		return null;
 	}
 }
