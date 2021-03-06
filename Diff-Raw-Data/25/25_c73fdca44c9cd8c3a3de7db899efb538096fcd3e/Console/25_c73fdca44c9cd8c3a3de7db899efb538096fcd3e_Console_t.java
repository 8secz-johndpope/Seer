 package org.eclipse.team.internal.ccvs.ui;
 
 /*
 * (c) Copyright IBM Corp. 2000, 2002.
  * All Rights Reserved.
  */
  
 import java.util.Vector;
 
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
 import org.eclipse.jface.action.Action;
 import org.eclipse.jface.action.IMenuListener;
 import org.eclipse.jface.action.IMenuManager;
 import org.eclipse.jface.action.IToolBarManager;
 import org.eclipse.jface.action.MenuManager;
 import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
 import org.eclipse.jface.text.Document;
 import org.eclipse.jface.text.IDocument;
 import org.eclipse.jface.text.ITextOperationTarget;
 import org.eclipse.jface.text.TextViewer;
 import org.eclipse.jface.viewers.ISelectionChangedListener;
 import org.eclipse.jface.viewers.SelectionChangedEvent;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Menu;
 import org.eclipse.ui.IActionBars;
 import org.eclipse.ui.IWorkbenchPage;
 import org.eclipse.ui.PartInitException;
 import org.eclipse.ui.part.ViewPart;
 import org.eclipse.ui.texteditor.ITextEditorActionConstants;
 
 /**
  * Console is a view that displays the communication with the CVS server
  */
 public class Console extends ViewPart {
 
 	public final static String CONSOLE_ID = "org.eclipse.team.ccvs.ui.console";
 	
 	private TextViewer viewer;
 	private TextViewerAction copyAction;
 	private TextViewerAction selectAllAction;
 	private Action clearOutputAction;
 	private IDocument document;
 	
 	// All instances of the console
 	static Vector instances = new Vector();
 
 	/**
 	 * Console Constructor
 	 */
 	public Console() {
 		instances.add(this);
		document = new Document("");
 	}
 
 	/**
 	 * Returns all instances of the console view
 	 * 
 	 * @return all instances of the console
 	 */
 	public static Console[] getInstances() {
 		return (Console[])instances.toArray(new Console[instances.size()]);
 	}
 
 	/**
 	 * Create contributed actions
 	 */
 	protected void createActions() {
 		clearOutputAction = new Action(Policy.bind("Console.clearOutput"), CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_CLEAR)) {
 			public void run() {
 				clearOutput();
 			}
 		};
 		
 		MenuManager mgr = new MenuManager();
 		mgr.setRemoveAllWhenShown(true);
 		mgr.addMenuListener(new IMenuListener() {
 			public void menuAboutToShow(IMenuManager mgr) {
 				fillContextMenu(mgr);
 			}
 		});
 		Menu menu = mgr.createContextMenu(viewer.getControl());
 		viewer.getControl().setMenu(menu);
 
 		// Create the local tool bar
 		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
 		tbm.add(clearOutputAction);
 		tbm.update(false);
 
 
 		// Create actions for the text editor
 		IActionBars actionBars = getViewSite().getActionBars();
 		
 		copyAction = new TextViewerAction(viewer, ITextOperationTarget.COPY);
 		copyAction.setText(Policy.bind("Console.copy"));
 		actionBars.setGlobalActionHandler(ITextEditorActionConstants.COPY, copyAction);
 		
 		selectAllAction = new TextViewerAction(viewer, ITextOperationTarget.SELECT_ALL);
 		selectAllAction.setText(Policy.bind("Console.selectAll"));
 		actionBars.setGlobalActionHandler(ITextEditorActionConstants.SELECT_ALL, selectAllAction);
 
 		actionBars.updateActionBars();
 	}
 
 	/**
 	 * Append the given string to the console
 	 * 
 	 * @param message  the string to append
 	 */
 	public void append(final String message) {
 		getViewSite().getShell().getDisplay().syncExec(new Runnable() {
 			public void run() {
				try {
					document.replace(document.getLength(), 0, message);
					if (message.length() > 0 && viewer != null) {
						viewer.revealRange(document.getLength() - 1, 1);
					}
				} catch (BadLocationException e) {
					IStatus status = new Status(IStatus.ERROR, CVSUIPlugin.ID, 1, null, e);
					ErrorDialog.openError(getViewSite().getShell(), Policy.bind("simpleInternal"), Policy.bind("internal"), status);
					CVSUIPlugin.log(status);
				}
 			}
 		});
 	}
 
 	/**
 	 * Append the given string to all consoles
 	 * 
 	 * @param message  the string to append
 	 */
 	public static void appendAll(String message) {
 		// Show the console
 		try {
 			IWorkbenchPage page = CVSUIPlugin.getActivePage();
 			if (page != null) {
 				page.showView(CONSOLE_ID);
 			}
 		} catch (PartInitException e) {
 			CVSUIPlugin.log(e.getStatus());
 		}
 		
 		if (message.length() == 0) return;
 		// XXX What was the purpose of this?
 //		boolean appendCr = message.charAt(message.length() - 1) == '\n';
 		Console[] consoles = Console.getInstances();
 		for (int i = 0; i < consoles.length; i++) {
 			consoles[i].append(message);
 //			if (appendCr) {
 //				consoles[i].append("\n");
 //			}
 		}
 	}
 	
 	/**
 	 * Clear the output of the console
 	 */
 	public void clearOutput() {
 		document.set("");
 	}
 
 	/**
 	 * Add the actions to the context menu
 	 * 
 	 * @param manager  the manager of the context menu
 	 */
 	protected void fillContextMenu(IMenuManager manager) {
 		manager.add(copyAction);
 		manager.add(selectAllAction);
 		manager.add(new Separator());
 		manager.add(clearOutputAction);
 	}
 
 	/*
 	 * @see WorkbenchPart#setFocus()
 	 */
 	public void setFocus() {
 		viewer.getTextWidget().setFocus();
 	}
 
 	/*
 	 * @see WorkbenchPart#createPartControl(Composite)
 	 */
 	public void createPartControl(Composite parent) {
 		Composite composite = new Composite(parent, SWT.NULL);
 		GridLayout layout = new GridLayout();
 		layout.marginHeight = 0;
 		layout.marginWidth = 0;
 		composite.setLayout(layout);
 		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
 	
 		viewer = new TextViewer(composite, SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
 		GridData data = new GridData(GridData.FILL_BOTH);
 		viewer.setEditable(false);
 		viewer.getControl().setLayoutData(data);
 		viewer.setDocument(document);
 		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
 			public void selectionChanged(SelectionChangedEvent event) {
 				copyAction.update();
 			}
 		});
 		createActions();
 	}
 
 	/*
 	 * @see WorkbenchPart#dispose
 	 */
 	public void dispose() {
 		instances.remove(this);
 		super.dispose();
 	}
 }
