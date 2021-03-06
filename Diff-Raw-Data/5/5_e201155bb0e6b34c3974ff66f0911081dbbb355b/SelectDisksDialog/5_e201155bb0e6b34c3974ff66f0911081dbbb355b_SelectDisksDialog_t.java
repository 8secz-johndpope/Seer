 /*******************************************************************************
  * Copyright (c) 2011 Gluster, Inc. <http://www.gluster.com>
  * This file is part of Gluster Management Console.
  *
  * Gluster Management Console is free software; you can redistribute it and/or 
  * modify it under the terms of the GNU Affero General Public License as published
  * by the Free Software Foundation; either version 3 of the License, or
  * (at your option) any later version.
  *  
  * Gluster Management Console is distributed in the hope that it will be useful, 
  * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
  * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License
  * for more details.
  *  
  * You should have received a copy of the GNU Affero General Public License
  * along with this program. If not, see
  * <http://www.gnu.org/licenses/>.
  *******************************************************************************/
 package com.gluster.storage.management.gui.dialogs;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.eclipse.jface.dialogs.Dialog;
 import org.eclipse.jface.dialogs.IDialogConstants;
 import org.eclipse.jface.dialogs.MessageDialog;
 import org.eclipse.osgi.framework.internal.core.Msg;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.graphics.Point;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Shell;
 
 import com.gluster.storage.management.client.GlusterDataModelManager;
 import com.gluster.storage.management.core.model.Disk;
 import com.gluster.storage.management.core.model.Volume;
 
 public class SelectDisksDialog extends Dialog {
 
 	private CreateVolumeDisksPage disksPage;
 	private List<Disk> allDisks;
 	private List<Disk> selectedDisks;
 
 	/**
 	 * Create the dialog.
 	 * 
 	 * @param parentShell
 	 */
 	public SelectDisksDialog(Shell parentShell, List<Disk> allDisks, List<String> selectedDisks) {
 		super(parentShell);
 		setShellStyle(getShellStyle() | SWT.RESIZE);
 		this.allDisks = allDisks;
 		this.selectedDisks = getSelectedDisks(allDisks, selectedDisks);
 	}
 
 	private List<Disk> getSelectedDisks(List<Disk> allDisks, List<String> selectedDisks) {
 		List<Disk> disks = new ArrayList<Disk>();
 		for (String selectedDisk : selectedDisks) {
			for (Disk disk : allDisks) {
 				String brick[] = selectedDisk.split(":");
				if (disk.getServerName().equals(brick[0]) && disk.getName().equals(brick[1])) {
 					disks.add(disk);
 				}
 			}
 		}
 		return disks;
 	}
 
 	/**
 	 * Create contents of the dialog.
 	 * 
 	 * @param parent
 	 */
 	@Override
 	protected Control createDialogArea(Composite parent) {
 		Composite container = new Composite(parent, SWT.NONE);
 		GridLayout containerLayout = new GridLayout(2, false);
 		container.setLayout(containerLayout);
 		GridData containerLayoutData = new GridData(SWT.FILL, SWT.FILL, true,
 				true);
 		container.setLayoutData(containerLayoutData);
 
 		getShell().setText("Create Volume - Select Disks");
 
 		disksPage = new CreateVolumeDisksPage(container, SWT.NONE, allDisks,
 				selectedDisks);
 
 		return container;
 	}
 
 	/**
 	 * Create contents of the button bar.
 	 * 
 	 * @param parent
 	 */
 	@Override
 	protected void createButtonsForButtonBar(Composite parent) {
 		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
 				true);
 		createButton(parent, IDialogConstants.CANCEL_ID,
 				IDialogConstants.CANCEL_LABEL, false);
 	}
 
 	/**
 	 * Return the initial size of the dialog.
 	 */
 	@Override
 	protected Point getInitialSize() {
 		return new Point(1024, 600);
 	}
 
 	@Override
 	protected void cancelPressed() {
 		super.cancelPressed();
 	}
 
 	@Override
 	protected void okPressed() {
 		if (this.getSelectedDisks().size() == 0) {
 			MessageDialog.openError(getShell(), "Select Disk(s)",
 					"Please select atlease one disk");
 		} else {
 			super.okPressed();
 		}
 	}
 
 	public List<Disk> getSelectedDisks() {
 		return disksPage.getChosenDisks();
 	}
 	
 	public List<String> getSelectedBricks() {
 		return disksPage.getChosenBricks();
 	}
 }
