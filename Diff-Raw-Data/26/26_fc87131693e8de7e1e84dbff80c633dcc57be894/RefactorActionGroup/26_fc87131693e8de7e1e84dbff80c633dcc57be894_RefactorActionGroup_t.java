 /*******************************************************************************
  * Copyright (c) 2000, 2003 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.team.internal.ui.synchronize.actions;
 
import java.util.*;
 import java.util.Iterator;
import java.util.List;
 
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.IAdaptable;
 import org.eclipse.jface.action.IMenuManager;
 import org.eclipse.jface.action.MenuManager;
 import org.eclipse.jface.viewers.IStructuredSelection;
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.team.internal.ui.Policy;
 import org.eclipse.team.internal.ui.Utils;
 import org.eclipse.team.ui.synchronize.ISynchronizeView;
 import org.eclipse.ui.*;
 import org.eclipse.ui.actions.*;
 
 /**
  * This action group is modeled after the class of the same name in 
  * the org.eclipse.ui.workbench plugin. We couldn't reuse that class
  * because of a hard dependency on the navigator.
  */
 public class RefactorActionGroup extends ActionGroup {
 
 	private MoveResourceAction moveAction;
 	private RenameResourceAction renameAction;
 	private TextActionHandler textActionHandler;
 	private ISynchronizeView view;
	private DeleteResourceAction deleteAction;
	
 	public RefactorActionGroup(ISynchronizeView view) {
 		this.view = view;
 		makeActions();
 	}
 
 	public void fillContextMenu(IMenuManager parentMenu) {
 		IStructuredSelection selection = getSelection();
 
 		boolean anyResourceSelected =
 			!selection.isEmpty()
 				&& allResourcesAreOfType(
 					selection,
 					IResource.PROJECT | IResource.FOLDER | IResource.FILE);
 
 		MenuManager menu = new MenuManager(Policy.bind("RefactorActionGroup.0")); //$NON-NLS-1$
 		IStructuredSelection convertedSelection = convertSelection(selection);
 		
 		if (anyResourceSelected) {
 			deleteAction.selectionChanged(convertedSelection);
 			menu.add(deleteAction);
 			moveAction.selectionChanged(convertedSelection);
 			menu.add(moveAction);
 			renameAction.selectionChanged(convertedSelection);
 			menu.add(renameAction);
 		}
 		parentMenu.add(menu);
 	}
 
 	private IStructuredSelection convertSelection(IStructuredSelection selection) {
 		return new StructuredSelection(Utils.getResources(selection.toArray()));
 	}
 
 	public void fillActionBars(IActionBars actionBars) {
 		textActionHandler = new TextActionHandler(actionBars); // hooks handlers
 		textActionHandler.setDeleteAction(deleteAction);
 		renameAction.setTextActionHandler(textActionHandler);		
		deleteAction.selectionChanged(getSelection());
 	}
 
 	protected void makeActions() {
 		// Get the key binding service for registering actions with commands. 
 		final IWorkbenchPartSite site = view.getSite();
 		final IKeyBindingService keyBindingService = site.getKeyBindingService();
 		
 		Shell shell = site.getShell();
 		ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
 		
 		moveAction = new MoveResourceAction(shell);
 		renameAction = new RenameResourceAction(shell);
 		
		deleteAction = new DeleteResourceAction(shell) {
			protected List getSelectedResources() {
				return Arrays.asList(Utils.getResources(getSelection().toArray()));
			}
		};
		deleteAction.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));			
 		deleteAction.setActionDefinitionId("org.eclipse.ui.edit.delete");  //$NON-NLS-1$
 		keyBindingService.registerAction(deleteAction);
 	}
 
 	public void updateActionBars() {
 		IStructuredSelection selection = getSelection();
 		deleteAction.selectionChanged(selection);
 		moveAction.selectionChanged(selection);
 		renameAction.selectionChanged(selection);
 	}
 
 	private IStructuredSelection getSelection() {
 		return (IStructuredSelection)view.getSite().getPage().getSelection();
 	}
 
 	private boolean allResourcesAreOfType(IStructuredSelection selection, int resourceMask) {
 		Iterator resources = selection.iterator();
 		while (resources.hasNext()) {
 			Object next = resources.next();
 			IResource resource = null;
 			if (next instanceof IResource) {
 				resource = (IResource)next;
 			} else if (next instanceof IAdaptable) {
 				IAdaptable adaptable = (IAdaptable)next;
 				resource = (IResource)adaptable.getAdapter(IResource.class);
 			}
 			if(resource == null) {
 				IResource[] r = Utils.getResources(new Object[] {next});
 				if(r.length == 1) {
 					resource = r[0];
 				}
 			}
 			if (resource == null || (resource.getType() & resourceMask) == 0) {
 				return false;
 			}
 		}
 		return true;
 	}
 }
