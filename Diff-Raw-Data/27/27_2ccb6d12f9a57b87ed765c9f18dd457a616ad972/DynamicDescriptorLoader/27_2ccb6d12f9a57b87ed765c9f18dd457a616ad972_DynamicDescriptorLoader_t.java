 package org.strategoxt.imp.metatooling.loading;
 
 import static org.eclipse.core.resources.IMarker.SEVERITY_ERROR;
 import static org.eclipse.core.resources.IResourceDelta.CONTENT;
 import static org.eclipse.core.resources.IResourceDelta.MOVED_FROM;
 import static org.eclipse.core.resources.IResourceDelta.MOVED_TO;
 import static org.eclipse.core.resources.IResourceDelta.REPLACED;
 
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.Queue;
 import java.util.Set;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.IResourceChangeEvent;
 import org.eclipse.core.resources.IResourceChangeListener;
 import org.eclipse.core.resources.IResourceDelta;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.resources.WorkspaceJob;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.core.runtime.jobs.Job;
 import org.spoofax.ArrayDeque;
 import org.spoofax.interpreter.terms.IStrategoTerm;
 import org.strategoxt.imp.metatooling.building.DynamicDescriptorBuilder;
 import org.strategoxt.imp.runtime.Environment;
 import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
 import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
 import org.strategoxt.imp.runtime.dynamicloading.DescriptorFactory;
 import org.strategoxt.imp.runtime.parser.ast.AstMessageHandler;
 
 /**
  * This class updates any editors in the environment,
  * triggered by resource change events.
  * 
  * @author Lennart Kats <lennart add lclnet.nl>
  */
 public class DynamicDescriptorLoader implements IResourceChangeListener {
 	
 	private static final int SCHEDULE_DELAY = 300;
 	
 	private static final DynamicDescriptorLoader instance = new DynamicDescriptorLoader();
 	
 	private final Queue<IResourceChangeEvent> asyncEventQueue =
 		new ArrayDeque<IResourceChangeEvent>();
 	
 	private final Set<String> asyncIgnoreOnce =
 		Collections.synchronizedSet(new HashSet<String>());
 	
 	private final AstMessageHandler asyncMessageHandler =
 		new AstMessageHandler(AstMessageHandler.ANALYSIS_MARKER_TYPE);
 	
 	private boolean isAsyncEventHandlerActive;
 	
 	private DynamicDescriptorLoader() {
 		// use getInstance() instead
 	}
 	
 	public static DynamicDescriptorLoader getInstance() {
 		return instance;
 	}
 	
 	/**
 	 * Loads the editor for the specified descriptor,
 	 * and ignore it at the next resource event arrives.
 	 */
 	public void forceUpdate(IResource resource) {
 		synchronized (Environment.getSyncRoot()) {
 			assert resource.toString().endsWith(".packed.esv");
 			forceNoUpdate(resource);
 			loadPackedDescriptor(resource);
 		}
 	}
 
 	/**
 	 * Ignores the specified descriptor at the next resource event arrives.
 	 */
 	public void forceNoUpdate(IResource resource) {
 		forceNoUpdate(resource.getFullPath().toString());
 	}
 
 	/**
 	 * Ignores the specified descriptor at the next resource event arrives.
 	 */
 	public void forceNoUpdate(String file) {
 		synchronized (Environment.getSyncRoot()) {
 			asyncIgnoreOnce.add(file);
 		}
 	}
 
 	public void resourceChanged(final IResourceChangeEvent event) {
 		if (event.getType() == IResourceChangeEvent.POST_CHANGE && isSignificantChange(event.getDelta())) {
 			// TODO: aggregate multiple events into a single job?
 			//       this seems to spawn way to many threads
 			synchronized (asyncEventQueue) {
 				asyncEventQueue.add(event);
 				if (!isAsyncEventHandlerActive)
 					startEventHandler(event);
 			}
 		}
 	}
 
 	private void startEventHandler(IResourceChangeEvent event) {
 		isAsyncEventHandlerActive = true;
		Job job = new WorkspaceJob("Loading editor descriptors") {
 			@Override
 			public IStatus runInWorkspace(IProgressMonitor monitor) {
 				// TODO: Finer-grained locking? (that seems to lead to deadlocks)
 				synchronized (Environment.getSyncRoot()) {
 					for (;;) {
 	 					IResourceChangeEvent event;
 						synchronized (asyncEventQueue) {
 							if (asyncEventQueue.isEmpty()) {
 								isAsyncEventHandlerActive = false;
 								return Status.OK_STATUS;
 							}
 							event = asyncEventQueue.remove();
 						}
 						// (monitor updates acquire display lock)
 						monitor.beginTask("Scanning workspace", IProgressMonitor.UNKNOWN);
 						postResourceChanged(event.getDelta(), monitor);
 					}
 				}
 			}
 		};
 		// UNDONE: locking the workspace is a clear deadlock risk
 		//         (so locking small parts is likely almost as bad)
 		// job.setRule(/*event.getResource()*/ResourcesPlugin.getWorkspace().getRoot());
 		job.schedule(SCHEDULE_DELAY);
 	}
 	
 	public void postResourceChanged(IResourceDelta delta, IProgressMonitor monitor) {
 		IResourceDelta[] children = delta.getAffectedChildren();
 		
 		if (children.length == 0) {		
 			IResource resource = delta.getResource();
 			if (isSignificantChange(delta))
 				updateResource(resource, monitor, false);
 		} else {
 			// Recurse
 			for (IResourceDelta child : children)
 				postResourceChanged(child, monitor);
 		}
 	}
 
 	private static boolean isSignificantChange(IResourceDelta delta) {
 		int flags = delta.getFlags();
 		return (flags & CONTENT) == CONTENT
 			|| (flags & MOVED_TO) == MOVED_TO
 			|| (flags & MOVED_FROM) == MOVED_FROM
 			|| (flags & REPLACED) == REPLACED
 			|| (flags == 0);
 	}
 	
 	public void updateResource(IResource resource, IProgressMonitor monitor, boolean startup) {
 		Environment.assertLock();
 		
 		String name = resource.getName();
 		if (name.endsWith(".packed.esv")) {
 			IResource source = getSourceDescriptor(resource);
 			
 			if (asyncIgnoreOnce.remove(resource.getFullPath().toString()))
 				return;
 			
 			if (!source.equals(resource) && source.exists() && !startup) {
 				// Try to build using the .main.esv instead;
 				// the build.xml file may touch the .packed.esv file
 				// to signal a rebuild is necessary
 				// TODO: Prevent duplicate builds triggered this way...?
 				DynamicDescriptorBuilder.getInstance().updateResource(source, monitor);
 			} else if (resource.exists()) {
 				monitor.setTaskName("Loading " + name);
 				loadPackedDescriptor(resource);
 				monitor.setTaskName(null);
 			}
 		} else if (name.endsWith(".tbl")) {
 			name = name.substring(0, name.length() - 4);
 			if (resource instanceof IFile)
 				Environment.registerUnmanagedParseTable(name, (IFile) resource);
 		}
 	}
 
 	public void loadPackedDescriptor(IResource descriptor) {
 		Environment.assertLock();
 		
 		// TODO2: Properly trace back descriptor errors to their original source
 		IResource source = getSourceDescriptor(descriptor);
 		try {
 			asyncMessageHandler.clearMarkers(source);
 			asyncMessageHandler.clearMarkers(descriptor);
 			
 			IFile file = descriptor.getProject().getFile(descriptor.getProjectRelativePath());
 			Descriptor result = DescriptorFactory.load(file, getSourceDescriptor(descriptor));
 			result.setDynamicallyLoaded(true);
 			
 		} catch (BadDescriptorException e) {
 			Environment.logException("Error in descriptor " + descriptor, e);
 			reportError(source, "Error in descriptor: " + e.getMessage(), null);
 			reportError(descriptor, e.getOffendingTerm(), "Error in descriptor " + descriptor + ": " + e.getMessage(), e);
 		} catch (IOException e) {
 			Environment.logException("Error reading descriptor " + descriptor, e);
 			reportError(source, "Internal error reading descriptor " + descriptor + ": " + e.getMessage(), e);
 		} catch (CoreException e) {
 			Environment.logException("Unable to load descriptor " + descriptor, e);
 			reportError(source, "Internal error loading descriptor " + descriptor + ": " + e.getMessage(), e);
 		} catch (RuntimeException e) {
 			Environment.logException("Unable to load descriptor " + descriptor, e);
 			reportError(source, "Internal error loading descriptor " + descriptor + ": " + e.getMessage(), e);
 		} catch (Error e) { // workspace thread swallows this >:(
 			Environment.logException("Unable to load descriptor " + descriptor, e);
 			reportError(source, "Internal error loading descriptor " + descriptor + ": " + e.getMessage(), e);
 			throw e;
 		} finally {
 			asyncMessageHandler.commitAllChanges();
 		}
 	}
 	
 	public static IResource getSourceDescriptor(IResource packedDescriptor) {
 		String name = packedDescriptor.getName();
 		if (name.endsWith(".main.esv")) return packedDescriptor;
 		name = name.substring(0, name.length() - ".packed.esv".length());
 		IResource result = packedDescriptor.getParent().getParent().findMember("editor/" + name + ".main.esv");
 		if (result == null) {
 			Environment.logException("Source descriptor not found", new FileNotFoundException("include/" + name + ".packed.esv"));
 			return packedDescriptor;
 		} else {
 			return result;
 		}
 	}
 	
 	public static String getSourceDescriptor(String packedDescriptor) {
 		File file = new File(packedDescriptor);
 		String name = file.getName();
 		if (name.endsWith(".main.esv")) return packedDescriptor;
 		name = name.substring(0, name.length() - ".packed.esv".length());
 		return file.getParentFile().getParent() + "/editor/" + name + ".main.esv";
 	}
 	
 	private void reportError(final IResource descriptor, final String message, Throwable exception) {
 		Environment.assertLock();
 		
 		if (exception != null)
 			Environment.asynOpenErrorDialog("Dynamic editor descriptor loading", message, exception);
 		
 		if (ResourcesPlugin.getWorkspace().isTreeLocked()) {
 			Job job = new WorkspaceJob("Add error marker") {
 				{ setSystem(true); } // don't show to user
 				@Override
 				public IStatus runInWorkspace(IProgressMonitor monitor) {
 					asyncMessageHandler.addMarkerFirstLine(descriptor, message, SEVERITY_ERROR);
 					return Status.OK_STATUS;
 				}
 			};
 			job.setRule(descriptor);
 			job.schedule();
 		} else {
 			asyncMessageHandler.addMarkerFirstLine(descriptor, message, SEVERITY_ERROR);
 		}
 	}
 	
 	private void reportError(final IResource descriptor, final IStrategoTerm offendingTerm, final String message, final Throwable exception) {
 		Environment.assertLock();
 		
 		if (exception != null)
 			Environment.asynOpenErrorDialog("Dynamic editor descriptor loading", message, exception);
 
 		if (ResourcesPlugin.getWorkspace().isTreeLocked()) {
 			Job job = new WorkspaceJob("Add error marker") {
 				{ setSystem(true); } // don't show to user
 				@Override
 				public IStatus runInWorkspace(IProgressMonitor monitor) {
 					asyncMessageHandler.addMarker(descriptor, offendingTerm, message, SEVERITY_ERROR);
 					return Status.OK_STATUS;
 				}
 			};
 			job.setRule(descriptor);
 			job.schedule();
 		} else {
 			asyncMessageHandler.addMarkerFirstLine(descriptor, message, SEVERITY_ERROR);
 		}
 	}
 }
