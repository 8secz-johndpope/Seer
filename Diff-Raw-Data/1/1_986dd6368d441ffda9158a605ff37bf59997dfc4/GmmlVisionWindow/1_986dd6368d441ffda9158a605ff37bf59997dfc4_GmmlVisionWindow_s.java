 // PathVisio,
 // a tool for data visualization and analysis using Biological Pathways
 // Copyright 2006-2007 BiGCaT Bioinformatics
 //
 // Licensed under the Apache License, Version 2.0 (the "License"); 
 // you may not use this file except in compliance with the License. 
 // You may obtain a copy of the License at 
 // 
 // http://www.apache.org/licenses/LICENSE-2.0 
 //  
 // Unless required by applicable law or agreed to in writing, software 
 // distributed under the License is distributed on an "AS IS" BASIS, 
 // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 // See the License for the specific language governing permissions and 
 // limitations under the License.
 //
 package gmmlVision;
 
 import gmmlVision.GmmlVision.ApplicationEvent;
 import gmmlVision.GmmlVision.ApplicationEventListener;
 import gmmlVision.sidepanels.TabbedSidePanel;
 import graphics.GmmlDrawing;
 import graphics.GmmlGeneProduct;
 
 import java.io.File;
 import java.lang.reflect.InvocationTargetException;
 import java.net.URL;
 import java.util.Vector;
 
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.jface.action.*;
 import org.eclipse.jface.dialogs.MessageDialog;
 import org.eclipse.jface.dialogs.ProgressMonitorDialog;
 import org.eclipse.jface.preference.PreferenceDialog;
 import org.eclipse.jface.preference.PreferenceManager;
 import org.eclipse.jface.resource.ImageDescriptor;
 import org.eclipse.jface.window.ApplicationWindow;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.SashForm;
 import org.eclipse.swt.custom.ScrolledComposite;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.graphics.Point;
 import org.eclipse.swt.layout.FillLayout;
 import org.eclipse.swt.widgets.*;
 
 import preferences.GmmlPreferenceManager;
 import preferences.GmmlPreferences;
 import search.PathwaySearchComposite;
 import util.SwtUtils.SimpleRunnableWithProgress;
 import visualization.GmmlLegend;
 import visualization.VisualizationManager;
 import data.*;
 import data.GmmlGex.ExpressionDataEvent;
 import data.GmmlGex.ExpressionDataListener;
 import edu.stanford.ejalbert.BrowserLauncher;
 
 
 /**
  * This class is the main class in the GPML project. 
  * It acts as a container for pathwaydrawings and facilitates
  * loading, creating and saving drawings to and from GPML.
  */
 public class GmmlVisionWindow extends ApplicationWindow implements 
 						ApplicationEventListener, ExpressionDataListener
 {
 	private static final long serialVersionUID = 1L;
 	private static int ZOOM_TO_FIT = -1;
 		
 	/**
 	 * {@link Action} to create a new gpml pathway
 	 */
 	private class NewAction extends Action 
 	{
 		GmmlVisionWindow window;
 		public NewAction (GmmlVisionWindow w)
 		{
 			window = w;
 			setText ("&New pathway@Ctrl+N");
 			setToolTipText ("Create new pathway");
 			setImageDescriptor(ImageDescriptor.createFromURL(
 					GmmlVision.getResourceURL("icons/new.gif")));
 		}
 		public void run () {
 			if (GmmlVision.gmmlData == null ||
 				MessageDialog.openQuestion(window.getShell(), "Discard changes?",
 						"Warning: This will discard any changes to " +
 						"the current pathway. Are you sure?"))
 			{
 				GmmlVision.newPathway();
 			}
 		}
 	}
 	private NewAction newAction = new NewAction (this);
 	
 	/**
 	 * {@link Action} to open an gpml pathway
 	 */
 	private class OpenAction extends Action 
 	{
 		GmmlVisionWindow window;
 		public OpenAction (GmmlVisionWindow w)
 		{
 			window = w;
 			setText ("&Open pathway@Ctrl+O");
 			setToolTipText ("Open pathway");
 			setImageDescriptor(ImageDescriptor.createFromURL(GmmlVision.getResourceURL("icons/open.gif")));
 		}
 		public void run () 
 		{
 			FileDialog fd = new FileDialog(window.getShell(), SWT.OPEN);
 			fd.setText("Open");
 			String pwpath = GmmlVision.getPreferences().getString(GmmlPreferences.PREF_DIR_PWFILES);
 			fd.setFilterPath(pwpath);
 			fd.setFilterExtensions(new String[] {"*." + GmmlVision.PATHWAY_FILE_EXTENSION, "*.*"});
 			fd.setFilterNames(new String[] {GmmlVision.PATHWAY_FILTER_NAME, "All files (*.*)"});
 	        String fnMapp = fd.open();
 	        // Only open pathway if user selected a file
 	        
 	        if(fnMapp != null) { 
 	        	GmmlVision.openPathway(fnMapp); 
 	        }
 		}
 	}
 	private OpenAction openAction = new OpenAction (this);
 	
 	/**
 	 * {@link Action} to open an gpml pathway
 	 */
 	private class ImportAction extends Action 
 	{
 		GmmlVisionWindow window;
 		public ImportAction (GmmlVisionWindow w)
 		{
 			window = w;
 			setText ("&Import");
 			setToolTipText ("Import Pathway in GenMAPP format");
 		}
 		public void run () 
 		{
 			FileDialog fd = new FileDialog(window.getShell(), SWT.OPEN);
 			fd.setText("Open");
 			fd.setFilterPath(GmmlVision.getPreferences().getString(GmmlPreferences.PREF_DIR_PWFILES));
 			fd.setFilterExtensions(new String[] {"*." + GmmlVision.GENMAPP_FILE_EXTENSION, "*.*"});
 			fd.setFilterNames(new String[] {GmmlVision.GENMAPP_FILTER_NAME, "All files (*.*)"});
 	        String fnMapp = fd.open();
 	        // Only open pathway if user selected a file
 	        
 	        if(fnMapp != null) { 
 	        	GmmlVision.openPathway(fnMapp); 
 	        }
 		}
 	}
 	private ImportAction importAction = new ImportAction (this);
 	
 	/**
 	 * {@link Action} to save a gpml pathway
 	 */
 	private class SaveAction extends Action 
 	{
 		GmmlVisionWindow window;
 		public SaveAction (GmmlVisionWindow w)
 		{
 			window = w;
 			setText ("&Save pathway@Ctrl+S");
 			setToolTipText ("Save pathway");
 			setImageDescriptor(ImageDescriptor.createFromURL(GmmlVision.getResourceURL("icons/save.gif")));
 		}
 		
 		public void run () {
 			GmmlData gmmlData = GmmlVision.getGmmlData();
 			GmmlDrawing drawing = GmmlVision.getDrawing();
 			
 			double usedZoom = drawing.getZoomFactor() * 100;
 			// Set zoom to 100%
 			drawing.setPctZoom(100);			
 			// Overwrite the existing xml file
 			if (gmmlData.getSourceFile() != null)
 			{
 				try
 				{
 					gmmlData.writeToXml(gmmlData.getSourceFile(), true);
 				}
 				catch (ConverterException e)
 				{
 					String msg = "While writing xml to " 
 							+ gmmlData.getSourceFile().getAbsolutePath();					
 					MessageDialog.openError (window.getShell(), "Error", 
 							"Error: " + msg + "\n\n" + 
 							"See the error log for details.");
 					GmmlVision.log.error(msg, e);
 				}
 			}
 			else
 			{
 				saveAsAction.run();
 			}
 			// Set zoom back
 			drawing.setPctZoom(usedZoom);
 		}
 	}
 	private SaveAction saveAction = new SaveAction(this);
 	
 	/**
 	 * {@link Action} to save a gpml pathway to a file specified by the user
 	 */
 	private class SaveAsAction extends Action 
 	{
 		GmmlVisionWindow window;
 		public SaveAsAction (GmmlVisionWindow w)
 		{
 			window = w;
 			setText ("Save pathway &As");
 			setToolTipText ("Save pathway with new file name");
 		}
 		public void run () {
 			GmmlDrawing drawing = GmmlVision.getDrawing();
 			GmmlData gmmlData = GmmlVision.getGmmlData();
 			// Check if a gpml pathway is loaded
 			if (drawing != null)
 			{
 				FileDialog fd = new FileDialog(window.getShell(), SWT.SAVE);
 				fd.setText("Save");
 				fd.setFilterExtensions(new String[] {"*." + GmmlVision.PATHWAY_FILE_EXTENSION, "*.*"});
 				fd.setFilterNames(new String[] {GmmlVision.PATHWAY_FILTER_NAME, "All files (*.*)"});
 				
 				File xmlFile = gmmlData.getSourceFile();
 				if(xmlFile != null) {
 					fd.setFileName(xmlFile.getName());
 					fd.setFilterPath(xmlFile.getPath());
 				} else {
 					fd.setFilterPath(GmmlVision.getPreferences().getString(GmmlPreferences.PREF_DIR_PWFILES));
 				}
 				String fileName = fd.open();
 				// Only proceed if user selected a file
 				
 				if(fileName == null) return;
 				
 				// Append .gpml extension if not already present
 				if(!fileName.endsWith("." + GmmlVision.PATHWAY_FILE_EXTENSION)) 
 					fileName += "." + GmmlVision.PATHWAY_FILE_EXTENSION;
 				
 				File checkFile = new File(fileName);
 				boolean confirmed = true;
 				// If file exists, ask overwrite permission
 				if(checkFile.exists())
 				{
 					confirmed = MessageDialog.openQuestion(window.getShell(),"",
 					"File already exists, overwrite?");
 				}
 				if(confirmed)
 				{
 					double usedZoom = drawing.getZoomFactor() * 100;
 					// Set zoom to 100%
 					drawing.setPctZoom(100);					
 					// Overwrite the existing xml file
 					try
 					{
 						gmmlData.writeToXml(checkFile, true);
 						// Set zoom back
 						drawing.setPctZoom(usedZoom);
 					}
 					catch (ConverterException e)
 					{
 						String msg = "While writing xml to " 
 							+ checkFile.getAbsolutePath();					
 						MessageDialog.openError (window.getShell(), "Error", 
 								"Error: " + msg + "\n\n" + 
 								"See the error log for details.");
 						GmmlVision.log.error(msg, e);
 					}
 				}
 			}
 			else
 			{
 				MessageDialog.openError (window.getShell(), "Error", 
 					"No gpml file loaded! Open or create a new gpml file first");
 			}			
 		}
 	}
 	private SaveAsAction saveAsAction = new SaveAsAction (this);
 
 	/**
 	 * {@link Action} to save a gpml pathway to a file specified by the user
 	 */
 	private class ExportAction extends Action 
 	{
 		GmmlVisionWindow window;
 		public ExportAction (GmmlVisionWindow w)
 		{
 			window = w;
 			setText ("&Export");
 			setToolTipText ("Export Pathway to GenMAPP format");
 		}
 		public void run () {
 			GmmlDrawing drawing = GmmlVision.getDrawing();
 			GmmlData gmmlData = GmmlVision.getGmmlData();
 			// Check if a gpml pathway is loaded
 			if (drawing != null)
 			{
 				FileDialog fd = new FileDialog(window.getShell(), SWT.SAVE);
 				fd.setText("Save");
 				fd.setFilterExtensions(new String[] {"*." + GmmlVision.GENMAPP_FILE_EXTENSION, "*.*"});
 				fd.setFilterNames(new String[] {GmmlVision.GENMAPP_FILTER_NAME, "All files (*.*)"});
 				
 				File xmlFile = gmmlData.getSourceFile();
 				if(xmlFile != null) {
 					String name = xmlFile.getName();
 					if (name.endsWith("." + GmmlVision.PATHWAY_FILE_EXTENSION))
 					{
 						name = name.substring(0, name.length() - 
 							GmmlVision.PATHWAY_FILE_EXTENSION.length()) +
 							GmmlVision.GENMAPP_FILE_EXTENSION;
 					}
 					fd.setFileName(name);
 					fd.setFilterPath(xmlFile.getPath());
 				} else {
 					fd.setFileName(GmmlVision.getPreferences().getString(GmmlPreferences.PREF_DIR_PWFILES));
 				}
 				String fileName = fd.open();
 				// Only proceed if user selected a file
 				
 				if(fileName == null) return;
 				
 				// Append .mapp extension if not already present
 				if(!fileName.endsWith("." + GmmlVision.GENMAPP_FILE_EXTENSION)) 
 					fileName += "." + GmmlVision.GENMAPP_FILE_EXTENSION;
 				
 				File checkFile = new File(fileName);
 				boolean confirmed = true;
 				// If file exists, ask overwrite permission
 				if(checkFile.exists())
 				{
 					confirmed = MessageDialog.openQuestion(window.getShell(),"",
 					"File already exists, overwrite?");
 				}
 				if(confirmed)
 				{
 					double usedZoom = drawing.getZoomFactor() * 100;
 					// Set zoom to 100%
 					drawing.setPctZoom(100);					
 					// Overwrite the existing xml file
 					try
 					{
 						gmmlData.writeToMapp(checkFile);
 						// Set zoom back
 						drawing.setPctZoom(usedZoom);
 					}
 					catch (ConverterException e)
 					{
 						String msg = "While writing mapp to " 
 							+ checkFile.getAbsolutePath();					
 						MessageDialog.openError (window.getShell(), "Error", 
 								"Error: " + msg + "\n\n" + 
 								"See the error log for details.");
 						GmmlVision.log.error(msg, e);
 					}
 				}
 			}
 			else
 			{
 				MessageDialog.openError (window.getShell(), "Error", 
 					"No pathway to save! Open or create a new pathway first");
 			}			
 		}
 	}
 	private ExportAction exportAction = new ExportAction (this);
 
 	/**
 	 * {@link Action} to close the gpml pathway (does nothing yet)
 	 */
 	private class CloseAction extends Action 
 	{
 		GmmlVisionWindow window;
 		public CloseAction (GmmlVisionWindow w)
 		{
 			window = w;
 			setText ("&Close pathway@Ctrl+W");
 			setToolTipText ("Close this pathway");
 		}
 		public void run () {
 			//TODO: unload drawing, ask to save
 		}
 	}
 	private CloseAction closeAction = new CloseAction(this);
 	
 	/**
 	 * {@link Action} to exit the application
 	 */
 	private class ExitAction extends Action 
 	{
 		GmmlVisionWindow window;
 		public ExitAction (GmmlVisionWindow w)
 		{
 			window = w;
 			setText ("E&xit@Ctrl+X");
 			setToolTipText ("Exit Application");
 		}
 		public void run () {
 			window.close();
 			//TODO: ask to save pathway if content is changed
 		}
 	}
 	private ExitAction exitAction = new ExitAction(this);
 
 	private class PreferencesAction extends Action
 	{
 		GmmlVisionWindow window;
 		public PreferencesAction (GmmlVisionWindow w)
 		{
 			window = w;
 			setText("&Preferences");
 			setToolTipText("Edit preferences");
 		}
 		public void run () {
 			PreferenceManager pg = new GmmlPreferenceManager();
 			PreferenceDialog pd = new PreferenceDialog(window.getShell(), pg);
 			pd.setPreferenceStore(GmmlVision.getPreferences());
 			pd.open();
 		}
 	}
 	private PreferencesAction preferencesAction = new PreferencesAction(this);
 
 	/**
 	 * {@link Action} that zooms a mapp to the specified zoomfactor
 	 */
 	private class ZoomAction extends Action 
 	{
 		GmmlVisionWindow window;
 		int pctZoomFactor;
 		
 		/**
 		 * Constructor for this class
 		 * @param w {@link GmmlVisionWindow} window this action belongs to
 		 * @param newPctZoomFactor the zoom factor as percentage of original
 		 */
 		public ZoomAction (GmmlVisionWindow w, int newPctZoomFactor)
 		{
 			window = w;
 			pctZoomFactor = newPctZoomFactor;
 			if(pctZoomFactor == ZOOM_TO_FIT) 
 			{
 				setText ("Zoom to fit");
 				setToolTipText("Zoom mapp to fit window");
 			}
 			else
 			{
 				setText (pctZoomFactor + " %");
 				setToolTipText ("Zoom mapp to " + pctZoomFactor + " %");
 			}
 		}
 		public void run () {
 			GmmlDrawing drawing = GmmlVision.getDrawing();
 			if (drawing != null)
 			{
 				double newPctZoomFactor = pctZoomFactor;
 				if(pctZoomFactor == ZOOM_TO_FIT) 
 				{
 					Point shellSize = window.sc.getSize();
 					Point drawingSize = drawing.getSize();
 					newPctZoomFactor = (int)Math.min(
 							drawing.getZoomFactor() * 100 * (double)shellSize.x / drawingSize.x,
 							drawing.getZoomFactor() * 100 * (double)shellSize.y / drawingSize.y
 					);
 				} 
 				drawing.setPctZoom(newPctZoomFactor);
 			}
 			else
 			{
 				MessageDialog.openError (window.getShell(), "Error", 
 					"No gpml file loaded! Open or create a new gpml file first");
 			}
 		}
 	}
 	
 	/**
 	 * {@link Action} to select a Gene Database
 	 */
 	private class SelectGdbAction extends Action
 	{
 		GmmlVisionWindow window;
 		public SelectGdbAction(GmmlVisionWindow w)
 		{
 			window = w;
 			setText("Select &Gene Database");
 			setToolTipText("Select Gene Database");
 		}
 		
 		public void run () {			
 			try {
 				DBConnector dbcon = GmmlGdb.getDBConnector();
 				String dbName = dbcon.openChooseDbDialog(getShell());
 				
 				if(dbName == null) return;
 				
 				GmmlGdb.connect(dbName);
 				setStatus("Using Gene Database: '" + GmmlVision.getPreferences().getString(GmmlPreferences.PREF_CURR_GDB) + "'");
 				cacheExpressionData();
 			} catch(Exception e) {
 				String msg = "Failed to open Gene Database; " + e.getMessage();
 				MessageDialog.openError (window.getShell(), "Error", 
 						"Error: " + msg + "\n\n" + 
 						"See the error log for details.");
 				GmmlVision.log.error(msg, e);
 			}
 		}
 	}
 	private SelectGdbAction selectGdbAction = new SelectGdbAction(this);
 	
 	/**
 	 * Loads expression data for all {@link GmmlGeneProduct}s in the loaded pathway
 	 */
 	private void cacheExpressionData()
 	{
 		if(GmmlVision.isDrawingOpen())
 		{
 			GmmlDrawing drawing = GmmlVision.getDrawing();
 			//Check for neccesary connections
 			if(GmmlGex.isConnected() && GmmlGdb.isConnected())
 			{
 				ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
 				try {
 					dialog.run(true, true, GmmlGex.createCacheRunnable(drawing.getMappIds(), drawing.getSystemCodes()));
 					drawing.redraw();
 				} catch(Exception e) {
 					String msg = "while caching expression data: " + e.getMessage();					
 					MessageDialog.openError (getShell(), "Error", 
 							"Error: " + msg + "\n\n" + 
 							"See the error log for details.");
 					GmmlVision.log.error(msg, e);
 				}
 			}
 		}
 	}	
 	
 	/**
 	 * {@link Action} to open a {@link GmmlAboutBox} window
 	 */
 	private class AboutAction extends Action 
 	{
 		GmmlVisionWindow window;
 		public AboutAction (GmmlVisionWindow w)
 		{
 			window = w;
 			setText ("&About");
 			setToolTipText ("About " + Globals.APPLICATION_VERSION_NAME);
 		}
 		public void run () {
 			GmmlAboutBox gmmlAboutBox = new GmmlAboutBox(window.getShell(), SWT.NONE);
 			gmmlAboutBox.open();
 		}
 	}
 	private AboutAction aboutAction = new AboutAction(this);
 	
 	/**
 	 * {@link Action} to open a {@link GmmlAboutBox} window
 	 */
 	private class HelpAction extends Action 
 	{
 		GmmlVisionWindow window;
 		public HelpAction (GmmlVisionWindow w)
 		{
 			window = w;
 			setText ("&Help@F1");
 			setToolTipText ("Opens " + Globals.APPLICATION_VERSION_NAME + " help in your web browser");
 		}
 		public void run () {
 			SimpleRunnableWithProgress rwp = new SimpleRunnableWithProgress(
 					window.getClass(), "openHelp", new Class[] {}, new Object[] {}, null);
 			SimpleRunnableWithProgress.setMonitorInfo("Opening help", IProgressMonitor.UNKNOWN);
 			ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
 			try {
 				dialog.run(true, true, rwp);
 			} catch (InvocationTargetException e) {
 				Throwable cause = e.getCause();
 				String msg = cause == null ? null : cause.getMessage();
 				MessageDialog.openError(getShell(), "Unable to open help",
 				"Unable to open web browser" +
 				(msg == null ? "" : ": " + msg) +
 				"\nYou can open the help page manually:\n" +
 				Globals.HELP_URL);
 			} catch (InterruptedException ignore) {}
 			
 
 		}
 	}
 	private HelpAction helpAction = new HelpAction(this);
 	
 	public static void openHelp() throws Exception {
 		BrowserLauncher bl = new BrowserLauncher(null);
 		bl.openURLinBrowser(Globals.HELP_URL);
 	}
 	
 	private class CopyAction extends Action
 	{
 		GmmlVisionWindow window;
 		public CopyAction (GmmlVisionWindow w)
 		{
 			window = w;
 			setText ("Copy@Ctrl+C");
 			setToolTipText ("Copy selected objects to clipboard");
 		}
 		public void run()
 		{
 			GmmlVision.drawing.copyToClipboard();
 		}
 	}
 		
 	private CopyAction copyAction = new CopyAction(this);
 	
 	private class PasteAction extends Action
 	{
 		GmmlVisionWindow window;
 		public PasteAction (GmmlVisionWindow w)
 		{
 			window = w;
 			setText ("Paste@Ctrl+V");
 			setToolTipText ("Paste contents of clipboard");
 		}
 		public void run()
 		{
 			GmmlVision.drawing.pasteFromClipboad();
 		}
 	}
 	
 	private PasteAction pasteAction = new PasteAction(this);
 				
 	/**
 	 * {@link Action} to switch between edit and view mode
 	 */
 	private class SwitchEditModeAction extends Action implements ApplicationEventListener
 	{
 		final String ttChecked = "Exit edit mode";
 		final String ttUnChecked = "Switch to edit mode to edit the pathway content";
 		GmmlVisionWindow window;
 		public SwitchEditModeAction (GmmlVisionWindow w)
 		{
 			super("&Edit mode", IAction.AS_CHECK_BOX);
 			setImageDescriptor(ImageDescriptor.createFromURL(GmmlVision.getResourceURL("icons/edit.gif")));
 			setToolTipText(ttUnChecked);
 			window = w;
 			
 			GmmlVision.addApplicationEventListener(this);
 		}
 		
 		public void run () {
 			if(GmmlVision.isDrawingOpen())
 			{
 				GmmlDrawing drawing = GmmlVision.getDrawing();
 				if(isChecked())
 				{
 					//Switch to edit mode: show edit toolbar, show property table in sidebar
 					drawing.setEditMode(true);
 					showEditActionsCI(true);
 					rightPanel.getTabFolder().setSelection(1);
 				}
 				else
 				{
 					//Switch to view mode: hide edit toolbar, show backpage browser in sidebar
 					drawing.setEditMode(false);
 					showEditActionsCI(false);
 					rightPanel.getTabFolder().setSelection(0);
 				}
 			}
 			else //No gpml pathway loaded, deselect action and do nothing
 			{
 				setChecked(false);
 			}
 			getCoolBarManager().update(true);
 		}
 		
 		public void setChecked(boolean check) {
 			super.setChecked(check);
 			setToolTipText(check ? ttChecked : ttUnChecked);
 		}
 		
 		public void switchEditMode(boolean edit) {
 			setChecked(edit);
 			run();
 			
 		}
 
 		public void applicationEvent(ApplicationEvent e) {
 			if(e.type == ApplicationEvent.OPEN_PATHWAY) {
 				GmmlVision.getDrawing().setEditMode(isChecked());
 			}
 			else if(e.type == ApplicationEvent.NEW_PATHWAY) {
 				switchEditMode(true);
 			}
 		}
 	}
 	private SwitchEditModeAction switchEditModeAction = new SwitchEditModeAction(this);
 		
 	/**
 	 * {@link Action} to show or hide the right sidepanel
 	 */
 	public class ShowRightPanelAction extends Action
 	{
 		GmmlVisionWindow window;
 		public ShowRightPanelAction (GmmlVisionWindow w)
 		{
 			super("Show &information panel", IAction.AS_CHECK_BOX);
 			window = w;
 			setChecked(true);
 		}
 		
 		public void run() {
 			if(isChecked()) rightPanel.show();
 			else rightPanel.hide();
 		}
 	}
 	public ShowRightPanelAction showRightPanelAction = new ShowRightPanelAction(this);
 	
 	/**
 	 * {@link Action} to add a new element to the gpml pathway
 	 */
 	private class NewElementAction extends Action
 	{
 		GmmlVisionWindow window;
 		int element;
 		
 		/**
 		 * Constructor for this class
 		 * @param e	type of element this action adds; a {@link GmmlDrawing} field constant
 		 */
 		public NewElementAction (int e)
 		{
 			element = e;
 		
 			String toolTipText;
 			URL imageURL = null;
 			toolTipText = null;
 			switch(element) {
 			case GmmlDrawing.NEWLINE: 
 				toolTipText = "Draw new line";
 				imageURL = GmmlVision.getResourceURL("icons/newline.gif");
 				setChecked(false);
 				break;
 			case GmmlDrawing.NEWLINEARROW:
 				toolTipText = "Draw new arrow";
 				imageURL = GmmlVision.getResourceURL("icons/newarrow.gif");
 				setChecked(false);
 				break;
 			case GmmlDrawing.NEWLINEDASHED:
 				toolTipText = "Draw new dashed line";
 				imageURL = GmmlVision.getResourceURL("icons/newdashedline.gif");
 				setChecked(false);
 				break;
 			case GmmlDrawing.NEWLINEDASHEDARROW:
 				toolTipText = "Draw new dashed arrow";
 				imageURL = GmmlVision.getResourceURL("icons/newdashedarrow.gif");
 				setChecked(false);
 				break;
 			case GmmlDrawing.NEWLABEL:
 				toolTipText = "Draw new label";
 				imageURL = GmmlVision.getResourceURL("icons/newlabel.gif");
 				setChecked(false);
 				break;
 			case GmmlDrawing.NEWARC:
 				toolTipText = "Draw new arc";
 				imageURL = GmmlVision.getResourceURL("icons/newarc.gif");
 				setChecked(false);
 				break;
 			case GmmlDrawing.NEWBRACE:
 				toolTipText = "Draw new brace";
 				imageURL = GmmlVision.getResourceURL("icons/newbrace.gif");
 				setChecked(false);
 				break;
 			case GmmlDrawing.NEWGENEPRODUCT:
 				toolTipText = "Draw new geneproduct";
 				imageURL = GmmlVision.getResourceURL("icons/newgeneproduct.gif");
 				setChecked(false);
 				break;
 			case GmmlDrawing.NEWRECTANGLE:
 				imageURL = GmmlVision.getResourceURL("icons/newrectangle.gif");
 				setChecked(false);
 				break;
 			case GmmlDrawing.NEWOVAL:
 				toolTipText = "Draw new oval";
 				imageURL = GmmlVision.getResourceURL("icons/newoval.gif");
 				setChecked(false);
 				break;
 			case GmmlDrawing.NEWTBAR:
 				toolTipText = "Draw new TBar";
 				imageURL = GmmlVision.getResourceURL("icons/newtbar.gif");
 				setChecked(false);
 				break;
 			case GmmlDrawing.NEWRECEPTORROUND:
 				toolTipText = "Draw new round receptor";
 				imageURL = GmmlVision.getResourceURL("icons/newreceptorround.gif");
 				setChecked(false);
 				break;
 			case GmmlDrawing.NEWRECEPTORSQUARE:
 				toolTipText = "Draw new square receptor";
 				imageURL = GmmlVision.getResourceURL("icons/newreceptorsquare.gif");
 				setChecked(false);
 				break;
 			case GmmlDrawing.NEWLIGANDROUND:
 				toolTipText = "Draw new round ligand";
 				imageURL = GmmlVision.getResourceURL("icons/newligandround.gif");
 				setChecked(false);
 				break;
 			case GmmlDrawing.NEWLIGANDSQUARE:
 				toolTipText = "Draw new square ligand";
 				imageURL = GmmlVision.getResourceURL("icons/newligandsquare.gif");
 				setChecked(false);
 				break;
 			case GmmlDrawing.NEWLINEMENU:
 				setMenuCreator(new NewItemMenuCreator(GmmlDrawing.NEWLINEMENU));
 				imageURL = GmmlVision.getResourceURL("icons/newlinemenu.gif");
 				toolTipText = "Draw new line or arrow";
 				break;
 			case GmmlDrawing.NEWLINESHAPEMENU:
 				setMenuCreator(new NewItemMenuCreator(GmmlDrawing.NEWLINESHAPEMENU));
 				imageURL = GmmlVision.getResourceURL("icons/newlineshapemenu.gif");
 				toolTipText = "Draw new ligand or receptor";
 				break;
 			}
 			setToolTipText(toolTipText);
 			setId("newItemAction");
 			if(imageURL != null) setImageDescriptor(ImageDescriptor.createFromURL(imageURL));
 		}
 				
 		public void run () {
 			if(isChecked())
 			{
 				deselectNewItemActions();
 				setChecked(true);
 				GmmlVision.getDrawing().setNewGraphics(element);
 			}
 			else
 			{	
 				GmmlVision.getDrawing().setNewGraphics(GmmlDrawing.NEWNONE);
 			}
 		}
 		
 	}
 	
 	/**
 	 * {@link IMenuCreator} that creates the drop down menus for 
 	 * adding new line-type and -shape elements
 	 */
 	private class NewItemMenuCreator implements IMenuCreator {
 		private Menu menu;
 		int element;
 		
 		/**
 		 * Constructor for this class
 		 * @param e	type of menu to create; one of {@link GmmlDrawing}.NEWLINEMENU
 		 * , {@link GmmlDrawing}.NEWLINESHAPEMENU
 		 */
 		public NewItemMenuCreator(int e) 
 		{
 			element = e;
 		}
 		
 		public Menu getMenu(Menu parent) {
 			return null;
 		}
 
 		public Menu getMenu(Control parent) {
 			if (menu != null)
 				menu.dispose();
 			
 			menu = new Menu(parent);
 			Vector<Action> actions = new Vector<Action>();
 			switch(element) {
 			case GmmlDrawing.NEWLINEMENU:
 				actions.add(new NewElementAction(GmmlDrawing.NEWLINE));
 				actions.add(new NewElementAction(GmmlDrawing.NEWLINEARROW));
 				actions.add(new NewElementAction(GmmlDrawing.NEWLINEDASHED));
 				actions.add(new NewElementAction(GmmlDrawing.NEWLINEDASHEDARROW));
 				break;
 			case GmmlDrawing.NEWLINESHAPEMENU:
 				actions.add(new NewElementAction(GmmlDrawing.NEWLIGANDROUND));
 				actions.add(new NewElementAction(GmmlDrawing.NEWRECEPTORROUND));
 				actions.add(new NewElementAction(GmmlDrawing.NEWLIGANDSQUARE));
 				actions.add(new NewElementAction(GmmlDrawing.NEWRECEPTORSQUARE));
 			}
 			
 			for (Action act : actions)
 			{			
 				addActionToMenu(menu, act);
 			}
 
 			return menu;
 		}
 		
 		protected void addActionToMenu(Menu parent, Action a)
 		{
 			 ActionContributionItem item= new ActionContributionItem(a);
 			 item.fill(parent, -1);
 		}
 		
 		public void dispose() 
 		{
 			if (menu != null)  {
 				menu.dispose();
 				menu = null;
 			}
 		}
 	}
 	
 	/**
 	 * Deselects all {@link NewElementAction}s on the toolbar and sets 
 	 * {@link GmmlDrawing}.newGraphics to {@link GmmlDrawing}.NEWNONE
 	 */
 	public void deselectNewItemActions()
 	{
 		IContributionItem[] items = editActionsCI.getToolBarManager().getItems();
 		for(int i = 0; i < items.length; i++)
 		{
 			if(items[i] instanceof ActionContributionItem)
 			{
 				((ActionContributionItem)items[i]).getAction().setChecked(false);
 			}
 		}
 		GmmlVision.getDrawing().setNewGraphics(GmmlDrawing.NEWNONE);
 	}
 	
 	// Elements of the coolbar
 	ToolBarContributionItem commonActionsCI;
 	ToolBarContributionItem editActionsCI;
 	ToolBarContributionItem viewActionsCI;
 	protected CoolBarManager createCoolBarManager(int style)
 	{
 		createCommonActionsCI();
 		createEditActionsCI();
 		createViewActionsCI();
 		
 		CoolBarManager coolBarManager = new CoolBarManager(style);
 		coolBarManager.setLockLayout(true);
 		
 		coolBarManager.add(commonActionsCI);
 		coolBarManager.add(viewActionsCI);
 		return coolBarManager;
 	}
 	
 	/**
 	 * Creates element of the coolbar containing common actions as new, save etc.
 	 */
 	protected void createCommonActionsCI()
 	{
 		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
 		toolBarManager.add(newAction);
 		toolBarManager.add(openAction);
 		toolBarManager.add(saveAction);
 		commonActionsCI = new ToolBarContributionItem(toolBarManager, "CommonActions");
 	}
 	
 	/**
 	 * Creates element of the coolbar only shown in edit mode (new element actions)
 	 */
 	protected void createEditActionsCI()
 	{
 		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);		
 		toolBarManager.add(new NewElementAction(GmmlDrawing.NEWGENEPRODUCT));
 		toolBarManager.add(new NewElementAction(GmmlDrawing.NEWLABEL));
 		toolBarManager.add(new NewElementAction(GmmlDrawing.NEWLINEMENU));
 		toolBarManager.add(new NewElementAction(GmmlDrawing.NEWRECTANGLE));
 		toolBarManager.add(new NewElementAction(GmmlDrawing.NEWOVAL));
 		toolBarManager.add(new NewElementAction(GmmlDrawing.NEWARC));
 		toolBarManager.add(new NewElementAction(GmmlDrawing.NEWBRACE));
 		toolBarManager.add(new NewElementAction(GmmlDrawing.NEWTBAR));
 		toolBarManager.add(new NewElementAction(GmmlDrawing.NEWLINESHAPEMENU));
 		
 		editActionsCI = new ToolBarContributionItem(toolBarManager, "EditModeActions");
 	}
 	
 	/**
 	 * Creates element of the coolbar containing controls related to viewing a pathway
 	 */
 	protected void createViewActionsCI()
 	{
 		final GmmlVisionWindow window = this;
 		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
 		//Add zoomCombo
 		toolBarManager.add(new ControlContribution("ZoomCombo") {
 			protected Control createControl(Composite parent) {
 				final Combo zoomCombo = new Combo(parent, SWT.DROP_DOWN);
 				zoomCombo.setItems(new String[] { "200%", "100%", "75%", "50%", "Zoom to fit" });
 				zoomCombo.setText("100%");
 				zoomCombo.addSelectionListener(new SelectionAdapter() {
 					public void widgetSelected(SelectionEvent e) {
 						int pctZoom = 100;
 						String zoomText = zoomCombo.getText().replace("%", "");
 						try {
 							pctZoom = Integer.parseInt(zoomText);
 						} catch (Exception ex) { 
 							if(zoomText.equals("Zoom to fit"))
 									{ pctZoom = ZOOM_TO_FIT; } else { return; }
 						}
 						new ZoomAction(window, pctZoom).run();
 					}
 					public void widgetDefaultSelected(SelectionEvent e) { widgetSelected(e); }
 				});
 				return zoomCombo;
 			}
 		});
 		//Add swich to editmode
 		toolBarManager.add(switchEditModeAction);
 		
 		viewActionsCI =  new ToolBarContributionItem(toolBarManager, "SwitchActions");
 	}
 	
 	/**
 	 * Shows or hides the editActionsCI
 	 * @param show	true/false for either show or hide
 	 */
 	private void showEditActionsCI(boolean show)
 	{
 		if(show) {
 			getCoolBarManager().insertAfter(viewActionsCI.getId(), editActionsCI);
 		}
 		else {
 			getCoolBarManager().remove(editActionsCI);
 		}
 //		showVisualizationCI(!show); //Visualizations can show up in edit mode...
 		getCoolBarManager().update(true);
 	}
 		
 	protected StatusLineManager createStatusLineManager() {
 		return super.createStatusLineManager();
 	}
 
 	/**
 	 *Builds and ads a menu to the GmmlVision frame
 	 */
 	protected MenuManager createMenuManager()
 	{
 		MenuManager m = new MenuManager();
 		MenuManager fileMenu = new MenuManager ("&File");
 		fileMenu.add(newAction);
 		fileMenu.add(openAction);
 		fileMenu.add(saveAction);
 		fileMenu.add(saveAsAction);
 		//fileMenu.add(closeAction);
 		fileMenu.add(new Separator());
 		fileMenu.add(importAction);
 		fileMenu.add(exportAction);
 		fileMenu.add(new Separator());
 		fileMenu.add(exitAction);
 		MenuManager editMenu = new MenuManager ("&Edit");
 		editMenu.add(copyAction);
 		editMenu.add(pasteAction);
 		editMenu.add(new Separator());
 		editMenu.add(switchEditModeAction);
 		editMenu.add(preferencesAction);
 		MenuManager viewMenu = new MenuManager ("&View");
 		viewMenu.add(showRightPanelAction);
 		MenuManager zoomMenu = new MenuManager("&Zoom");
 		zoomMenu.add(new ZoomAction(this, 50));
 		zoomMenu.add(new ZoomAction(this, 75));
 		zoomMenu.add(new ZoomAction(this, 100));
 		zoomMenu.add(new ZoomAction(this, 125));
 		zoomMenu.add(new ZoomAction(this, 150));
 		zoomMenu.add(new ZoomAction(this, 200));
 		zoomMenu.add(new ZoomAction(this, ZOOM_TO_FIT));
 		viewMenu.add(zoomMenu);
 		MenuManager dataMenu = new MenuManager ("&Data");
 		dataMenu.add(selectGdbAction);
 		
 		MenuManager helpMenu = new MenuManager ("&Help");
 		helpMenu.add(aboutAction);
 		helpMenu.add(helpAction);
 		m.add(fileMenu);
 		m.add(editMenu);
 		m.add(viewMenu);
 		m.add(dataMenu);
 		m.add(helpMenu);
 		return m;
 	}
 	
 	public GmmlVisionWindow()
 	{
 		this(null);
 	}
 	
 	/**
 	 *Constructor for the GmmlVision class
 	 *Initializes new GmmlVision and sets properties for frame
 	 */
 	public GmmlVisionWindow(Shell shell)
 	{
 		super(shell);
 		
 		addMenuBar();
 		addStatusLine();
 		addCoolBar(SWT.FLAT | SWT.LEFT);
 		
 		GmmlVision.addApplicationEventListener(this);
 		GmmlGex.addListener(this);
 	}
 	
 	public boolean close() {
 		GmmlVision.fireApplicationEvent(
 				new ApplicationEvent(this, ApplicationEvent.CLOSE_APPLICATION));
 		return super.close();
 	}
 	
 	public ScrolledComposite sc;
 	public GmmlBpBrowser bpBrowser; //Browser for showing backpage information
 	public GmmlPropertyTable propertyTable;	//Table showing properties of GmmlGraphics objects
 	SashForm sashForm; //SashForm containing the drawing area and sidebar
 	TabbedSidePanel rightPanel; //side panel containing backbage browser and property editor
 	PathwaySearchComposite pwSearchComposite; //Composite that handles pathway searches and displays results
 	GmmlLegend legend; //Legend to display colorset information
 	protected Control createContents(Composite parent)
 	{		
 		Shell shell = parent.getShell();
 		shell.setSize(800, 600);
 		shell.setLocation(100, 100);
 		
 		shell.setText(Globals.APPLICATION_VERSION_NAME);
 		
 		GmmlVisionMain.loadImages(shell.getDisplay());
 		
 		shell.setImage(GmmlVision.getImageRegistry().get("shell.icon"));
 		
 		Composite viewComposite = new Composite(parent, SWT.NULL);
 		viewComposite.setLayout(new FillLayout());
 		
 		sashForm = new SashForm(viewComposite, SWT.HORIZONTAL);
 		
 		sc = new ScrolledComposite (sashForm, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
 		sc.setFocus();
 		
 		rightPanel = new TabbedSidePanel(sashForm, SWT.NULL);
 		
 		//rightPanel controls
 		bpBrowser = new GmmlBpBrowser(rightPanel.getTabFolder(), SWT.NONE);
 		propertyTable = new GmmlPropertyTable(
 				rightPanel.getTabFolder(), SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
 		pwSearchComposite = new PathwaySearchComposite(rightPanel.getTabFolder(), SWT.NONE, this);
 		legend = new GmmlLegend(rightPanel.getTabFolder(), SWT.V_SCROLL | SWT.H_SCROLL);
 		Composite visPanel = VisualizationManager.createSidePanel(rightPanel.getTabFolder());
 		
 		rightPanel.addTab(bpBrowser, "Backpage");
 		rightPanel.addTab(propertyTable, "Properties");
 		rightPanel.addTab(pwSearchComposite, "Pathway Search");
 		rightPanel.addTab(legend, "Legend");
 		rightPanel.addTab(visPanel, "Visualization");
 		
 		int sidePanelSize = GmmlVision.getPreferences().getInt(GmmlPreferences.PREF_SIDEPANEL_SIZE);
 		sashForm.setWeights(new int[] {100 - sidePanelSize, sidePanelSize});
 		showRightPanelAction.setChecked(sidePanelSize > 0);
 		
 		rightPanel.getTabFolder().setSelection(0); //select backpage browser tab
 		rightPanel.hideTab("Legend"); //hide legend on startup
 		
 		setStatus("Using Gene Database: '" + GmmlVision.getPreferences().getString(GmmlPreferences.PREF_CURR_GDB) + "'");
 				
 		return parent;
 		
 	};
 	
 	public TabbedSidePanel getSidePanel() { return rightPanel; }
 	
 	public GmmlLegend getLegend() { return legend; }
 	
 	public void showLegend(boolean show) {	
 		if(show && GmmlGex.isConnected()) {
			legend.refresh();
 			if(rightPanel.isVisible("Legend")) return; //Legend already visible, only refresh
 			rightPanel.unhideTab("Legend", 0);
 			rightPanel.selectTab("Legend");
 		}
 		
 		else rightPanel.hideTab("Legend");
 	}
 			
 	/**
 	 * Creates a new empty drawing canvas
 	 * @return the empty {@link GmmlDrawing}
 	 */
 	public GmmlDrawing createNewDrawing()
 	{		
 		return new GmmlDrawing(sc, SWT.NO_BACKGROUND);
 	}
 	
 	public void applicationEvent(ApplicationEvent e) {
 		GmmlDrawing drawing = null;
 		switch(e.type) {
 		case ApplicationEvent.NEW_PATHWAY:
 			drawing = GmmlVision.getDrawing();
 			sc.setContent(drawing);
 			drawing.setSize(drawing.getMappInfo().getBoardSize());
 			break;
 		case ApplicationEvent.OPEN_PATHWAY:
 			drawing = GmmlVision.getDrawing();
 			sc.setContent(drawing);
 			drawing.setSize(drawing.getMappInfo().getBoardSize());
 			if(GmmlGex.isConnected()) cacheExpressionData();
 			break;	
 		}
 	}
 
 	public void expressionDataEvent(ExpressionDataEvent e) {
 		switch(e.type) {
 		case ExpressionDataEvent.CONNECTION_CLOSED:
 			showLegend(false);
 			break;
 		case ExpressionDataEvent.CONNECTION_OPENED:
 			cacheExpressionData();
 			showLegend(true);
 			break;
 		}
 	}
 } // end of class
