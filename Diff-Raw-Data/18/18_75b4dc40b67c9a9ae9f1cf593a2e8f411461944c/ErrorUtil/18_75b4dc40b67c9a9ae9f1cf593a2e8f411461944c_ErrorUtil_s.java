 /*******************************************************************************
  * Copyright (c) 2000, 2006 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.help.ui.internal.util;
 
 import org.eclipse.help.internal.base.util.IErrorUtil;
 import org.eclipse.help.ui.internal.Messages;
 import org.eclipse.jface.dialogs.MessageDialog;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.ui.IWorkbenchWindow;
 import org.eclipse.ui.PlatformUI;
 
 /**
 * Utiliy class for common error displaying tasks.
  */
 public class ErrorUtil implements IErrorUtil {
 
 	public void displayError(String msg) {
 		displayErrorDialog(msg);
 	}
 
 	public void displayError(final String msg, Thread uiThread) {
 		try {
 			Display.findDisplay(uiThread).asyncExec(new Runnable() {
 
 				public void run() {
 					displayErrorDialog(msg);
 				}
 			});
 		} catch (Exception e2) {
 		}
 	}
 
 	/**
	 * Immidiately displays error dialog with a given string
 	 * 
 	 * @param msg
 	 *            error message to display and log.
 	 */
 	public static void displayErrorDialog(String msg) {
 		String title = Messages.Help_Error;
 		IWorkbenchWindow workbenchWindow = getActiveWorkbenchWindow();
 		Shell shell;
 		if (workbenchWindow != null) {
 			shell = workbenchWindow.getShell();
 		} else {
 			shell = new Shell();
 		}
 		MessageDialog.openError(shell, title, msg);
 	}
 
 	/**
	 * Immidiately displays an Information dialog with a given string
 	 * 
 	 * @param msg
 	 *            error message to display.
 	 */
 	public static void displayInfoDialog(String msg) {
 		String title = Messages.Help_Info;
 		IWorkbenchWindow workbenchWindow = getActiveWorkbenchWindow();
 		Shell shell;
 		if (workbenchWindow != null) {
 			shell = workbenchWindow.getShell();
 		} else {
 			shell = new Shell();
 		}
 		MessageDialog.openInformation(shell, title, msg);
 	}
 
 	/**
	 * Immidiately displays a Question dialog with a given string (question).
 	 * 
 	 * @return which button(Yes/No) was pressed by user
 	 */
 	public static boolean displayQuestionDialog(String msg) {
 		String title = Messages.Help_Question;
 		IWorkbenchWindow workbenchWindow = getActiveWorkbenchWindow();
 		Shell shell;
 		if (workbenchWindow != null) {
 			shell = workbenchWindow.getShell();
 		} else {
 			shell = new Shell();
 		}
 		return MessageDialog.openQuestion(shell, title, msg);
 	}
 
 	protected static IWorkbenchWindow getActiveWorkbenchWindow() {
 		return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
 	}
 }
