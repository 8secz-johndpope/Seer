 package org.eclipse.jdt.internal.debug.ui.launcher;
 
 /*
  * (c) Copyright IBM Corp. 2000, 2001.
  * All Rights Reserved.
  */
  
 import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
 import org.eclipse.debug.ui.CommonTab;
 import org.eclipse.debug.ui.ILaunchConfigurationDialog;
 import org.eclipse.debug.ui.ILaunchConfigurationTab;
 
 public class RemoteJavaApplicationTabGroup extends AbstractLaunchConfigurationTabGroup {
 
 	/**
 	 * @see ILaunchConfigurationTabGroup#createTabs(ILaunchConfigurationDialog, String)
 	 */
 	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
 		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[2];
 		tabs[0] = new JavaConnectTab();
		tabs[0].setLaunchConfigurationDialog(dialog);
 		tabs[1] = new CommonTab();
		tabs[1].setLaunchConfigurationDialog(dialog);
 		setTabs(tabs);
 	}
 
 }
