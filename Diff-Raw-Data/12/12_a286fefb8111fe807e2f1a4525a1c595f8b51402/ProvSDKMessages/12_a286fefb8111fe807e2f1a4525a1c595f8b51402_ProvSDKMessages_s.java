 /*******************************************************************************
  * Copyright (c) 2007, 2008 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.equinox.internal.p2.ui.sdk;
 
 import org.eclipse.osgi.util.NLS;
 
 /**
  * Message class for provisioning UI messages.  
  * 
  * @since 3.4
  */
 public class ProvSDKMessages extends NLS {
 	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.ui.sdk.messages"; //$NON-NLS-1$
 	static {
 		// load message values from bundle file
 		NLS.initializeMessages(BUNDLE_NAME, ProvSDKMessages.class);
 	}
 	public static String Handler_CannotLaunchUI;
 	public static String Handler_SDKUpdateUIMessageTitle;
 	public static String InstallNewSoftwareHandler_LoadRepositoryJobLabel;
 	public static String ProvisioningPreferencePage_AlwaysOpenWizard;
 	public static String ProvisioningPreferencePage_BrowsingPrefsGroup;
 	public static String ProvisioningPreferencePage_ShowLatestVersions;
 	public static String ProvisioningPreferencePage_ShowAllVersions;
 	public static String ProvisioningPreferencePage_NeverOpenWizard;
 	public static String ProvisioningPreferencePage_OpenWizardIfInvalid;
 	public static String ProvisioningPreferencePage_PromptToOpenWizard;
 	public static String ProvSDKUIActivator_ErrorWritingLicenseRegistry;
 	public static String ProvSDKUIActivator_LicenseManagerReadError;
 	public static String ProvSDKUIActivator_NoSelfProfile;
 	public static String ProvSDKUIActivator_OpenWizardAnyway;
 	public static String ProvSDKUIActivator_Question;
 }
