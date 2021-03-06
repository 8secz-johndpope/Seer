 /**
  * GF Eclipse Plugin
  * http://www.grammaticalframework.org/eclipse/
  * John J. Camilleri, 2012
  * 
  * The research leading to these results has received funding from the
  * European Union's Seventh Framework Programme (FP7/2007-2013) under
  * grant agreement no. FP7-ICT-247914.
  */
 package org.grammaticalframework.eclipse.ui.perspectives;
 
 import org.eclipse.debug.ui.IDebugUIConstants;
 import org.eclipse.ui.IFolderLayout;
 import org.eclipse.ui.IPageLayout;
 import org.eclipse.ui.IPerspectiveFactory;
 import org.eclipse.ui.console.IConsoleConstants;
 import org.grammaticalframework.eclipse.ui.views.GFLibraryTreeView;
 import org.grammaticalframework.eclipse.ui.views.GFTreebankManagerView;
 
 /**
  * Set up custom GF perspective
  * 
  * Ref: http://www.eclipsepluginsite.com/perspectives.html
  * 
  * @author John J. Camilleri
  * 
  */
 public class GFPerspectiveFactory implements IPerspectiveFactory {
 	
 	/**
 	 * Create layout
 	 *
 	 * @return the message console stream
 	 */
 	/* (non-Javadoc)
 	 * @see org.eclipse.ui.IPerspectiveFactory#createInitialLayout(org.eclipse.ui.IPageLayout)
 	 */
 	public void createInitialLayout(IPageLayout layout) {
 		// Get the editor area.
 		String editorArea = layout.getEditorArea();
 
 		// Top left: Resource Navigator view and Bookmarks view placeholder
 		IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, 0.25f, editorArea);
 		topLeft.addView(IPageLayout.ID_PROJECT_EXPLORER);
 		// topLeft.addPlaceholder(IPageLayout.ID_BOOKMARKS);
 
 		// Bottom left: Outline view and Property Sheet view
 		IFolderLayout bottomLeft = layout.createFolder("bottomLeft", IPageLayout.BOTTOM, 0.50f, "topLeft");
 		bottomLeft.addView(IPageLayout.ID_OUTLINE); // outline
 		bottomLeft.addView(GFLibraryTreeView.ID); // external libraries 
 		//bottomLeft.addView(IPageLayout.ID_PROP_SHEET);
 
 		// Bottom right: Console, Problems, Treebank Manager
 		// See also: http://wiki.eclipse.org/FAQ_How_do_I_write_to_the_console_from_a_plug-in%3F
 		IFolderLayout underEditor = layout.createFolder("underEditor", IPageLayout.BOTTOM, 0.66f, editorArea);
 		underEditor.addView(IConsoleConstants.ID_CONSOLE_VIEW); // console
 		underEditor.addView(GFTreebankManagerView.ID); // test manager
		underEditor.addView(IPageLayout.ID_PROBLEM_VIEW); // error log
 		
 		// Add Run/debug buttons
 		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
 	}
 	
 }
