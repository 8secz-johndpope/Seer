 /******************************************************************************* 
  * Copyright (c) 2011 Red Hat, Inc. 
  * Distributed under license by Red Hat, Inc. All rights reserved. 
  * This program is made available under the terms of the 
  * Eclipse Public License v1.0 which accompanies this distribution, 
  * and is available at http://www.eclipse.org/legal/epl-v10.html 
  * 
  * Contributors: 
  * Red Hat, Inc. - initial API and implementation 
  ******************************************************************************/
 package org.jboss.tools.common.ui;
 
 import org.eclipse.debug.ui.IDebugUIConstants;
 import org.eclipse.jdt.ui.JavaUI;
 import org.eclipse.ui.IFolderLayout;
 import org.eclipse.ui.IPageLayout;
 import org.eclipse.ui.IPerspectiveFactory;
 import org.eclipse.ui.internal.PageLayout;
 import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
 import org.eclipse.ui.navigator.resources.ProjectExplorer;
 import org.eclipse.ui.progress.IProgressConstants;
 
 /**
  * @author Alexey Kazakov
  */
 public class JBossPerspectiveFactory implements IPerspectiveFactory {
 
 	public static final String PERSPECTIVE_ID = "org.jboss.tools.common.ui.JBossPerspective"; //$NON-NLS-1$
 
 	protected static final String ID_SERVERS_VIEW = "org.eclipse.wst.server.ui.ServersView"; //$NON-NLS-1$
 	protected static final String ID_SEARCH_VIEW = "org.eclipse.search.ui.views.SearchView"; //$NON-NLS-1$
 	protected static final String ID_CONSOLE_VIEW = "org.eclipse.ui.console.ConsoleView"; //$NON-NLS-1$
 	protected static final String ID_CHEATSHEET_VIEW = "org.eclipse.ui.cheatsheets.cheatSheetView"; //$NON-NLS-1$
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.ui.IPerspectiveFactory#createInitialLayout(org.eclipse.ui.IPageLayout)
 	 */
 	@Override
 	public void createInitialLayout(IPageLayout layout) {
 		layout.addActionSet("org.eclipse.jst.j2ee.J2eeMainActionSet"); //$NON-NLS-1$
 		layout.addActionSet(JavaUI.ID_ACTION_SET);
 
 		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
 		layout.addActionSet(IDebugUIConstants.DEBUG_ACTION_SET);
 
 		layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);
 
 		layout.addShowViewShortcut(ProjectExplorer.VIEW_ID);
 		layout.addShowViewShortcut(ID_SERVERS_VIEW);
 		layout.addShowViewShortcut(IPageLayout.ID_BOOKMARKS);
 		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
 		layout.addShowViewShortcut(IPageLayout.ID_PROP_SHEET);
 		layout.addShowViewShortcut(IPageLayout.ID_RES_NAV);
 		layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
 
 		// views - search
 		layout.addShowViewShortcut(ID_SEARCH_VIEW);
 		// views - debugging
 		layout.addShowViewShortcut(ID_CONSOLE_VIEW);
 
 		layout.addShowInPart(ProjectExplorer.VIEW_ID);
 
 		String editorArea = layout.getEditorArea();
 
 		// Top left.
 		IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, 0.25f, editorArea);//$NON-NLS-1$
 		topLeft.addView(ProjectExplorer.VIEW_ID);
 		topLeft.addPlaceholder(IPageLayout.ID_RES_NAV);
 		topLeft.addPlaceholder(JavaUI.ID_TYPE_HIERARCHY);
 		topLeft.addPlaceholder(JavaUI.ID_PACKAGES_VIEW);
 
 		// Bottom right.
 		IFolderLayout bottomRight = layout.createFolder("bottomRight", IPageLayout.BOTTOM, 0.7f, editorArea);//$NON-NLS-1$
 		bottomRight.addView(IPageLayout.ID_PROBLEM_VIEW);
 		bottomRight.addView(IPageLayout.ID_PROP_SHEET);
 		bottomRight.addView(ID_SERVERS_VIEW);
 
 		bottomRight.addPlaceholder(IPageLayout.ID_TASK_LIST);
 		bottomRight.addPlaceholder(ID_CONSOLE_VIEW);
 		bottomRight.addPlaceholder(IPageLayout.ID_BOOKMARKS);
 		bottomRight.addPlaceholder(IProgressConstants.PROGRESS_VIEW_ID);
 		bottomRight.addPlaceholder(ID_SEARCH_VIEW);
 
 		// Top right.
 		IFolderLayout topRight = layout.createFolder("topRight", IPageLayout.RIGHT, 0.7f, editorArea);//$NON-NLS-1$
 		topRight.addView(IPageLayout.ID_OUTLINE);
 		// This line is required to force CheatSheetView placeholder, because it added by default as sticky view and 
 		// to make placeholder working it should be removed first 
 		((PageLayout)layout).removePlaceholder(ICheatSheetResource.CHEAT_SHEET_VIEW_ID);
 		topRight.addPlaceholder(ICheatSheetResource.CHEAT_SHEET_VIEW_ID);
 	}
 }
