 /**********************************************************************
 Copyright (c) 2003 CSC SoftwareConsult GmbH & Co. OHG, Germany and others.
 All rights reserved.   This program and the accompanying materials
 are made available under the terms of the Common Public License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/legal/cpl-v10.html
 
 Contributors:
 CSC - Intial implementation
 	
 **********************************************************************/
 
 package org.eclipse.team.internal.ccvs.ui;
 
 import org.eclipse.jface.viewers.ColumnWeightData;
 import org.eclipse.jface.viewers.ILabelProviderListener;
 import org.eclipse.jface.viewers.IStructuredContentProvider;
 import org.eclipse.jface.viewers.ITableLabelProvider;
 import org.eclipse.jface.viewers.TableLayout;
 import org.eclipse.jface.viewers.TableViewer;
 import org.eclipse.jface.viewers.Viewer;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Table;
 import org.eclipse.swt.widgets.TableColumn;
 import org.eclipse.team.internal.ccvs.core.EditorsInfo;
 import org.eclipse.ui.IWorkbenchPage;
 import org.eclipse.ui.PartInitException;
 import org.eclipse.ui.part.ViewPart;
 
 /**
  * 
  * The <code>EditorsView</code> shows the result of cvs editors command
  * 
  * @author <a href="mailto:gregor.kohlwes@csc.com,kohlwes@gmx.net">Gregor Kohlwes</a>
  * @see org.eclipse.team.internal.ccvs.ui.actions.ShowEditorsAction
  */
 public class EditorsView extends ViewPart {
 	public static final String VIEW_ID = "org.eclipse.team.ccvs.ui.EditorsView"; //$NON-NLS-1$
 
 	private Table table;
 	private TableViewer tableViewer;
 	
 	/**
 	 * Makes the editors view visible in the active perspective. If there isn't
 	 * a editors view registered <code>null</code> is returned. Otherwise the
 	 * opened view part is returned.
 	 */
 	public static EditorsView openInActivePerspective() {
 		try {
 
 			IWorkbenchPage page = CVSUIPlugin.getActivePage();
 			if (page == null)
 				return null;
 			return (EditorsView)page.showView(VIEW_ID);
 		} catch (PartInitException pe) {
 			return null;
 		}
 	}
 
 
 	class EditorsContentProvider implements IStructuredContentProvider {
 
 		/**
 		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
 		 */
 		public Object[] getElements(Object inputElement) {
 			return (EditorsInfo[]) inputElement;
 		}
 
 		/**
 		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
 		 */
 		public void dispose() {
 		}
 
 		/**
 		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
 		 */
 		public void inputChanged(
 			Viewer viewer,
 			Object oldInput,
 			Object newInput) {
 		}
 
 	}
 
 	class EditorsLabelProvider implements ITableLabelProvider {
 		/**
 		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
 		 */
 		public Image getColumnImage(Object element, int columnIndex) {
 			return null;
 		}
 
 		/**
 		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
 		 */
 		public String getColumnText(Object element, int columnIndex) {
 			if (element == null)
 				return ""; //$NON-NLS-1$
 			EditorsInfo info = (EditorsInfo) element;
 
			String result = null;
 			switch (columnIndex) {
 				case 0 :
					result = info.getFileName();
					break;
 				case 1 :
					result = info.getUserName();
					break;
 				case 2 :
					result = info.getDateString();
					break;
 				case 3 :
					result = info.getComputerName();
					break;
 			}
			// This method must not return null
			if (result == null) result = ""; //$NON-NLS-1
			return result;
 
 		}
 
 		/**
 		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
 		 */
 		public void addListener(ILabelProviderListener listener) {
 		}
 
 		/**
 		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
 		 */
 		public void dispose() {
 		}
 
 		/**
 		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
 		 */
 		public boolean isLabelProperty(Object element, String property) {
 			return false;
 		}
 
 		/**
 		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
 		 */
 		public void removeListener(ILabelProviderListener listener) {
 		}
 
 	}
 
 	/**
 	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
 	 */
 	public void createPartControl(Composite parent) {
 		table =	new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
 		table.setHeaderVisible(true);
 		table.setLinesVisible(true);
 
 		GridData gridData = new GridData(GridData.FILL_BOTH);
 		gridData.widthHint=500;
 		gridData.heightHint=100;
 		table.setLayoutData(gridData);
 
 		TableLayout layout = new TableLayout();
 		table.setLayout(layout);
 
 		tableViewer = new TableViewer(table);
 		createColumns(table, layout);
 
 		tableViewer.setContentProvider(new EditorsContentProvider());
 		tableViewer.setLabelProvider(new EditorsLabelProvider());
 	}
 	public void setInput(EditorsInfo[] infos) {
 		tableViewer.setInput(infos);
 	}
 	/**
 	 * Method createColumns.
 	 * @param table
 	 * @param layout
 	 * @param viewer
 	 */
 	private void createColumns(Table table, TableLayout layout) {
 
 		TableColumn col;
 		// file name
 		col = new TableColumn(table, SWT.NONE);
 		col.setResizable(true);
 		col.setText(Policy.bind("EditorsView.file")); //$NON-NLS-1$
 		layout.addColumnData(new ColumnWeightData(30, true));
 
 		// user name
 		col = new TableColumn(table, SWT.NONE);
 		col.setResizable(true);
 		col.setText(Policy.bind("EditorsView.user")); //$NON-NLS-1$
 		layout.addColumnData(new ColumnWeightData(20, true));
 
 		// creation date
 		col = new TableColumn(table, SWT.NONE);
 		col.setResizable(true);
 		col.setText(Policy.bind("EditorsView.date")); //$NON-NLS-1$
 		layout.addColumnData(new ColumnWeightData(30, true));
 
 		// computer name
 		col = new TableColumn(table, SWT.NONE);
 		col.setResizable(true);
 		col.setText(Policy.bind("EditorsView.computer")); //$NON-NLS-1$
 		layout.addColumnData(new ColumnWeightData(20, true));
 
 	}
 
 	/**
 	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
 	 */
 	public void setFocus() {
 	}
 	/**
 	 * Method getTable.
 	 */
 	public Table getTable() {
 		return table;
 	}
 
 }
