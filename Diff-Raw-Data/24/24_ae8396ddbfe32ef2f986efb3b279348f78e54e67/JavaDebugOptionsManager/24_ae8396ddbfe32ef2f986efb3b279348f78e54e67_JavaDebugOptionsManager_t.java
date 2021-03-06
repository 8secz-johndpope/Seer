 package org.eclipse.jdt.internal.debug.ui;
 
 /*
  * (c) Copyright IBM Corp. 2000, 2001.
  * All Rights Reserved.
  */
  
 import java.text.MessageFormat;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.StringTokenizer;
 
 import com.sun.jdi.InvocationException;
 import com.sun.jdi.ObjectReference;
 
 import org.eclipse.core.resources.IMarker;
 import org.eclipse.core.resources.IMarkerDelta;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.IResourceChangeEvent;
 import org.eclipse.core.resources.IResourceChangeListener;
 import org.eclipse.core.resources.IResourceDelta;
 import org.eclipse.core.resources.IWorkspaceRunnable;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.debug.core.DebugEvent;
 import org.eclipse.debug.core.DebugException;
 import org.eclipse.debug.core.DebugPlugin;
 import org.eclipse.debug.core.IDebugEventSetListener;
 import org.eclipse.debug.core.ILaunch;
 import org.eclipse.debug.core.ILaunchConfiguration;
 import org.eclipse.debug.core.ILaunchListener;
 import org.eclipse.debug.core.model.IBreakpoint;
 import org.eclipse.debug.core.model.IDebugTarget;
 import org.eclipse.debug.core.model.ISourceLocator;
 import org.eclipse.debug.ui.DebugUITools;
 import org.eclipse.jdt.core.ICompilationUnit;
 import org.eclipse.jdt.core.IJavaElement;
 import org.eclipse.jdt.core.IJavaProject;
 import org.eclipse.jdt.core.JavaCore;
 import org.eclipse.jdt.core.dom.Message;
 import org.eclipse.jdt.debug.core.IJavaBreakpoint;
 import org.eclipse.jdt.debug.core.IJavaBreakpointListener;
 import org.eclipse.jdt.debug.core.IJavaDebugTarget;
 import org.eclipse.jdt.debug.core.IJavaExceptionBreakpoint;
 import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
 import org.eclipse.jdt.debug.core.IJavaStackFrame;
 import org.eclipse.jdt.debug.core.IJavaThread;
 import org.eclipse.jdt.debug.core.IJavaType;
 import org.eclipse.jdt.debug.core.JDIDebugModel;
 import org.eclipse.jdt.debug.ui.JavaUISourceLocator;
 import org.eclipse.jdt.internal.debug.ui.snippeteditor.ScrapbookLauncher;
 import org.eclipse.jdt.internal.launching.JavaLaunchConfigurationUtils;
 import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
 import org.eclipse.jdt.launching.sourcelookup.JavaSourceLocator;
 import org.eclipse.jface.preference.IPreferenceStore;
 import org.eclipse.jface.util.IPropertyChangeListener;
 import org.eclipse.jface.util.PropertyChangeEvent;
 import org.eclipse.jface.viewers.ILabelProvider;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Shell;
 
 /**
  * Manages options for the Java Debugger:<ul>
  * <li>Suspend on compilation errors</li>
  * <li>Ssuspend on uncaught exceptions</li>
  * <li>Step filters</li>
  * </ul>
  */
 public class JavaDebugOptionsManager implements ILaunchListener, IResourceChangeListener, IDebugEventSetListener, IPropertyChangeListener, IJavaBreakpointListener {
 	
 	/**
 	 * Singleton options manager
 	 */
 	private static JavaDebugOptionsManager fgOptionsManager = null;
 	
 	/**
 	 * Map of problems to locations
 	 * (<code>IMarker</code> -> <code>Location</code>)
 	 */
 	private HashMap fProblemMap = new HashMap(10);
 	
 	/**
 	 * Map of locations to problems.
 	 * (<code>Location</code> -> <code>IMarker</code>)
 	 */
 	private HashMap fLocationMap = new HashMap(10);
 	
 	/**
 	 * Breakpoint used to suspend on uncaught exceptions
 	 */
 	private IJavaExceptionBreakpoint fSuspendOnExceptionBreakpoint = null;
 	
 	/**
 	 * Breakpoint used to suspend on compilation errors
 	 */
 	private IJavaExceptionBreakpoint fSuspendOnErrorBreakpoint = null;	
 	
 	/**
 	 * A label provider
 	 */
 	private static ILabelProvider fLabelProvider= DebugUITools.newDebugModelPresentation();
 	
 	/**
 	 * Whether problem handling has been initialized.
 	 */
 	private boolean fProblemHandlingInitialized = false;
 	
 	/**
 	 * Constants indicating whether a breakpoint
 	 * is added, removed, or changed.
 	 */
 	private static final int ADDED = 0;
 	private static final int REMOVED = 1;
 	private static final int CHANGED = 2;
 		
 	/**
 	 * Local cache of active step filters.
 	 */
 	private String[] fActiveStepFilters = new String[0];
 	
 	/**
 	 * Helper class that describes a location in a stack
 	 * frame. A location consists of a package name, source
 	 * file name, and a line number.
 	 */
 	class Location {
 		private String fPackageName;
 		private String fSourceName;
 		private int fLineNumber;
 		
 		public Location(String packageName, String sourceName, int lineNumber) {
 			fPackageName = packageName;
 			fSourceName = sourceName;
 			fLineNumber = lineNumber;
 		}
 		
 		public boolean equals(Object o) {
 			if (o instanceof Location) {
 				Location l = (Location)o;
 				return l.fPackageName.equals(fPackageName) && l.fSourceName.equals(fSourceName) && l.fLineNumber == fLineNumber;
 				
 			}
 			return false;
 		}
 		
 		public int hashCode() {
 			return fPackageName.hashCode() + fSourceName.hashCode() + fLineNumber;
 		}
 	}
 
 	/**
 	 * Update cache of problems as they are added/removed.
 	 * 
 	 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
 	 */
 	public void resourceChanged(IResourceChangeEvent event) {
 		
 		if (!fProblemHandlingInitialized) {
 			// do nothing if problem handling has not yet been initialized.
 			// initialization is performed on first launch.
 			return;
 		}
 		
 		IMarkerDelta[] deltas = event.findMarkerDeltas("org.eclipse.jdt.core.problem", true); //$NON-NLS-1$
 		if (deltas != null) {
 			for (int i = 0; i < deltas.length; i++) {
 				IMarkerDelta delta = deltas[i];
 				switch (delta.getKind()) {
 					case IResourceDelta.ADDED:
 						problemAdded(delta.getMarker());
 						break;
 					case IResourceDelta.REMOVED:
 						problemRemoved(delta.getMarker());
 						break;
 				}
 			}
 		}
 	}
 	
 	/**
 	 * Not to be instantiated
 	 * 
 	 * @see JavaDebugOptionsManager.getDefault();
 	 */
 	private JavaDebugOptionsManager() {
 	}
 	
 	/**
 	 * Return the default options manager
 	 */
 	public static JavaDebugOptionsManager getDefault() {
 		if (fgOptionsManager == null) {
 			fgOptionsManager = new JavaDebugOptionsManager();
 		}
 		return fgOptionsManager;
 	}
 	
 	/**
 	 * Called at startup by the java debug ui plug-in
 	 */
 	public void startup() throws CoreException {
 		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
 		DebugPlugin.getDefault().addDebugEventListener(this);
 		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
 		JDIDebugUIPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
 		JDIDebugModel.addJavaBreakpointListener(this);
 		updateActiveFilters();
 	}
 	
 	/**
 	 * Called at shutdown by the java debug ui plug-in
 	 */
 	public void shutdown() throws CoreException {
 		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
 		DebugPlugin.getDefault().removeDebugEventListener(this);
 		DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
 		JDIDebugUIPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
 		JDIDebugModel.removeJavaBreakpointListener(this);
 		fProblemMap.clear();
 		fLocationMap.clear();
 	}	
 
 	/**
 	 * Initializes compilation error handling and suspending
 	 * on uncaught exceptions.
 	 */
 	protected void initializeProblemHandling() {
 		if (fProblemHandlingInitialized) {
 			return;
 		}
 		IWorkspaceRunnable wr = new IWorkspaceRunnable() {
 			public void run(IProgressMonitor monitor) throws CoreException {
 				// compilation error breakpoint
 				IJavaExceptionBreakpoint bp = JDIDebugModel.createExceptionBreakpoint(ResourcesPlugin.getWorkspace().getRoot(),"java.lang.Error", true, true, false, false, null); //$NON-NLS-1$
 				bp.setPersisted(false);
 				bp.setRegistered(false);
 				// disabled until there are errors
 				bp.setEnabled(false);
 				setSuspendOnCompilationErrorsBreakpoint(bp);
 				
 				// uncaught exception breakpoint
 				bp = JDIDebugModel.createExceptionBreakpoint(ResourcesPlugin.getWorkspace().getRoot(),"java.lang.Throwable", false, true, false, false, null); //$NON-NLS-1$
 				bp.setPersisted(false);
 				bp.setRegistered(false);
 				bp.setEnabled(isSuspendOnUncaughtExceptions());
 				setSuspendOnUncaughtExceptionBreakpoint(bp);
 				
 				// note existing compilation errors
 				IMarker[] problems = ResourcesPlugin.getWorkspace().getRoot().findMarkers("org.eclipse.jdt.core.problem", true, IResource.DEPTH_INFINITE); //$NON-NLS-1$
 				if (problems != null) {
 					for (int i = 0; i < problems.length; i++) {
 						problemAdded(problems[i]);
 					}
 				}				
 			}
 		};
 		
 		try {
 			ResourcesPlugin.getWorkspace().run(wr, null);
 		} catch (CoreException e) {
 			JDIDebugUIPlugin.log(e);
 		}
 		fProblemHandlingInitialized = true;
 	}
 		
 	/**
 	 * The given problem has been added. Cross
 	 * reference the problem with its location.
 	 * Enable the error breakpoint if the suspend
 	 * option is on, and this is the first problem
 	 * being added.
 	 */
 	protected void problemAdded(IMarker problem) {
 		if (problem.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
 			IResource res = problem.getResource();
 			IJavaElement cu = JavaCore.create(res);
 			if (cu != null && cu instanceof ICompilationUnit) {
 				// auto-enable the exception breakpoint if this is the first problem added
 				// and the preference is turned on.
 				boolean autoEnable = fProblemMap.isEmpty();
 				int line = problem.getAttribute(IMarker.LINE_NUMBER, -1);
 				String name = cu.getElementName();
 				Location l = new Location(cu.getParent().getElementName(), name, line);
 				fLocationMap.put(l, problem);
 				fProblemMap.put(problem, l);
 				if (autoEnable) {
 					IWorkspaceRunnable wRunnable= new IWorkspaceRunnable() {
 						public void run(IProgressMonitor monitor) {
 							try {
 								getSuspendOnCompilationErrorBreakpoint().setEnabled(isSuspendOnCompilationErrors());
 							} catch (CoreException e) {
 								JDIDebugUIPlugin.log(e);
 							}
 						}
 					};
 					fork(wRunnable);
 				}
 			}
 		}
 	}
 	
 	/**
 	 * The given problem has been removed. Remove
 	 * cross reference of problem and location.
 	 * Disable the breakpoint if there are no errors.
 	 */
 	protected void problemRemoved(IMarker problem) {
 		Object location = fProblemMap.remove(problem);
 		if (location != null) {
 			fLocationMap.remove(location);
 		}
 		if (fProblemMap.isEmpty()) {
 			IWorkspaceRunnable wRunnable= new IWorkspaceRunnable() {
 				public void run(IProgressMonitor monitor) {
 					try {
 						getSuspendOnCompilationErrorBreakpoint().setEnabled(false);
 					} catch (CoreException e) {
 						JDIDebugUIPlugin.log(e);
 					}
 				}
 			};
 			fork(wRunnable);
 		}
 	}
 				
 	/**
 	 * Notifies java debug targets of the given breakpoint
 	 * addition or removal.
 	 * 
 	 * @param breakpoint a breakpoint
 	 * @param kind ADDED, REMOVED, or CHANGED
 	 */
 	protected void notifyTargets(IBreakpoint breakpoint, int kind) {
 		IDebugTarget[] targets = DebugPlugin.getDefault().getLaunchManager().getDebugTargets();
 		for (int i = 0; i < targets.length; i++) {
 			if (targets[i] instanceof IJavaDebugTarget) {
 				IJavaDebugTarget target = (IJavaDebugTarget)targets[i];
 				notifyTarget(target, breakpoint, kind);
 			}
 		}	
 	}
 	
 	/**
 	 * Notifies the give debug target of filter specifications
 	 * 
 	 * @param target Java debug target
 	 */
 	protected void notifyTargetOfFilters(IJavaDebugTarget target) {
 
 		IPreferenceStore store = JDIDebugUIPlugin.getDefault().getPreferenceStore();
 		
 		target.setFilterConstructors(store.getBoolean(IJDIPreferencesConstants.PREF_FILTER_CONSTRUCTORS));
 		target.setFilterStaticInitializers(store.getBoolean(IJDIPreferencesConstants.PREF_FILTER_STATIC_INITIALIZERS));
 		target.setFilterSynthetics(store.getBoolean(IJDIPreferencesConstants.PREF_FILTER_SYNTHETICS));
 		target.setStepFiltersEnabled(store.getBoolean(IJDIPreferencesConstants.PREF_USE_FILTERS));
 		target.setStepFilters(getActiveStepFilters());
 
 	}	
 	
 	/**
 	 * Notifies all targets of current filter specifications.
 	 */
 	protected void notifyTargetsOfFilters() {
 		IDebugTarget[] targets = DebugPlugin.getDefault().getLaunchManager().getDebugTargets();
 		for (int i = 0; i < targets.length; i++) {
 			if (targets[i] instanceof IJavaDebugTarget) {
 				IJavaDebugTarget target = (IJavaDebugTarget)targets[i];
 				notifyTargetOfFilters(target);
 			}
 		}	
 	}		
 
 	/**
 	 * Notifies the given target of the given breakpoint
 	 * addition or removal.
 	 * 
 	 * @param target Java debug target
 	 * @param breakpoint a breakpoint
 	 * @param kind ADDED, REMOVED, or CHANGED
 	 */	
 	protected void notifyTarget(IJavaDebugTarget target, IBreakpoint breakpoint, int kind) {
 		switch (kind) {
 			case ADDED:
 				target.breakpointAdded(breakpoint);
 				break;
 			case REMOVED:
 				target.breakpointRemoved(breakpoint,null);
 				break;
 			case CHANGED:
 				target.breakpointChanged(breakpoint,null);
 				break;
 		}
 	}
 	
 	/**
 	 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
 	 */
 	public void propertyChange(PropertyChangeEvent event) {
 		if (event.getProperty().equals(IJDIPreferencesConstants.PREF_SUSPEND_ON_COMPILATION_ERRORS)) {
 			setSuspendOnCompilationErrors(((Boolean)event.getNewValue()).booleanValue());
 		} else if (event.getProperty().equals(IJDIPreferencesConstants.PREF_SUSPEND_ON_UNCAUGHT_EXCEPTIONS)) {
 			setSuspendOnUncaughtExceptions(((Boolean)event.getNewValue()).booleanValue());
 		} else if (event.getProperty().equals(IJDIPreferencesConstants.PREF_FILTER_CONSTRUCTORS)) {
 			notifyTargetsOfFilters();
 		} else if (event.getProperty().equals(IJDIPreferencesConstants.PREF_FILTER_STATIC_INITIALIZERS)) {
 			notifyTargetsOfFilters();
 		} else if (event.getProperty().equals(IJDIPreferencesConstants.PREF_FILTER_SYNTHETICS)) {
 			notifyTargetsOfFilters();
 		} else if (event.getProperty().equals(IJDIPreferencesConstants.PREF_USE_FILTERS)) {
 			notifyTargetsOfFilters();
 		} else if (event.getProperty().equals(IJDIPreferencesConstants.PREF_ACTIVE_FILTERS_LIST)) {
 			updateActiveFilters();
 		} else if (event.getProperty().equals(IJDIPreferencesConstants.PREF_INACTIVE_FILTERS_LIST)) {
 			updateActiveFilters();
 		}
 	}
 	
 	/**
 	 * Sets whether or not to suspend on compilation errors
 	 * 
 	 * @param enabled whether to suspend on compilation errors
 	 */
 	protected void setSuspendOnCompilationErrors(boolean enabled) {
 		initializeProblemHandling();
 		IBreakpoint breakpoint = getSuspendOnCompilationErrorBreakpoint();
 		setEnabled(breakpoint, enabled);
 	}
 	
 	/**
 	 * Sets whether or not to suspend on uncaught exceptions
 	 * 
 	 * @param enabled whether or not to suspend on uncaught exceptions
 	 */
 	protected void setSuspendOnUncaughtExceptions(boolean enabled) {
 		initializeProblemHandling();
 		IBreakpoint breakpoint = getSuspendOnUncaughtExceptionBreakpoint();
 		setEnabled(breakpoint, enabled);
 	}	
 	
 	/**
 	 * Enable/Disable the given breakpoint and notify
 	 * targets of the change.
 	 * 
 	 * @param breakpoint a breakpoint
 	 * @param enabled whether enabeld
 	 */ 
 	protected void setEnabled(IBreakpoint breakpoint, boolean enabled) {
 		try {
 			breakpoint.setEnabled(enabled);
 			notifyTargets(breakpoint, CHANGED);
 		} catch (CoreException e) {
 			JDIDebugUIPlugin.log(e);
 		}		
 	}
 	
 	/**
 	 * Returns whether suspend on compilation errors is
 	 * enabled.
 	 * 
 	 * @return whether suspend on compilation errors is
 	 * enabled
 	 */
 	protected boolean isSuspendOnCompilationErrors() {
 		return JDIDebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IJDIPreferencesConstants.PREF_SUSPEND_ON_COMPILATION_ERRORS);
 	}
 	
 	/**
 	 * Returns whether suspend on uncaught exception is
 	 * enabled
 	 * 
 	 * @return whether suspend on uncaught exception is
 	 * enabled
 	 */
 	protected boolean isSuspendOnUncaughtExceptions() {
 		return JDIDebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IJDIPreferencesConstants.PREF_SUSPEND_ON_UNCAUGHT_EXCEPTIONS);
 	}	
 
 
 	/**
 	 * Sets the breakpoint used to suspend on uncaught exceptions
 	 * 
 	 * @param breakpoint exception breakpoint
 	 */
 	private void setSuspendOnUncaughtExceptionBreakpoint(IJavaExceptionBreakpoint breakpoint) {
 		fSuspendOnExceptionBreakpoint = breakpoint;
 	}
 	
 	/**
 	 * Returns the breakpoint used to suspend on uncaught exceptions
 	 * 
 	 * @return exception breakpoint
 	 */
 	protected IJavaExceptionBreakpoint getSuspendOnUncaughtExceptionBreakpoint() {
 		return fSuspendOnExceptionBreakpoint;
 	}	
 	
 	/**
 	 * Sets the breakpoint used to suspend on compilation 
 	 * errors.
 	 * 
 	 * @param breakpoint exception breakpoint
 	 */
 	private void setSuspendOnCompilationErrorsBreakpoint(IJavaExceptionBreakpoint breakpoint) {
 		fSuspendOnErrorBreakpoint = breakpoint;
 	}
 	
 	/**
 	 * Returns the breakpoint used to suspend on compilation
 	 * errors
 	 * 
 	 * @return exception breakpoint
 	 */
 	protected IJavaExceptionBreakpoint getSuspendOnCompilationErrorBreakpoint() {
 		return fSuspendOnErrorBreakpoint;
 	}	
 	
 	/**
 	 * Parses the comma separated string into an array of strings
 	 * 
 	 * @return list
 	 */
 	public static String[] parseList(String listString) {
 		List list = new ArrayList(10);
 		StringTokenizer tokenizer = new StringTokenizer(listString, ","); //$NON-NLS-1$
 		while (tokenizer.hasMoreTokens()) {
 			String token = tokenizer.nextToken();
 			list.add(token);
 		}
 		return (String[])list.toArray(new String[list.size()]);
 	}
 	
 	/**
 	 * Serializes the array of strings into one comma
 	 * separated string.
 	 * 
 	 * @param list array of strings
 	 * @return a single string composed of the given list
 	 */
 	public static String serializeList(String[] list) {
 		if (list == null) {
 			return ""; //$NON-NLS-1$
 		}
 		StringBuffer buffer = new StringBuffer();
 		for (int i = 0; i < list.length; i++) {
 			if (i > 0) {
 				buffer.append(',');
 			}
 			buffer.append(list[i]);
 		}
 		return buffer.toString();
 	}	
 	
 	/**
 	 * Sets the current list of active step filters
 	 * 
 	 * @param filters the current list of active step filters
 	 */
 	private void setActiveStepFilters(String[] filters) {
 		fActiveStepFilters = filters;
 	}
 	
 	/**
 	 * Returns the current list of active step filters
 	 * 
 	 * @return current list of active step filters
 	 */
 	protected String[] getActiveStepFilters() {
 		return fActiveStepFilters;
 	}
 	
 	/**
 	 * Updates local copy of active step filters and
 	 * notifies targets.
 	 */
 	protected void updateActiveFilters() {
 		String[] filters = parseList(JDIDebugUIPlugin.getDefault().getPreferenceStore().getString(IJDIPreferencesConstants.PREF_ACTIVE_FILTERS_LIST));
 		setActiveStepFilters(filters);
 		notifyTargetsOfFilters();
 	}
 	
 	/**
 	 * When a Java debug target is created, install options in
 	 * the target.
 	 * 
 	 * @see IDebugEventSetListener#handleDebugEvents(DebugEvent[])
 	 */
 	public void handleDebugEvents(DebugEvent[] events) {
 		for (int i = 0; i < events.length; i++) {
 			DebugEvent event = events[i];
 			if (event.getKind() == DebugEvent.CREATE) {
 				Object source = event.getSource();
 				if (source instanceof IJavaDebugTarget) {
 					IJavaDebugTarget javaTarget = (IJavaDebugTarget)source;
 					
 					// compilation breakpoints				
 					notifyTarget(javaTarget, getSuspendOnCompilationErrorBreakpoint(), ADDED);
 					
 					// uncaught exception breakpoint
 					notifyTarget(javaTarget, getSuspendOnUncaughtExceptionBreakpoint(), ADDED);
 					
 					// step filters
 					notifyTargetOfFilters(javaTarget);
 				}
 			}
 		}
 	}
 
 	/**
 	 * @see IJavaBreakpointListener#addingBreakpoint(IJavaDebugTarget, IJavaBreakpoint)
 	 */
 	public void addingBreakpoint(IJavaDebugTarget target, IJavaBreakpoint breakpoint) {
 	}
 
 	/**
 	 * @see IJavaBreakpointListener#installingBreakpoint(IJavaDebugTarget, IJavaBreakpoint, IJavaType)
 	 */
 	public boolean installingBreakpoint(IJavaDebugTarget target, IJavaBreakpoint breakpoint, IJavaType type) {
 		return true;
 	}
 	
 	/**
 	 * @see IJavaBreakpointListener#breakpointHit(IJavaThread, IJavaBreakpoint)
 	 */
 	public boolean breakpointHit(IJavaThread thread, IJavaBreakpoint breakpoint) {
 		if (breakpoint == getSuspendOnCompilationErrorBreakpoint()) {
 			try {
 				IJavaStackFrame frame = (IJavaStackFrame)thread.getTopStackFrame();
 				if (frame != null) {
 					return  getProblem(frame) != null;
 				}
 			} catch (DebugException e) {
 				JDIDebugUIPlugin.log(e);
 			}
 			
 		}
 		return true;
 	}
 
 	/**
 	 * @see IJavaBreakpointListener#breakpointInstalled(IJavaDebugTarget, IJavaBreakpoint)
 	 */
 	public void breakpointInstalled(IJavaDebugTarget target, IJavaBreakpoint breakpoint) {
 	}
 
 	/**
 	 * @see IJavaBreakpointListener#breakpointRemoved(IJavaDebugTarget, IJavaBreakpoint)
 	 */
 	public void breakpointRemoved(IJavaDebugTarget target, IJavaBreakpoint breakpoint) {
 	}
 	
 	/**
 	 * Returns any problem marker associated with the current location
 	 * of the given stack frame, or <code>null</code> if none.
 	 * 
 	 * @param frame stack frame
 	 * @return marker representing compilation problem, or <code>null</code>
 	 */
 	protected IMarker getProblem(IJavaStackFrame frame) {
 		try {
 			String name = frame.getSourceName();
 			String packageName = frame.getDeclaringTypeName();
 			int index = packageName.lastIndexOf('.');
 			if (index == -1) {
 				if (name == null) {
 					// guess at source name if no debug attribute
 					name = packageName;
 					int dollar = name.indexOf('$');
 					if (dollar >= 0) {
 						name = name.substring(0, dollar);
 					}
 					name+= ".java"; //$NON-NLS-1$
 				}
 				packageName = ""; //$NON-NLS-1$
 			} else {
 				if (name == null) {
 					name = packageName.substring(index + 1);
 					int dollar = name.indexOf('$');
 					if (dollar >= 0) {
 						name = name.substring(0, dollar);
 					}
 					name += ".java"; //$NON-NLS-1$
 				}
 				packageName = packageName.substring(0,index);
 			}
 			int line = frame.getLineNumber();
 			Location l = new Location(packageName, name, line);
 			return  (IMarker)fLocationMap.get(l);		
 		} catch (DebugException e) {
 			JDIDebugUIPlugin.log(e);
 		}
 		return null;
 	}
 	
 	protected void fork(final IWorkspaceRunnable wRunnable) {
 		Runnable runnable= new Runnable() {
 			public void run() {
 				try {
 					ResourcesPlugin.getWorkspace().run(wRunnable, null);
 				} catch (CoreException ce) {
 					JDIDebugUIPlugin.log(ce);
 				}
 			}
 		};
 		new Thread(runnable).start();
 	}		
 
 	/**
 	 * Replaces source locator with UI source locator
 	 * 
 	 * @see ILaunchListener#launchAdded(ILaunch)
 	 */
 	public void launchAdded(ILaunch launch) {
 		
 		initializeProblemHandling();
 		ILaunchConfiguration config = launch.getLaunchConfiguration();
 		try {
			if (config.getAttribute(ScrapbookLauncher.SCRAPBOOK_LAUNCH, (String)null) != null) {
				// do not use UI source locator for scrapbook
				return;
			}
			
 			if (config != null && 
 				((config.getType().getIdentifier().equals(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION))
 				|| (config.getType().getIdentifier().equals(IJavaLaunchConfigurationConstants.ID_REMOTE_JAVA_APPLICATION)))) {
 					ISourceLocator sl = launch.getSourceLocator();
 					if (sl == null || sl instanceof JavaSourceLocator) {
 						IJavaProject jp = JavaLaunchConfigurationUtils.getJavaProject(config);
 						if (jp != null) {
 								JavaUISourceLocator jsl = new JavaUISourceLocator(jp);
 								launch.setSourceLocator(jsl);
 						}
 					}
 				}
 		} catch (CoreException e) {
 			JDIDebugUIPlugin.log(e);
 		}
 	}
 	/**
 	 * @see ILaunchListener#launchChanged(ILaunch)
 	 */
 	public void launchChanged(ILaunch launch) {
 	}
 
 	/**
 	 * @see ILaunchListener#launchRemoved(ILaunch)
 	 */
 	public void launchRemoved(ILaunch launch) {
 	}
 	
 	/**
 	 * @see IJavaConditionalBreakpointListener#breakpointHasRuntimeException(IJavaLineBreakpoint, Throwable)
 	 */
 	public void breakpointHasRuntimeException(final IJavaLineBreakpoint breakpoint, final DebugException exception) {
 		IStatus status;
 		Throwable wrappedException= exception.getStatus().getException();
 		if (wrappedException instanceof InvocationException) {
 			InvocationException ie= (InvocationException) wrappedException;
 			ObjectReference ref= ie.exception();		
 			status= new Status(IStatus.ERROR,JDIDebugUIPlugin.getUniqueIdentifier(), IStatus.ERROR, ref.referenceType().name(), null);
 		} else {
 			status= exception.getStatus();
 		}
 		openConditionErrorDialog(breakpoint, DebugUIMessages.getString("JavaDebugOptionsManager.Conditional_breakpoint_evaluation_failed_3"), DebugUIMessages.getString("JavaDebugOptionsManager.An_exception_occurred_while_evaluating_the_condition_for_breakpoint__{0}__4"), status); //$NON-NLS-1$ //$NON-NLS-2$
 	}
 
 	/**
 	 * @see IJavaConditionalBreakpointListener#breakpointHasCompilationErrors(IJavaLineBreakpoint, Message[])
 	 */
 	public void breakpointHasCompilationErrors(final IJavaLineBreakpoint breakpoint, final Message[] errors) {
 		StringBuffer message= new StringBuffer();
 		Message error;
 		for (int i=0, numErrors= errors.length; i < numErrors; i++) {
 			error= errors[i];
 			message.append(error.getMessage());
 			message.append("\n "); //$NON-NLS-1$
 		}
 		IStatus status= new Status(IStatus.ERROR, JDIDebugUIPlugin.getUniqueIdentifier(), IStatus.ERROR, message.toString(), null);
 		openConditionErrorDialog(breakpoint, DebugUIMessages.getString("JavaDebugOptionsManager.Conditional_breakpoint_compilation_failed_6"), DebugUIMessages.getString("JavaDebugOptionsManager.Errors_detected_compiling_the_condition_for_breakpoint_{0}_7"), status); //$NON-NLS-1$ //$NON-NLS-2$
 	}
 	
 	private void openConditionErrorDialog(final IJavaLineBreakpoint breakpoint, final String title, final String errorMessage, final IStatus status) {
 		final Display display= JDIDebugUIPlugin.getStandardDisplay();
 		if (display.isDisposed()) {
 			return;
 		}
 		display.asyncExec(new Runnable() {
 			public void run() {
 				if (display.isDisposed()) {
 					return;
 				}
 				Shell shell= JDIDebugUIPlugin.getActiveWorkbenchShell();
 				String breakpointText= fLabelProvider.getText(breakpoint);
 				ConditionalBreakpointErrorDialog dialog= new ConditionalBreakpointErrorDialog(shell, title,
 					MessageFormat.format(errorMessage, new String[] {breakpointText}), status, breakpoint);
 				dialog.open();
 			}
 		});
 	}
 	
 }
