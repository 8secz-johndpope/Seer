 package org.eclipse.team.internal.ccvs.ui.actions;
 
 /*
  * (c) Copyright IBM Corp. 2000, 2001.
  * All Rights Reserved.
  */
 
 import java.lang.reflect.InvocationTargetException;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.Iterator;
 
 import java.util.Set;
 import org.eclipse.core.runtime.IAdaptable;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.jface.action.IAction;
 import org.eclipse.jface.operation.IRunnableWithProgress;
 import org.eclipse.jface.viewers.ISelection;
 import org.eclipse.jface.viewers.IStructuredSelection;
 import org.eclipse.team.ccvs.core.ILogEntry;
 import org.eclipse.team.ccvs.core.ICVSRemoteFile;
 import org.eclipse.team.ccvs.core.ICVSRemoteResource;
 import org.eclipse.team.ccvs.core.ICVSRepositoryLocation;
 import org.eclipse.team.core.TeamException;
 import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
 import org.eclipse.team.internal.ccvs.ui.Policy;
 import org.eclipse.team.internal.ccvs.ui.RemoteFileEditorInput;
 import org.eclipse.team.internal.ccvs.ui.RepositoryManager;
 import org.eclipse.team.internal.ccvs.ui.model.Tag;
 import org.eclipse.team.ui.actions.TeamAction;
 import org.eclipse.ui.IWorkbenchPage;
 import org.eclipse.ui.PartInitException;
 
 public class AutoDefineTagsAction extends TeamAction {
 	/**
 	 * Returns the selected remote files
 	 */
 	protected ICVSRemoteFile[] getSelectedRemoteFiles() {
 		ArrayList resources = null;
 		if (!selection.isEmpty()) {
 			resources = new ArrayList();
 			Iterator elements = ((IStructuredSelection) selection).iterator();
 			while (elements.hasNext()) {
 				Object next = elements.next();
 				if (next instanceof ICVSRemoteFile) {
 					resources.add(next);
 					continue;
 				}
 				if (next instanceof IAdaptable) {
 					IAdaptable a = (IAdaptable) next;
 					Object adapter = a.getAdapter(ICVSRemoteFile.class);
 					if (adapter instanceof ICVSRemoteFile) {
 						resources.add(adapter);
 						continue;
 					}
 				}
 			}
 		}
 		if (resources != null && !resources.isEmpty()) {
 			ICVSRemoteFile[] result = new ICVSRemoteFile[resources.size()];
 			resources.toArray(result);
 			return result;
 		}
 		return new ICVSRemoteFile[0];
 	}
 	/*
 	 * @see IActionDelegate#run(IAction)
 	 */
 	public void run(IAction action) {
 		run(new IRunnableWithProgress() {
 			public void run(IProgressMonitor monitor) throws InvocationTargetException {
 				RepositoryManager manager = CVSUIPlugin.getPlugin().getRepositoryManager();
 				ICVSRemoteFile[] files = getSelectedRemoteFiles();
 				for (int i = 0; i < files.length; i++) {
 					ICVSRemoteFile file = files[i];
 					ICVSRepositoryLocation root = getRemoteRoot(file);
 					Set tagSet = new HashSet();
 					ILogEntry[] entries = null;
 					try {
 						entries = file.getLogEntries(monitor);
 					} catch (TeamException e) {
 						CVSUIPlugin.log(e.getStatus());
 						return;
 					}
 					for (int j = 0; j < entries.length; j++) {
						String[] tags = entries[i].getTags();
 						for (int k = 0; k < tags.length; k++) {
 							tagSet.add(tags[k]);
 						}
 					}
 					Iterator it = tagSet.iterator();
 					while (it.hasNext()) {
 						manager.addTag(root, new Tag((String)it.next(), root));
 					}
 				}
 			}
 		}, Policy.bind("AutoDefineTagsAction.defineTags"), this.PROGRESS_DIALOG);
 	}
 	/*
 	 * @see TeamAction#isEnabled()
 	 */
 	protected boolean isEnabled() throws TeamException {
 		ICVSRemoteFile[] resources = getSelectedRemoteFiles();
 		if (resources.length == 0) return false;
 		return true;
 	}
 	protected ICVSRepositoryLocation getRemoteRoot(ICVSRemoteFile file) {
 		return file.getRepository();
 	}
 }
