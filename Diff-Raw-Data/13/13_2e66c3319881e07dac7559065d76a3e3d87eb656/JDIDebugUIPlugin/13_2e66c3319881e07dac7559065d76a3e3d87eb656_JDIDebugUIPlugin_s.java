 package org.eclipse.jdt.internal.debug.ui;
 
 /*
  * (c) Copyright IBM Corp. 2000, 2001.
  * All Rights Reserved.
  */
  
 import java.text.MessageFormat;
 
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IAdapterManager;
 import org.eclipse.core.runtime.IPluginDescriptor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Platform;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.debug.core.DebugException;
 import org.eclipse.debug.core.ILaunch;
 import org.eclipse.debug.core.ILauncher;
 import org.eclipse.debug.core.model.ILauncherDelegate;
 import org.eclipse.debug.internal.ui.DelegatingModelPresentation;
 import org.eclipse.jdt.debug.core.IJavaDebugTarget;
 import org.eclipse.jdt.debug.core.IJavaHotCodeReplaceListener;
 import org.eclipse.jdt.debug.core.JDIDebugModel;
 import org.eclipse.jdt.debug.ui.JavaDebugUI;
 import org.eclipse.jdt.internal.debug.ui.snippeteditor.ScrapbookLauncherDelegate;
 import org.eclipse.jdt.internal.debug.ui.snippeteditor.SnippetFileDocumentProvider;
 import org.eclipse.jdt.launching.sourcelookup.IJavaSourceLocation;
 import org.eclipse.jface.dialogs.ErrorDialog;
 import org.eclipse.jface.preference.IPreferenceStore;
 import org.eclipse.jface.resource.ImageRegistry;
 import org.eclipse.jface.viewers.ILabelProvider;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.ui.IWorkbenchPage;
 import org.eclipse.ui.IWorkbenchWindow;
 import org.eclipse.ui.editors.text.FileDocumentProvider;
 import org.eclipse.ui.plugin.AbstractUIPlugin;
 import org.eclipse.ui.texteditor.IDocumentProvider;
 
 /**
  * Plug-in class for the org.eclipse.jdt.debug.ui plug-in.
  */
 public class JDIDebugUIPlugin extends AbstractUIPlugin implements IJavaHotCodeReplaceListener {
 
 	/**
 	 * Java Debug UI plug-in instance
 	 */
 	private static JDIDebugUIPlugin fgPlugin;
 	
 	private static ILabelProvider fLabelProvider= new DelegatingModelPresentation();
 	
 	private FileDocumentProvider fSnippetDocumentProvider;
 	
 	private ImageDescriptorRegistry fImageDescriptorRegistry;
 	
 	/**
 	 * @see Plugin(IPluginDescriptor)
 	 */
 	public JDIDebugUIPlugin(IPluginDescriptor descriptor) {
 		super(descriptor);
 		setDefault(this);
		fImageDescriptorRegistry= new ImageDescriptorRegistry();
 	}
 	
 	/**
 	 * Sets the Java Debug UI plug-in instance
 	 * 
 	 * @param plugin the plugin instance
 	 */
 	private static void setDefault(JDIDebugUIPlugin plugin) {
 		fgPlugin = plugin;
 	}
 	
 	/**
 	 * Returns the Java Debug UI plug-in instance
 	 * 
 	 * @return the Java Debug UI plug-in instance
 	 */
 	public static JDIDebugUIPlugin getDefault() {
 		return fgPlugin;
 	}
 	
 	/**
 	 * Returns the identifier for the Java Debug UI plug-in
 	 * 
 	 * @return the identifier for the Java Debug UI plug-in
 	 */
 	public static String getPluginId() {
 		return getDefault().getDescriptor().getUniqueIdentifier();
 	}
 	
 	/**
 	 * Logs the specified status with this plug-in's log.
 	 * 
 	 * @param status status to log
 	 */
 	public static void log(IStatus status) {
 		getDefault().getLog().log(status);
 	}
 	
 	/**
 	 * Logs an internal error with the specified message.
 	 * 
 	 * @param message the error message to log
 	 */
 	public static void logErrorMessage(String message) {
 		log(new Status(IStatus.ERROR, getPluginId(), JavaDebugUI.INTERNAL_ERROR, message, null));
 	}
 
 	/**
 	 * Logs an internal error with the specified throwable
 	 * 
 	 * @param e the exception to be logged
 	 */	
 	public static void log(Throwable e) {
 		log(new Status(IStatus.ERROR, getPluginId(), JavaDebugUI.INTERNAL_ERROR, DebugUIMessages.getString("JDIDebugUIPlugin.Internal_Error_1"), e));  //$NON-NLS-1$
 	}
 	
 	/**
 	 * Returns the active workbench window
 	 * 
 	 * @return the active workbench window
 	 */
 	public static IWorkbenchWindow getActiveWorkbenchWindow() {
 		return getDefault().getWorkbench().getActiveWorkbenchWindow();
 	}	
 	
 	public static IWorkbenchPage getActivePage() {
 		return getDefault().getActiveWorkbenchWindow().getActivePage();
 	}
 	
 	
 	/**
 	 * Returns the active workbench shell
 	 * 
 	 * @return the active workbench shell
 	 */
 	public static Shell getActiveWorkbenchShell() {
 		return getActiveWorkbenchWindow().getShell();
 	}	
 	
 	/* (non - Javadoc)
 	 * Method declared in AbstractUIPlugin
 	 */
 	protected ImageRegistry createImageRegistry() {
 		return JavaDebugImages.getImageRegistry();
 	}	
 	
 	public IDocumentProvider getSnippetDocumentProvider() {
 		if (fSnippetDocumentProvider == null)
 			fSnippetDocumentProvider= new SnippetFileDocumentProvider();
 		return fSnippetDocumentProvider;
 	}	
 	
 	public static void logError(Exception e) {
 		if (getDefault().isDebugging()) {
 			// this message is intentionally not internationalized, as an exception may
 			// be due to the resource bundle itself
 			log(new Status(IStatus.ERROR, getPluginId(), JavaDebugUI.INTERNAL_ERROR, "Internal error logged from JDT Debug UI: ", e));  //$NON-NLS-1$		
 		}
 	}
 	
 	public static void errorDialog(String message, IStatus status) {
 		Shell shell = getActiveWorkbenchShell();
 		if (shell == null) {
 			log(status);
 		} else {
 			ErrorDialog.openError(shell, DebugUIMessages.getString("JDIDebugUIPlugin.Error_1"), message, status); //$NON-NLS-1$
 		}
 	}
 	
 	/**
 	 * @see AbstractUIPlugin#initializeDefaultPreferences
 	 */
 	protected void initializeDefaultPreferences(IPreferenceStore store) {
 		super.initializeDefaultPreferences(store);
 		
 		store.setDefault(IJDIPreferencesConstants.ATTACH_LAUNCH_PORT, "8000"); //$NON-NLS-1$
 		store.setDefault(IJDIPreferencesConstants.ATTACH_LAUNCH_HOST, "localhost"); //$NON-NLS-1$
 		store.setDefault(IJDIPreferencesConstants.ALERT_HCR_FAILED, true);
 		store.setDefault(IJDIPreferencesConstants.ALERT_OBSOLETE_METHODS, true);
 		
 		JavaDebugPreferencePage.initDefaults(store);
 	}
 	
 	public void startup() throws CoreException {
 		JDIDebugModel.addHotCodeReplaceListener(this);
 		super.startup();
 		
 		JavaDebugOptionsManager.getDefault().startup();
 		
 		IAdapterManager manager= Platform.getAdapterManager();
 		manager.registerAdapters(new JDIDebugUIAdapterFactory(), IJavaSourceLocation.class);		
 	}
 	
 	public void shutdown() throws CoreException {
 		JDIDebugModel.removeHotCodeReplaceListener(this);
 		JavaDebugOptionsManager.getDefault().shutdown();
 		getImageDescriptorRegistry().dispose();
 		super.shutdown();
 	}
 	/**
 	 * @see IJavaHotCodeReplaceListener#hotCodeReplaceFailed(DebugException)
 	 */
 	public void hotCodeReplaceFailed(final IJavaDebugTarget target, final DebugException exception) {
 		if (!getPreferenceStore().getBoolean(IJDIPreferencesConstants.ALERT_HCR_FAILED)) {
 			return;
 		}
 		// do not report errors for snippet editor targets
 		// that do not support HCR. HCR is simulated by using
 		// a new class loader for each evaluation
 		ILaunch launch = target.getLaunch();
 		if (launch != null) {
 			ILauncher launcher = launch.getLauncher();
 			if (launcher != null) {
 				ILauncherDelegate delegate = launcher.getDelegate();
 				if (delegate instanceof ScrapbookLauncherDelegate) {
 					if (!target.supportsHotCodeReplace()) {
 						return;
 					}
 				}
 			}
 		}
 		getDisplay().asyncExec(new Runnable() {
 			public void run() {
 				Shell shell= getActiveWorkbenchShell();
 				String vmName= fLabelProvider.getText(target);
 				IStatus status;
 				if (exception == null) {
 					status= new Status(IStatus.WARNING, getPluginId(), IStatus.WARNING, DebugUIMessages.getString("JDIDebugUIPlugin.The_target_VM_does_not_support_hot_code_replace_1"), null); //$NON-NLS-1$
 				} else {
 					status= exception.getStatus();
 				}
 				DebugErrorDialog dialog= new DebugErrorDialog(shell, DebugUIMessages.getString("JDIDebugUIPlugin.Hot_code_replace_failed_1"), //$NON-NLS-1$
 					MessageFormat.format(DebugUIMessages.getString("JDIDebugUIPlugin.{0}_was_unable_to_replace_the_running_code_with_the_code_in_the_workspace._2"), //$NON-NLS-1$
 					new Object[] {vmName}), status, IStatus.OK | IStatus.INFO | IStatus.WARNING | IStatus.ERROR, IJDIPreferencesConstants.ALERT_HCR_FAILED,
 					DebugUIMessages.getString("JDIDebugUIPlugin.Always_alert_me_of_hot_code_replace_failure_1")); //$NON-NLS-1$
 				dialog.open();
 			}
 		});
 	}
 	/**
 	 * @see IJavaHotCodeReplaceListener#hotCodeReplaceSucceeded()
 	 */
 	public void hotCodeReplaceSucceeded() {
 	}
 	
 	/**
 	 * @see IJavaHotCodeReplaceListener#obsoleteMethods(IJavaDebugTarget)
 	 */
 	public void obsoleteMethods(final IJavaDebugTarget target) {
 		if (!getPreferenceStore().getBoolean(IJDIPreferencesConstants.ALERT_OBSOLETE_METHODS)) {
 			return;
 		}
 		getDisplay().asyncExec(new Runnable() {
 			public void run() {
 				Shell shell= getActiveWorkbenchShell();
 				String vmName= fLabelProvider.getText(target);
 				IStatus status;
 				status= new Status(IStatus.WARNING, getPluginId(), IStatus.WARNING, DebugUIMessages.getString("JDIDebugUIPlugin.Stepping_may_be_hazardous_1"), null); //$NON-NLS-1$
 				DebugErrorDialog dialog= new DebugErrorDialog(shell, DebugUIMessages.getString("JDIDebugUIPlugin.Obsolete_methods_remain_1"), //$NON-NLS-1$
 					MessageFormat.format(DebugUIMessages.getString("JDIDebugUIPlugin.{0}_contains_obsolete_methods_1"), //$NON-NLS-1$
 					new Object[] {vmName}), status, IStatus.OK | IStatus.INFO | IStatus.WARNING | IStatus.ERROR, IJDIPreferencesConstants.ALERT_OBSOLETE_METHODS,
 					DebugUIMessages.getString("JDIDebugUIPlugin.Always_alert_me_of_obsolete_methods_1")); //$NON-NLS-1$
 				dialog.open();
 			}
 		});
 	}
 
 	/**
 	 * Debug ui thread safe access to a display
 	 */
 	protected Display getDisplay() {
 		//we can rely on not creating a display as we 
 		//prereq the base eclipse ui plugin.
 		return Display.getDefault();
 	}
 	
 	/**
 	 * Returns the image descriptor registry used for this plugin.
 	 */
 	public static ImageDescriptorRegistry getImageDescriptorRegistry() {
 		return getDefault().fImageDescriptorRegistry;
 	}
 	
 	/**
 	 * Returns the standard display to be used. The method first checks, if
 	 * the thread calling this method has an associated disaply. If so, this
 	 * display is returned. Otherwise the method returns the default display.
 	 */
 	public static Display getStandardDisplay() {
 		Display display;
 		display= Display.getCurrent();
 		if (display == null)
 			display= Display.getDefault();
 		return display;		
 	}
 		
 }
 
