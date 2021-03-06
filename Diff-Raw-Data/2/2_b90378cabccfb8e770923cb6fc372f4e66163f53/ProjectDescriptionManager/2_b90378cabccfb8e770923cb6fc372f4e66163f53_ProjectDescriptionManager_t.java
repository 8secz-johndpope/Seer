 package org.eclipse.team.internal.ccvs.core.util;
 
 /*
  * (c) Copyright IBM Corp. 2000, 2001.
  * All Rights Reserved.
  */
  
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.DataInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 
 import org.apache.xerces.parsers.SAXParser;
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IProjectDescription;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.IResourceChangeEvent;
 import org.eclipse.core.resources.IResourceChangeListener;
 import org.eclipse.core.resources.IResourceDelta;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Path;
 import org.eclipse.team.ccvs.core.CVSProviderPlugin;
 import org.eclipse.team.ccvs.core.CVSTeamProvider;
 import org.eclipse.team.core.ITeamProvider;
 import org.eclipse.team.core.TeamException;
 import org.eclipse.team.core.TeamPlugin;
 import org.eclipse.team.internal.ccvs.core.CVSException;
 import org.eclipse.team.internal.ccvs.core.Policy;
 import org.xml.sax.InputSource;
 import org.xml.sax.SAXException;
 
 /**
  * This class handles the updating of the .vcm_meta file in projects managed by the CVSTeamProvider.
  * 
  * It does so by listening to deltas on the project description and the .vcm_meta file itself.
  * 
  */
 public class ProjectDescriptionManager {
 
 	public final static IPath PROJECT_DESCRIPTION_PATH = new Path(".vcm_meta");
 	public final static boolean UPDATE_PROJECT_DESCRIPTION_ON_LOAD = true;
 
 	/*
 	 * Read the projetc meta file into the provider description
 	 */
 	public static void readProjectDescription(IProjectDescription desc, InputStream stream) throws IOException, CVSException {
 		SAXParser parser = new SAXParser();
 		parser.setContentHandler(new ProjectDescriptionContentHandler(desc));
 		try {
 			parser.parse(new InputSource(stream));
 		} catch (SAXException ex) {
 			throw new CVSException(IStatus.ERROR, IStatus.ERROR, Policy.bind("ProjectDescriptionManager.unableToReadDescription"), ex);
 		}
 	}
 
 	public static void writeProjectDescription(IProject project, IProgressMonitor progress) throws CVSException {
 			
 		// generate the new contents of the project meta file into a string
 		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
 		String newContents = null;
 		try {
 			IProjectDescription desc = project.getDescription();
 			ProjectDescriptionWriter.writeProjectDescription(desc, byteOutputStream);
 			byteOutputStream.close();
 			newContents = byteOutputStream.toString("UTF8");
 		} catch (IOException ex) {
 			throw CVSException.wrapException(project, Policy.bind("ProjectDescriptionManager.ioDescription"), ex);
 		} catch (CoreException ex) {
 			throw CVSException.wrapException(project, Policy.bind("ProjectDescriptionManager.coreDescription"), ex);
 		}
 
 		IFile descResource = project.getFile(PROJECT_DESCRIPTION_PATH);
 		if (descResource.exists()) {
 			// check if the existing contents are the same before rewriting
 			String oldContents = null;
 			try {
 				StringBuffer stringBuffer = new StringBuffer();
 				InputStream is = ((IFile) descResource).getContents();
 				byte[] buf = new byte[512];
 				int result = is.read(buf);
 				while (result != -1) {
 					stringBuffer.append(new String(buf, 0, result, "UTF8"));
 					result = is.read(buf);
 				}
 				oldContents = stringBuffer.toString();
 				is.close();
 			} catch (IOException ex) {
 				throw CVSException.wrapException(project, Policy.bind("ProjectDescriptionManager.ioDescription"), ex);
 			} catch (CoreException ex) {
 				throw CVSException.wrapException(project, Policy.bind("ProjectDescriptionManager.coreDescription"), ex);
 			}
 
 			if (oldContents.equals(newContents)) {
 				// the contents of the new file would be the same as the old
 				return;
 			}
 			try {
 				descResource.setContents(
 					new ByteArrayInputStream(byteOutputStream.toByteArray()),
 					false,
 					false,
 					progress);
 			} catch (CoreException ex) {
 				throw CVSException.wrapException(project, Policy.bind("ProjectDescriptionManager.coreDescription"), ex);
 			}
 		} else {
 			try {
 				descResource.create(
 					new ByteArrayInputStream(byteOutputStream.toByteArray()),
 					false,
 					progress);
 			} catch (CoreException ex) {
 				throw CVSException.wrapException(descResource, Policy.bind("ProjectDescriptionManager.coreDescription"), ex);
 			}
 
 		}
 	}
 
 	/*
 	 * To be called whenever the project description file is believed to have changed by
 	 * a load/loadIfIncoming operation.
 	 */
 	public static void updateProjectIfNecessary(IProject project, IProgressMonitor progress) throws CoreException {
 		IFile descResource = project.getFile(PROJECT_DESCRIPTION_PATH);
 		if (descResource.exists() && UPDATE_PROJECT_DESCRIPTION_ON_LOAD) {
 			// update project description file (assuming it has changed)
 			IProjectDescription desc = project.getDescription();
 			DataInputStream is = null;
 			try {
 				is = new DataInputStream(((IFile) descResource).getContents());
 				try {
 					readProjectDescription(desc, is);
 				} finally {
 					is.close();
 				}
 				project.setDescription(desc, progress);
 				// Make sure we have the cvs nature (the above read may have removed it)
 				if (!project.getDescription().hasNature(CVSProviderPlugin.NATURE_ID)) {
 					TeamPlugin.getManager().setProvider(project, CVSProviderPlugin.NATURE_ID, null, progress);
 					writeProjectDescription(project, progress);
 				}
 			} catch(TeamException ex) {
 				Util.logError(Policy.bind("ProjectDescriptionManager.unableToReadDescription"), ex);
 				// something went wrong, delete the project description file
 				descResource.delete(true, progress);
 			} catch (IOException ex) {
 				Util.logError(Policy.bind("ProjectDescriptionManager.unableToReadDescription"), ex);
 				// something went wrong, delete the project description file
 				descResource.delete(true, progress);
 			} catch (CoreException ex) {
 				Util.logError(Policy.bind("ProjectDescriptionManager.unableToReadDescription"), ex);
 				// something went wrong, delete the project description file
 				descResource.delete(true, progress);
 			}
 		}
 	}
 
 	/* 
 	 * Write out the project description file.
 	 * 
 	 * For now just do it. It would be nice to detect the proper conditions
 	 * 
 	 */
 	public static void writeProjectDescriptionIfNecessary(CVSTeamProvider provider, IResource resource, IProgressMonitor monitor) throws CVSException {
 		if (resource.getType() == IResource.PROJECT || isProjectDescription(resource)) {
 			IProject project = resource.getProject();
 			writeProjectDescription(project, monitor);
 		}
 	}
 
 	public static boolean isProjectDescription(IResource resource) {
 		return resource.getProjectRelativePath().equals(PROJECT_DESCRIPTION_PATH);
 	}
 
 	public static void initializeChangeListener() {
 		IResourceChangeListener changeListener = new IResourceChangeListener() {
 			public void resourceChanged(IResourceChangeEvent event) {
 				try {
 					IResourceDelta root = event.getDelta();
					IResourceDelta[] projectDeltas = root.getAffectedChildren(IResourceDelta.CHANGED | IResourceDelta.ADDED);
 					for (int i = 0; i < projectDeltas.length; i++) {
 						IResourceDelta delta = projectDeltas[i];
 						IResource resource = delta.getResource();
 						if (resource.getType() == IResource.PROJECT) {
 							IProject project = (IProject)resource;
 							ITeamProvider provider = TeamPlugin.getManager().getProvider(project);
 							if (! (provider instanceof CVSTeamProvider))
 								continue;
 							// First, check if the .vcm_meta file for the project is in the delta.
 							IResourceDelta[] children = delta.getAffectedChildren(IResourceDelta.REMOVED | IResourceDelta.ADDED | IResourceDelta.CHANGED);
 							for (int j = 0; j < children.length; j++) {
 								IResourceDelta childDelta = children[j];
 								IResource childResource = childDelta.getResource();
 								if (isProjectDescription(childResource))
 									switch (childDelta.getKind()) {
 										case IResourceDelta.REMOVED:
 											writeProjectDescriptionIfNecessary((CVSTeamProvider)provider, project, Policy.monitorFor(null));
 											return; // The file and description are in sync
 										case IResourceDelta.CHANGED:
 										case IResourceDelta.ADDED:
 											updateProjectIfNecessary(project, Policy.monitorFor(null));
 											return; // The file and description are in sync
 									}
 							}
 							// Check if we didn't do anything above and the project description changed.
 							if ((delta.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
 								writeProjectDescriptionIfNecessary((CVSTeamProvider)provider, project, Policy.monitorFor(null));
 							}
 						}
 					}
 				} catch (CVSException ex) {
 					Util.logError("Cannot update project description", ex);
 				} catch (CoreException ex) {
 					Util.logError("Cannot update project description", ex);
 				} 
 			}
 		};
 		ResourcesPlugin.getWorkspace().addResourceChangeListener(changeListener, IResourceChangeEvent.PRE_AUTO_BUILD);
 	}
 }
