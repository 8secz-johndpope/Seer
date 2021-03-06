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
 package org.eclipse.orion.server.git;
 
 import java.io.File;
 import java.io.IOException;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.util.Map.Entry;
 import javax.servlet.http.HttpServletRequest;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
 import org.eclipse.core.runtime.*;
 import org.eclipse.core.runtime.preferences.IEclipsePreferences;
 import org.eclipse.core.runtime.preferences.InstanceScope;
 import org.eclipse.jgit.api.Git;
 import org.eclipse.jgit.lib.*;
 import org.eclipse.jgit.storage.file.FileRepository;
 import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
 import org.eclipse.orion.internal.server.core.IWebResourceDecorator;
 import org.eclipse.orion.internal.server.servlets.ProtocolConstants;
 import org.eclipse.orion.internal.server.servlets.workspace.WebProject;
 import org.eclipse.orion.server.core.LogHelper;
 import org.eclipse.orion.server.git.servlets.*;
 import org.json.*;
 
 /**
  * Adds links to workspace and file resources referring to related git resources.
  */
 public class GitFileDecorator implements IWebResourceDecorator {
 
 	@Override
 	public void addAtributesFor(HttpServletRequest request, URI resource, JSONObject representation) {
 		IPath targetPath = new Path(resource.getPath());
 		if (targetPath.segmentCount() <= 1)
 			return;
 		String servlet = targetPath.segment(0);
 		if (!"file".equals(servlet) && !"workspace".equals(servlet)) //$NON-NLS-1$ //$NON-NLS-2$
 			return;
 
 		boolean isWorkspace = ("workspace".equals(servlet)); //$NON-NLS-1$
 
 		try {
 			if (isWorkspace && "POST".equals(request.getMethod())) {
 				String contentLocation = representation.getString(ProtocolConstants.KEY_CONTENT_LOCATION);
 				IPath path = new Path(new URI(contentLocation).getPath());
 
 				// initialize a new git repository on project creation if specified by configuration
 				initGitRepository(request, targetPath, representation);
 
 				if (GitUtils.getGitDir(path) != null)
 					addGitLinks(new URI(contentLocation), representation);
 				return;
 			}
 
 			if (isWorkspace && "GET".equals(request.getMethod())) {
 				JSONArray children = representation.optJSONArray(ProtocolConstants.KEY_CHILDREN);
 				if (children != null) {
 					for (int i = 0; i < children.length(); i++) {
 						JSONObject child = children.getJSONObject(i);
 						String location = child.getString(ProtocolConstants.KEY_LOCATION);
 						IPath path = new Path(new URI(location).getPath());
 						if (GitUtils.getGitDir(path) != null)
 							addGitLinks(new URI(location), child);
 					}
 				}
 				return;
 			}
 
 			if (!isWorkspace && "GET".equals(request.getMethod())) {
 				if (GitUtils.getGitDir(targetPath) != null) {
 					addGitLinks(resource, representation);
 
 					// assumption that Git resources may live only under another Git resource
 					JSONArray children = representation.optJSONArray(ProtocolConstants.KEY_CHILDREN);
 					if (children != null) {
 						for (int i = 0; i < children.length(); i++) {
 							JSONObject child = children.getJSONObject(i);
 							String location = child.getString(ProtocolConstants.KEY_LOCATION);
 							addGitLinks(new URI(location), child);
 						}
 					}
 				}
 			}
 		} catch (Exception e) {
 			// log and continue
 			LogHelper.log(e);
 		}
 	}
 
 	private void addGitLinks(URI location, JSONObject representation) throws URISyntaxException, JSONException, CoreException, IOException {
 		JSONObject gitSection = new JSONObject();
 		IPath targetPath = new Path(location.getPath());
 
 		// add Git Diff URI
 		IPath path = new Path(GitServlet.GIT_URI + '/' + GitConstants.DIFF_RESOURCE + '/' + GitConstants.KEY_DIFF_DEFAULT).append(targetPath);
 		URI link = new URI(location.getScheme(), location.getAuthority(), path.toString(), null, null);
 		gitSection.put(GitConstants.KEY_DIFF, link.toString());
 
 		// add Git Status URI
 		path = new Path(GitServlet.GIT_URI + '/' + GitConstants.STATUS_RESOURCE).append(targetPath);
 		link = new URI(location.getScheme(), location.getAuthority(), path.toString(), null, null);
 		gitSection.put(GitConstants.KEY_STATUS, link.toString());
 
 		// add Git Index URI
 		path = new Path(GitServlet.GIT_URI + '/' + GitConstants.INDEX_RESOURCE).append(targetPath);
 		link = new URI(location.getScheme(), location.getAuthority(), path.toString(), null, null);
 		gitSection.put(GitConstants.KEY_INDEX, link.toString());
 
 		// add Git Commit URI
 		path = new Path(GitServlet.GIT_URI + '/' + GitConstants.COMMIT_RESOURCE).append(Constants.HEAD).append(targetPath);
 		link = new URI(location.getScheme(), location.getAuthority(), path.toString(), null, null);
 		gitSection.put(GitConstants.KEY_COMMIT, link.toString());
 
 		// add Git Remote URI
 		path = new Path(GitServlet.GIT_URI + '/' + GitConstants.REMOTE_RESOURCE).append(targetPath);
 		link = new URI(location.getScheme(), location.getAuthority(), path.toString(), null, null);
 		gitSection.put(GitConstants.KEY_REMOTE, link.toString());
 
 		// add Git Default Remote Branch URI
 		File gitDir = GitUtils.getGitDir(targetPath);
 		Repository db = new FileRepository(gitDir);
 
 		for (Entry<String, Ref> refEntry : db.getRefDatabase().getRefs(Constants.R_REMOTES).entrySet()) {
 			if (!refEntry.getValue().isSymbolic()) {
 				Ref ref = refEntry.getValue();
 				String shortName = Repository.shortenRefName(ref.getName());
 				if (db.getBranch().equals(shortName.substring(Constants.DEFAULT_REMOTE_NAME.length() + 1))) {
 					link = new URI(location.getScheme(), location.getAuthority(), GitRemoteHandlerV1.baseToRemoteLocation(link, 2, shortName).getPath(), null, null);
 					gitSection.put(GitConstants.KEY_DEFAULT_REMOTE_BRANCH, link);
 					break;
 				}
 			}
 		}
 
 		// add Git Tag URI
 		path = new Path(GitServlet.GIT_URI + '/' + GitConstants.TAG_RESOURCE).append(targetPath);
 		link = new URI(location.getScheme(), location.getAuthority(), path.toString(), null, null);
 		gitSection.put(GitConstants.KEY_TAG, link.toString());
 
 		representation.put(GitConstants.KEY_GIT, gitSection);
 	}
 
 	/**
 	 * If this  server is configured to use git by default, then each project creation that is not
 	 * already in a repository needs to have a git -init performed to initialize the repository.
 	 */
 	private void initGitRepository(HttpServletRequest request, IPath targetPath, JSONObject representation) throws IOException {
 		//project creation URL is of the form POST /workspace/<id>
 		if (!(targetPath.segmentCount() == 2 && "workspace".equals(targetPath.segment(0)) && "POST".equals(request.getMethod()))) //$NON-NLS-1$ //$NON-NLS-2$
 			return;
 		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode("org.eclipse.orion.server.configurator"); //$NON-NLS-1$
 		String scm = preferences.get("orion.project.defaultSCM", "").toLowerCase(); //$NON-NLS-1$ //$NON-NLS-2$
 		if (!"git".equals(scm)) //$NON-NLS-1$
 			return;
		try {
			IFileStore store = WebProject.fromId(representation.optString("Id")).getProjectStore();
			//create repository in each project if it doesn't already exist
			File localFile = store.toLocalFile(EFS.NONE, null);
			File gitDir = GitUtils.getGitDirForFile(localFile);
			if (gitDir == null) {
				gitDir = new File(localFile, Constants.DOT_GIT);
				FileRepository repo = new FileRepositoryBuilder().setGitDir(gitDir).build();
				repo.create();
				//we need to perform an initial commit to workaround JGit bug 339610.
				Git git = new Git(repo);
 				git.add().addFilepattern(".").call();
 				git.commit().setMessage("Initial commit").call();
 			}
		} catch (Exception e) {
			//just log it - this is not the purpose of the file decorator
			LogHelper.log(e);
 		}
 	}
 }
