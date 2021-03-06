 /*******************************************************************************
  * Copyright (c) 2004, 2009 Tasktop Technologies and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Tasktop Technologies - initial API and implementation
  *     IBM Corporation - helper methods from 
  *       org.eclipse.wst.common.frameworks.internal.ui.WTPActivityHelper 
  *******************************************************************************/
 
 package org.eclipse.mylyn.internal.provisional.commons.ui;
 
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.Calendar;
 
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.jface.dialogs.MessageDialog;
 import org.eclipse.mylyn.internal.commons.ui.CommonsUiPlugin;
 import org.eclipse.mylyn.internal.commons.ui.Messages;
 import org.eclipse.osgi.util.NLS;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.ui.IPluginContribution;
 import org.eclipse.ui.IWorkbench;
 import org.eclipse.ui.IWorkbenchWindow;
 import org.eclipse.ui.PartInitException;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.activities.IIdentifier;
 import org.eclipse.ui.activities.IWorkbenchActivitySupport;
 import org.eclipse.ui.browser.IWebBrowser;
 import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
 import org.eclipse.ui.internal.browser.WebBrowserPreference;
 import org.eclipse.ui.internal.browser.WorkbenchBrowserSupport;
 
 /**
  * @author Mik Kersten
  * @author Steffen Pingel
  */
 public class WorkbenchUtil {
 
 	// FIXME remove this again
 	private static final boolean TEST_MODE;
 
 	static {
 		String application = System.getProperty("eclipse.application", ""); //$NON-NLS-1$ //$NON-NLS-2$
 		if (application.length() > 0) {
 			TEST_MODE = application.endsWith("testapplication"); //$NON-NLS-1$
 		} else {
 			// eclipse 3.3 does not the eclipse.application property
 			String commands = System.getProperty("eclipse.commands", ""); //$NON-NLS-1$ //$NON-NLS-2$
 			TEST_MODE = commands.contains("testapplication\n"); //$NON-NLS-1$
 		}
 	}
 
 //	public static IViewPart getFromActivePerspective(String viewId) {
 //		if (PlatformUI.isWorkbenchRunning()) {
 //			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
 //			if (activePage != null) {
 //				return activePage.findView(viewId);
 //			}
 //		}
 //		return null;
 //	}
 
 //	public static IViewPart openInActivePerspective(String viewId) throws PartInitException {
 //		if (PlatformUI.isWorkbenchRunning() && PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
 //			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
 //			if (activePage != null) {
 //				return activePage.showView(viewId);
 //			}
 //		}
 //		return null;
 //	}
 
 	/**
 	 * Return the modal shell that is currently open. If there isn't one then return null.
 	 * <p>
 	 * <b>Note: Applied from patch on bug 99472.</b>
 	 * 
 	 * @param shell
 	 *            A shell to exclude from the search. May be <code>null</code>.
 	 * @return Shell or <code>null</code>.
 	 */
 	private static Shell getModalShellExcluding(Shell shell) {
 		IWorkbench workbench = PlatformUI.getWorkbench();
 		Shell[] shells = workbench.getDisplay().getShells();
 		int modal = SWT.APPLICATION_MODAL | SWT.SYSTEM_MODAL | SWT.PRIMARY_MODAL;
 		for (Shell shell2 : shells) {
 			if (shell2.equals(shell)) {
 				break;
 			}
 			// Do not worry about shells that will not block the user.
 			if (shell2.isVisible()) {
 				int style = shell2.getStyle();
 				if ((style & modal) != 0) {
 					return shell2;
 				}
 			}
 		}
 		return null;
 	}
 
 	/**
 	 * Utility method to get the best parenting possible for a dialog. If there is a modal shell create it so as to
 	 * avoid two modal dialogs. If not then return the shell of the active workbench window. If neither can be found
 	 * return null.
 	 * <p>
 	 * <b>Note: Applied from patch on bug 99472.</b>
 	 * 
 	 * @return Shell or <code>null</code>
 	 */
 	public static Shell getShell() {
 		if (!PlatformUI.isWorkbenchRunning() || PlatformUI.getWorkbench().isClosing()) {
 			return null;
 		}
 		Shell modal = getModalShellExcluding(null);
 		if (modal != null) {
 			return modal;
 		}
 		return getNonModalShell();
 	}
 
 	/**
 	 * Get the active non modal shell. If there isn't one return null.
 	 * <p>
 	 * <b>Note: Applied from patch on bug 99472.</b>
 	 * 
 	 * @return Shell
 	 */
 	private static Shell getNonModalShell() {
 		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
 		if (window == null) {
 			IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
 			if (windows.length > 0) {
 				return windows[0].getShell();
 			}
 		} else {
 			return window.getShell();
 		}
 
 		return null;
 	}
 
 	/**
 	 * @return whether the UI is set up to filter contributions (has defined activity categories).
 	 */
 	public static final boolean isFiltering() {
 		return !PlatformUI.getWorkbench().getActivitySupport().getActivityManager().getDefinedActivityIds().isEmpty();
 	}
 
 	public static boolean allowUseOf(Object object) {
 		if (!isFiltering()) {
 			return true;
 		}
 		if (object instanceof IPluginContribution) {
 			IPluginContribution contribution = (IPluginContribution) object;
 			if (contribution.getPluginId() != null) {
 				IWorkbenchActivitySupport workbenchActivitySupport = PlatformUI.getWorkbench().getActivitySupport();
 				IIdentifier identifier = workbenchActivitySupport.getActivityManager().getIdentifier(
 						createUnifiedId(contribution));
 				return identifier.isEnabled();
 			}
 		}
 		if (object instanceof String) {
 			IWorkbenchActivitySupport workbenchActivitySupport = PlatformUI.getWorkbench().getActivitySupport();
 			IIdentifier identifier = workbenchActivitySupport.getActivityManager().getIdentifier((String) object);
 			return identifier.isEnabled();
 		}
 		return true;
 	}
 
 	private static final String createUnifiedId(IPluginContribution contribution) {
 		if (contribution.getPluginId() != null) {
 			return contribution.getPluginId() + '/' + contribution.getLocalId();
 		}
 		return contribution.getLocalId();
 	}
 
 	/**
 	 * Opens <code>location</code> in a web-browser according to the Eclipse workbench preferences.
 	 * 
 	 * @param location
 	 *            the url to open
 	 * @see #openUrl(String, int)
 	 */
 	public static void openUrl(String location) {
 		openUrl(location, SWT.NONE);
 	}
 
 	/**
 	 * Opens <code>location</code> in a web-browser according to the Eclipse workbench preferences.
 	 * 
 	 * @param location
 	 *            the url to open
 	 * @param customFlags
 	 *            additional flags that are passed to {@link IWorkbenchBrowserSupport}, pass
 	 *            {@link IWorkbenchBrowserSupport#AS_EXTERNAL} to force opening external browser
 	 */
 	public static void openUrl(String location, int customFlags) {
 		try {
 			URL url = null;
 			if (location != null) {
 				url = new URL(location);
 			}
 			if (WebBrowserPreference.getBrowserChoice() == WebBrowserPreference.EXTERNAL
 					|| (customFlags & IWorkbenchBrowserSupport.AS_EXTERNAL) != 0) {
 				try {
 					IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
 					support.getExternalBrowser().openURL(url);
 				} catch (PartInitException e) {
 					Status status = new Status(IStatus.ERROR, CommonsUiPlugin.ID_PLUGIN,
 							Messages.WorkbenchUtil_Browser_Initialization_Failed);
 					CommonsUiPlugin.getDefault().getLog().log(status);
 					if (!TEST_MODE) {
 						MessageDialog.openError(getShell(), Messages.WorkbenchUtil_Open_Location_Title,
 								status.getMessage());
 					}
 				}
 			} else {
 				IWebBrowser browser = null;
 				int flags = customFlags;
 				if (WorkbenchBrowserSupport.getInstance().isInternalWebBrowserAvailable()) {
 					flags |= IWorkbenchBrowserSupport.AS_EDITOR | IWorkbenchBrowserSupport.LOCATION_BAR
 							| IWorkbenchBrowserSupport.NAVIGATION_BAR;
 				} else {
 					flags |= IWorkbenchBrowserSupport.AS_EXTERNAL | IWorkbenchBrowserSupport.LOCATION_BAR
 							| IWorkbenchBrowserSupport.NAVIGATION_BAR;
 				}
 
 				String generatedId = "org.eclipse.mylyn.web.browser-" + Calendar.getInstance().getTimeInMillis(); //$NON-NLS-1$
 				browser = WorkbenchBrowserSupport.getInstance().createBrowser(flags, generatedId, null, null);
 				browser.openURL(url);
 			}
 		} catch (PartInitException e) {
 			Status status = new Status(IStatus.ERROR, CommonsUiPlugin.ID_PLUGIN,
 					Messages.WorkbenchUtil_Browser_Initialization_Failed, e);
 			CommonsUiPlugin.getDefault().getLog().log(status);
 			if (!TEST_MODE) {
 				MessageDialog.openError(getShell(), Messages.WorkbenchUtil_Open_Location_Title, status.getMessage());
 			}
 		} catch (MalformedURLException e) {
 			if (location != null && location.trim().equals("")) { //$NON-NLS-1$
 				Status status = new Status(IStatus.WARNING, CommonsUiPlugin.ID_PLUGIN,
 						Messages.WorkbenchUtil_No_URL_Error, e);
 				if (!TEST_MODE) {
 					MessageDialog.openWarning(getShell(), Messages.WorkbenchUtil_Open_Location_Title,
 							status.getMessage());
 				} else {
 					CommonsUiPlugin.getDefault().getLog().log(status);
 				}
 			} else {
 				Status status = new Status(IStatus.ERROR, CommonsUiPlugin.ID_PLUGIN, NLS.bind(
 						Messages.WorkbenchUtil_Invalid_URL_Error, location), e);
 				if (!TEST_MODE) {
 					MessageDialog.openError(getShell(), Messages.WorkbenchUtil_Open_Location_Title, status.getMessage());
 				} else {
 					CommonsUiPlugin.getDefault().getLog().log(status);
 				}
 			}
 		}
 	}
 
 }
