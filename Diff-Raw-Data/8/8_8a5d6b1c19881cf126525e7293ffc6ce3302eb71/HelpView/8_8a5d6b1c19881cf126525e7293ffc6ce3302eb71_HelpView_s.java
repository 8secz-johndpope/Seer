 /*******************************************************************************
  * Copyright (c) 2000, 2005 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.help.ui.internal.views;
 
 import org.eclipse.help.*;
 import org.eclipse.help.ui.internal.*;
 import org.eclipse.jface.dialogs.IPageChangeProvider;
 import org.eclipse.jface.dialogs.IPageChangedListener;
 import org.eclipse.jface.dialogs.PageChangedEvent;
 import org.eclipse.jface.viewers.*;
 import org.eclipse.swt.widgets.*;
 import org.eclipse.ui.*;
 import org.eclipse.ui.forms.HyperlinkGroup;
 import org.eclipse.ui.forms.widgets.FormToolkit;
 import org.eclipse.ui.part.ViewPart;
 
 public class HelpView extends ViewPart implements IPartListener2,
 		ISelectionChangedListener, IPageChangedListener {
 	private FormToolkit toolkit;
 	
 	private String firstPageId;
 
 	private ReusableHelpPart reusableHelpPart;
 
 	//private Hashtable pageRecs;
 
 	private IWorkbenchPart monitoredPart;
 
 	private boolean visible;
 
 	/**
 	 * 
 	 */
 	public HelpView() {
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
 	 */
 	public void createPartControl(Composite parent) {
 		toolkit = new FormToolkit(parent.getDisplay());
 		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
 				HyperlinkGroup.UNDERLINE_HOVER);
 		// toolkit.setBackground(toolkit.getColors().createNoContentBackground());
 		toolkit.getColors().initializeSectionToolBarColors();
 		reusableHelpPart.createControl(parent, toolkit);
 		reusableHelpPart.setDefaultContextHelpText(Messages.HelpView_defaultText); //$NON-NLS-1$
 		reusableHelpPart.showPage(getFirstPage());
 		IWorkbenchWindow window = PlatformUI.getWorkbench()
 				.getActiveWorkbenchWindow();
 		if (window == null)
 			return;
 		IWorkbenchPage page = window.getActivePage();
 		if (page == null)
 			return;
 		IWorkbenchPartReference aref = page.getActivePartReference();
 		if (aref != null)
 			handlePartActivation(aref);
 	}
 
 	public void dispose() {
 		IWorkbenchWindow window = PlatformUI.getWorkbench()
 				.getActiveWorkbenchWindow();
 		IPartService service = window.getPartService();
 		if (monitoredPart != null) {
 			uninstallSelectionListener(monitoredPart);
 			uninstallPageListener(monitoredPart);
 		}
 		service.removePartListener(this);
 		if (reusableHelpPart != null) {
 			reusableHelpPart.dispose();
 			reusableHelpPart = null;
 		}
 		if (toolkit != null) {
 			toolkit.dispose();
 			toolkit = null;
 		}
 		super.dispose();
 	}
 
 	public void init(IViewSite site, IMemento memento) throws PartInitException {
 		if (memento!=null)
 			this.firstPageId = memento.getString("pageId"); //$NON-NLS-1$
 		init(site);
 		reusableHelpPart = new ReusableHelpPart(site.getWorkbenchWindow(),
 				getHelpPartStyle());
 		IActionBars actionBars = site.getActionBars();
 		reusableHelpPart.init(actionBars, actionBars.getToolBarManager(),
 				actionBars.getStatusLineManager(), memento);
 		IWorkbenchWindow window = PlatformUI.getWorkbench()
 				.getActiveWorkbenchWindow();
 		IPartService service = window.getPartService();
 		service.addPartListener(this);
 	}
 	
     public void saveState(IMemento memento) {
     	if (reusableHelpPart!=null && memento!=null) {
     		String pageId = reusableHelpPart.getCurrentPageId();
     		if (pageId!=null)
     			memento.putString("pageId", pageId); //$NON-NLS-1$
     		reusableHelpPart.saveState(memento);
     	}
     }
 
 	private void handlePartActivation(IWorkbenchPartReference ref) {
 		if (reusableHelpPart == null)
 			return;
 		if (!reusableHelpPart.isMonitoringContextHelp())
 			return;
 		if (isThisPart(ref))
 			return;
 		IWorkbenchPart part = ref.getPart(false);
 		Display display = part.getSite().getShell().getDisplay();
 		Control c = display.getFocusControl();
 		if (c != null && c.isVisible() && !c.isDisposed()) {
 			IContextProvider provider = (IContextProvider) part
 					.getAdapter(IContextProvider.class);
 			if (provider != null) {
 				if (visible)
 					reusableHelpPart.update(provider, part, c);
 				if ((provider.getContextChangeMask() & IContextProvider.SELECTION) != 0) {
 					// context help changes with selections
 					installSelectionListener(part);
 				}
				if (part instanceof IPageChangeProvider)
					installPageListener(part);
 			} else if (visible)
 				reusableHelpPart.update(part, c);
 		}
 	}
 
 	private void installPageListener(IWorkbenchPart part) {
 		if (part instanceof IPageChangeProvider)
 			((IPageChangeProvider)part).addPageChangedListener(this);
 		monitoredPart = part;
 	}
 	
 	private void uninstallPageListener(IWorkbenchPart part) {
 		if (part instanceof IPageChangeProvider)
 			((IPageChangeProvider)part).removePageChangedListener(this);
 		monitoredPart = null;
 	}
 
 	private void installSelectionListener(IWorkbenchPart part) {
 		ISelectionProvider provider = part.getSite().getSelectionProvider();
 		if (provider instanceof IPostSelectionProvider)
 			((IPostSelectionProvider) provider)
 				.addPostSelectionChangedListener(this);
 		else
 			provider.addSelectionChangedListener(this);
 		monitoredPart = part;
 	}
 
 	private void uninstallSelectionListener(IWorkbenchPart part) {
 		ISelectionProvider provider = part.getSite().getSelectionProvider();
 		if (provider instanceof IPostSelectionProvider)
 			((IPostSelectionProvider) provider)
 					.removePostSelectionChangedListener(this);
 		else
 			provider.removeSelectionChangedListener(this);
 		monitoredPart = null;
 	}
 
 	private boolean isThisPart(IWorkbenchPartReference ref) {
 		IWorkbenchPart part = ref.getPart(false);
 		return part != null && part.equals(this);
 	}
 
 	private void updateActivePart() {
 		if (reusableHelpPart == null)
 			return;
 		if (!reusableHelpPart.isMonitoringContextHelp())
 			return;
 		if (monitoredPart == null)
 			return;
 		IContextProvider provider = (IContextProvider) monitoredPart
 				.getAdapter(IContextProvider.class);
 		Control c = monitoredPart.getSite().getShell().getDisplay()
 				.getFocusControl();
 		if (c != null && c.isDisposed() == false && visible) {
 			if (provider != null)
 				reusableHelpPart.update(provider, monitoredPart, c);
 			else
 				reusableHelpPart.update(monitoredPart, c);
 		}
 	}
 
 	private void handlePartDeactivation(IWorkbenchPartReference ref) {
 		IWorkbenchPart part = ref.getPart(false);
 		if (monitoredPart != null && part != null && part.equals(monitoredPart)) {
 			uninstallSelectionListener(part);
 			uninstallPageListener(part);
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.ui.IPartListener2#partActivated(org.eclipse.ui.IWorkbenchPartReference)
 	 */
 	public void partActivated(final IWorkbenchPartReference partRef) {
 		if (isThisPart(partRef)) {
 			visible = true;
 			hook(true);
 			selectionChanged(null);
 		} else {
 			handlePartActivation(partRef);
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.ui.IPartListener2#partBroughtToTop(org.eclipse.ui.IWorkbenchPartReference)
 	 */
 	public void partBroughtToTop(IWorkbenchPartReference partRef) {
 		if (isThisPart(partRef)) {
 			visible = true;
 			hook(true);
 			selectionChanged(null);
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.ui.IPartListener2#partClosed(org.eclipse.ui.IWorkbenchPartReference)
 	 */
 	public void partClosed(IWorkbenchPartReference partRef) {
 		handlePartDeactivation(partRef);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.ui.IPartListener2#partDeactivated(org.eclipse.ui.IWorkbenchPartReference)
 	 */
 	public void partDeactivated(IWorkbenchPartReference partRef) {
 		handlePartDeactivation(partRef);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.ui.IPartListener2#partHidden(org.eclipse.ui.IWorkbenchPartReference)
 	 */
 	public void partHidden(IWorkbenchPartReference partRef) {
 		if (isThisPart(partRef)) {
 			visible = false;
 			hook(false);
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.ui.IPartListener2#partInputChanged(org.eclipse.ui.IWorkbenchPartReference)
 	 */
 	public void partInputChanged(IWorkbenchPartReference partRef) {
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.ui.IPartListener2#partOpened(org.eclipse.ui.IWorkbenchPartReference)
 	 */
 	public void partOpened(IWorkbenchPartReference partRef) {
 		if (isThisPart(partRef)) {
 			visible = true;
 			hook(true);
 			selectionChanged(null);
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.ui.IPartListener2#partVisible(org.eclipse.ui.IWorkbenchPartReference)
 	 */
 	public void partVisible(IWorkbenchPartReference partRef) {
 		if (isThisPart(partRef)) {
 			visible = true;
 			hook(true);
 			selectionChanged(null);
 		}
 	}
 
 	private void hook(boolean doHook) {
 		if (doHook) {
 			IWorkbenchWindow window = PlatformUI.getWorkbench()
 					.getActiveWorkbenchWindow();
 			IPartService service = window.getPartService();
 			IWorkbenchPartReference aref = service.getActivePartReference();
 			if (aref != null)
 				handlePartActivation(aref);
 		} else {
 			if (monitoredPart != null) {
 				uninstallSelectionListener(monitoredPart);
 				uninstallPageListener(monitoredPart);
 			}
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
 	 */
 	public void selectionChanged(SelectionChangedEvent event) {
 		if (!visible)
 			return;
 		getSite().getShell().getDisplay().asyncExec(new Runnable() {
 			public void run() {
 				updateActivePart();
 			}
 		});
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.help.ui.internal.views.BaseHelpView#getFirstPage()
 	 */
 	protected String getFirstPage() {
 		if (firstPageId!=null)
 			return firstPageId;
 		return IHelpUIConstants.HV_CONTEXT_HELP_PAGE;
 	}
 
 	public void displayContext(IContext context, IWorkbenchPart part,
 			Control control) {
 		if (reusableHelpPart != null) {
 			// Ensure that context help is currently showing
 			reusableHelpPart.showPage(IHelpUIConstants.HV_CONTEXT_HELP_PAGE);
 			// check if there is a dynamic version
 			IContextProvider provider = (IContextProvider) part
 					.getAdapter(IContextProvider.class);
 			if (provider != null)
 				reusableHelpPart.update(provider, part, control);
 			else
 				reusableHelpPart.update(context, part, control);
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.help.ui.internal.views.BaseHelpView#getHelpPartStyle()
 	 */
 	protected int getHelpPartStyle() {
 		return ReusableHelpPart.ALL_TOPICS | ReusableHelpPart.CONTEXT_HELP
 				| ReusableHelpPart.SEARCH | ReusableHelpPart.BOOKMARKS;
 	}
 
 	public void setFocus() {
 		if (reusableHelpPart != null)
 			reusableHelpPart.setFocus();
 	}
 
 	public void startSearch(String phrase) {
 		if (reusableHelpPart != null)
 			reusableHelpPart.startSearch(phrase);
 	}
 	public void showDynamicHelp(IWorkbenchPart part, Control c) {
 		if (reusableHelpPart != null)
 			reusableHelpPart.showDynamicHelp(part, c);
 	}
 
 	public void pageChanged(PageChangedEvent event) {
 		updateActivePart();
 	}
 }
