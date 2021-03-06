 package org.jboss.tools.ui.bot.ext.view;
 
 import java.util.List;
 import java.util.Vector;
 
 import org.apache.log4j.Logger;
 import org.eclipse.swt.widgets.MenuItem;
 import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
 import org.eclipse.swtbot.swt.finder.SWTBot;
 import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
 import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
 import org.eclipse.swtbot.swt.finder.results.WidgetResult;
 import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
 import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
 import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
 import org.jboss.tools.ui.bot.ext.SWTBotExt;
 import org.jboss.tools.ui.bot.ext.SWTEclipseExt;
 import org.jboss.tools.ui.bot.ext.SWTOpenExt;
 import org.jboss.tools.ui.bot.ext.SWTUtilExt;
 import org.jboss.tools.ui.bot.ext.Timing;
 import org.jboss.tools.ui.bot.ext.gen.IView;
 import org.jboss.tools.ui.bot.ext.helper.ContextMenuHelper;
 import org.jboss.tools.ui.bot.ext.types.IDELabel;
 /**
  * base class for explorer-like helper components
  * @author lzoubek
  *
  */
 public abstract class ExplorerBase extends SWTBotExt {
 	/**
 	 * view object representing current view, MUST be defined in derived constructor (for use, see {@link SWTOpenExt#viewOpen(IView)}
 	 */
 	protected IView viewObject;
 	protected final SWTOpenExt open;
 	Logger log = Logger.getLogger(ExplorerBase.class);
 	public ExplorerBase() {
 		open = new SWTOpenExt(this);
 	}
 	
 	/**
 	 * shows Explorer view
 	 */
 	public void show() {
 		open.viewOpen(viewObject);
 	}
 	/*
 	 * Selects given project in Package Explorer
 	 */
 	public void selectProject(String projectName) {
 		SWTBot viewBot = viewByTitle(viewObject.getName()).bot();
 		viewBot.tree().expandNode(projectName).select();
 	}
 	  /**
 	   * Selects Tree Item within Package Explorer
 	   * @param treeItemText
 	   * @param path
 	   * @return
 	   */
 	  public SWTBotTreeItem selectTreeItem(String treeItemText, String[] path) {
 	    return SWTEclipseExt.getTreeItemOnPath(viewByTitle(viewObject.getName()).bot().tree(),
 	      treeItemText, path)
 	      .select();
 	  }
 	/**
 	 * deletes given project from workspace
 	 * @param projectName
 	 * @param fileSystem if true, project will be also deleted from file-system
 	 */
 	public void deleteProject(String projectName, boolean fileSystem) {
 		log.info("Deleting project '"+projectName+"'");
 		SWTBot viewBot = viewByTitle(viewObject.getName()).bot();
 		SWTBotTreeItem item = viewBot.tree().expandNode(projectName);
 		ContextMenuHelper.prepareTreeItemForContextMenu(viewBot.tree(), item);
 		new SWTBotMenu(ContextMenuHelper.getContextMenu(viewBot.tree(), IDELabel.Menu.DELETE, false)).click();
 	     shell("Delete Resources").activate();
 	     if (fileSystem) {
 	    	 checkBox().click();
 	     }
 	     open.finish(this,IDELabel.Button.OK);
 	     new SWTUtilExt(this).waitForNonIgnoredJobs();	     
 	}
 	/**
 	 * deletes all projects from workspace
 	 */
 	public void deleteAllProjects() {
 		SWTBot viewBot = viewByTitle(viewObject.getName()).bot();
 		    List<String> items = new Vector<String>();
 		    for (SWTBotTreeItem ti : viewBot.tree().getAllItems()) {
 		    	items.add(ti.getText());
 		    }
 		    for (String proj : items) {
 		    	try {
 		    		viewBot.tree().expandNode(proj);
 		    		viewBot.tree().select(proj);
 		    		// try to select project in tree (in some cases, when one project is deleted, 
 		    		// the other item in tree (not being a project) is auto-deleted)
 		    		
 		    	} catch (WidgetNotFoundException ex) {
 		    		log.warn("Attempted to delete non-existing project '"+proj+"'");
 		    		continue;
 		    	}
 		    	deleteProject(proj,true);
 		    }
 	}
 	/**
 	 * opens file (selects in tree and doubleclicks)
 	 * @param projectName
 	 * @param path to file
 	 * @return editor with opened file
 	 */
 	public SWTBotEditor openFile(String projectName, String... path) {
		SWTBot viewBot = viewByTitle(IDELabel.View.PROJECT_EXPLORER).bot();
		viewByTitle(IDELabel.View.PROJECT_EXPLORER).show();
		viewByTitle(IDELabel.View.PROJECT_EXPLORER).setFocus();
 		SWTBotTree tree = viewBot.tree();
 		SWTBotTreeItem item = tree.expandNode(projectName);
 		StringBuilder builder = new StringBuilder(projectName);
 		// Go through path
 		for (String nodeName : path) {
 			item = item.expandNode(nodeName);
 			builder.append("/" + nodeName);
 		}
 		item.select().doubleClick();
 		log.info("File Opened:" + builder.toString());
 		SWTBotEditor editor = activeEditor();
 		return editor;
 	}
 	/**
 	 * runs given project on Server (uses default server, the first one) server MUST be running
 	 * @param projectName
 	 */
 	public void runOnServer(String projectName) {
		SWTBot viewBot = viewByTitle(viewObject.getName()).bot();
 		SWTBotTreeItem item = viewBot.tree().expandNode(projectName);
 		ContextMenuHelper.prepareTreeItemForContextMenu(viewBot.tree(), item);
 		   final SWTBotMenu menuRunAs = viewBot.menu(IDELabel.Menu.RUN).menu(IDELabel.Menu.RUN_AS);
 		    final MenuItem menuItem = UIThreadRunnable
 		      .syncExec(new WidgetResult<MenuItem>() {
 		        public MenuItem run() {
 		          int menuItemIndex = 0;
 		          MenuItem menuItem = null;
 		          final MenuItem[] menuItems = menuRunAs.widget.getMenu().getItems();
 		          while (menuItem == null && menuItemIndex < menuItems.length){
 		            if (menuItems[menuItemIndex].getText().indexOf("Run on Server") > - 1){
 		              menuItem = menuItems[menuItemIndex];
 		            }
 		            else{
 		              menuItemIndex++;
 		            }
 		          }
 		        return menuItem;
 		        }
 		      });
 		    if (menuItem != null){
 		      new SWTBotMenu(menuItem).click();
 		      shell(IDELabel.Shell.RUN_ON_SERVER).activate();
 		      open.finish(this);		      
 		      new SWTUtilExt(this).waitForAll(Timing.time3S());
 		    }
 		    else{
 		      throw new WidgetNotFoundException("Unable to find Menu Item with Label 'Run on Server'");
 		    }
 		
 	}
 	/**
 	 * true if resource described by parameters exists in ProjectExplorer
 	 * @param projectName project name
 	 * @param resource path (e.g. 'Project' 'src' 'org.jbosstools.test' 'MyClass.java')
 	 * @return 
 	 */
 	public boolean existsResource(String... resource) {
 		
 		try {
 			SWTBot viewBot = viewByTitle(viewObject.getName()).bot();
 			SWTBotTreeItem ancestor = viewBot.tree().getTreeItem(resource[0]);
 			viewBot.tree().expandNode(resource[0]);
 			for (int i=1;i<resource.length;i++) {
 				ancestor = getItem(ancestor, resource[i]);
 				if (ancestor == null) {
 					return false;
 				}				
 			}
 			return true;
 			}
 			catch (WidgetNotFoundException ex) {
 				ex.printStackTrace();
 				return false;
 			}
 	}
 	private SWTBotTreeItem getItem(SWTBotTreeItem ancestor, String name) {
 		try {
 			return ancestor.expandNode(name);
 		}
 		catch (WidgetNotFoundException ex) {
 			return null;
 		}
 	}
 	
 	
 
 }
