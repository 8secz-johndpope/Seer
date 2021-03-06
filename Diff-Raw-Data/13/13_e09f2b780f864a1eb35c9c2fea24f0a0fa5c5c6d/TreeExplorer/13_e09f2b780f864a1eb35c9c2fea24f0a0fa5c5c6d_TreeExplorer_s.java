 /**
  * Copyright (C) 2012 BonitaSoft S.A.
  * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 2.0 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.bonitasoft.studio.common.jface;
 
 import java.util.HashSet;
 import java.util.Set;
 
 import org.bonitasoft.studio.common.Messages;
 import org.eclipse.core.runtime.Assert;
 import org.eclipse.jface.layout.GridDataFactory;
 import org.eclipse.jface.layout.GridLayoutFactory;
 import org.eclipse.jface.viewers.ArrayContentProvider;
 import org.eclipse.jface.viewers.ColumnWeightData;
 import org.eclipse.jface.viewers.DoubleClickEvent;
 import org.eclipse.jface.viewers.IDoubleClickListener;
 import org.eclipse.jface.viewers.ILabelProvider;
 import org.eclipse.jface.viewers.ISelectionChangedListener;
 import org.eclipse.jface.viewers.IStructuredSelection;
 import org.eclipse.jface.viewers.ITreeContentProvider;
 import org.eclipse.jface.viewers.SelectionChangedEvent;
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.jface.viewers.TableLayout;
 import org.eclipse.jface.viewers.TableViewer;
 import org.eclipse.jface.viewers.TreeViewer;
 import org.eclipse.jface.viewers.Viewer;
 import org.eclipse.jface.viewers.ViewerFilter;
 import org.eclipse.jface.viewers.ViewerSorter;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.ModifyEvent;
 import org.eclipse.swt.events.ModifyListener;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.swt.widgets.TableColumn;
 import org.eclipse.swt.widgets.Text;
 import org.eclipse.swt.widgets.TreeColumn;
 
 /**
  * @author Romain Bioteau
  *
  */
 public class TreeExplorer extends Composite implements SWTBotConstants{
 
 	private TreeViewer leftTree;
 	private TableViewer rightTable;
 	private ITreeContentProvider contentProvider;
 	private ILabelProvider labelProvider;
 	private Composite additionalComposite;
 
 	public TreeExplorer(Composite parent, int style) {
 		super(parent, style);
 		setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
 
 		final Text searchField = new Text(this, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
 		searchField.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
 		searchField.setMessage(Messages.filterLabel);
 		
 		additionalComposite = new Composite(this, SWT.NONE);
 		additionalComposite.setLayoutData(GridDataFactory.fillDefaults().grab(false, false).hint(0, 0).create());
 		additionalComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).margins(0, 0).create());
 
 		final Composite content = new Composite(this, SWT.BORDER | SWT.FLAT);
 		content.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(false).spacing(0, 0).margins(0, 0).create());
 		content.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).span(2, 1).create());
 
 		leftTree = new TreeViewer(content, SWT.V_SCROLL );
 		leftTree.getTree().setData(SWTBOT_WIDGET_ID_KEY, SWTBOT_ID_EXPLORER_LEFT_TREE);
		leftTree.getTree().setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
 		final Label vSeparator = new Label(content, SWT.SEPARATOR | SWT.VERTICAL);
 		vSeparator.setLayoutData(GridDataFactory.fillDefaults().grab(false, true).create());
 		rightTable = new TableViewer(content, SWT.NONE);
 		rightTable.getTable().setData(SWTBOT_WIDGET_ID_KEY, SWTBOT_ID_EXPLORER_RIGHT_TABLE);
		rightTable.getTable().setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
 		rightTable.setSorter(new ViewerSorter());
 		rightTable.addFilter(new ViewerFilter() {
 			
 			@Override
 			public boolean select(Viewer viewer, Object parentElement, Object element) {
 				Object leftSelection =((IStructuredSelection) leftTree.getSelection()).getFirstElement();
 				Set<Object> parents = new HashSet<Object>();
 				Object p = contentProvider.getParent(element);
 				if(p != null){
 					parents.add(p);
 				}
 				while (p != null) {
 					p = contentProvider.getParent(p);
 					if(p != null){
 						parents.add(p);
 					}
 				}
 				if(searchField.getText().isEmpty()){
 					return leftSelection == null || parents.contains(leftSelection);
 				}
 				final String text = labelProvider.getText(element);
 				return text != null && text.toLowerCase().contains(searchField.getText().toLowerCase()) && ( leftSelection == null || parents.contains(leftSelection)) ;
 			}
 		});
 		leftTree.addSelectionChangedListener(new ISelectionChangedListener() {
 
 			@Override
 			public void selectionChanged(SelectionChangedEvent event) {
 				Object selection =	((IStructuredSelection) event.getSelection()).getFirstElement();
 				if(selection != null){
 					rightTable.refresh(null);
 				}
 			}
 		});
 		
 		leftTree.addDoubleClickListener(new IDoubleClickListener() {
 			
 			@Override
 			public void doubleClick(DoubleClickEvent event) {
 				Object selection =  ((IStructuredSelection) event.getSelection()).getFirstElement() ;
 		        if(selection != null){
 		        	if(leftTree.getExpandedState(selection)){
 		        		leftTree.collapseToLevel(selection, 1) ;
 		        	}else{
 		        		leftTree.expandToLevel(selection, 1) ;
 		        	}
 		        	
 		        }
 			}
 		});
 		
 		rightTable.addSelectionChangedListener(new ISelectionChangedListener() {
 
 			@Override
 			public void selectionChanged(SelectionChangedEvent event) {
 				Object selection =	((IStructuredSelection) event.getSelection()).getFirstElement();
 				if(selection != null){
 					Object parent = contentProvider.getParent(selection);
 					if(parent != null){
 						leftTree.setSelection(new StructuredSelection(parent),true);
 					}
 				}
 			}
 		});
 		
 		searchField.addModifyListener(new ModifyListener() {
 			
 			@Override
 			public void modifyText(ModifyEvent e) {
 				Display.getDefault().asyncExec(new Runnable() {
 					
 					@Override
 					public void run() {
 						rightTable.refresh();
 					}
 				});
 				
 			}
 		});
 	}
 
 	protected Object[] getSubtree(Object selection) {
 		Set<Object> result = new HashSet<Object>();
 		addChildren(result,selection);
 		return result.toArray();
 	}
 	
 	private void addChildren(Set<Object> result, Object element) {
 		if(contentProvider.hasChildren(element)){
 			for(Object c : contentProvider.getChildren(element)){
 				result.add(c);
 				addChildren(result, c);
 			}
 		}
 	}
 
 	public void setContentProvider(ITreeContentProvider contentProvider){
 		this.contentProvider = contentProvider;
 	}
 
 	public void addLeftTreeFilter(ViewerFilter filter){
 		leftTree.addFilter(filter);
 	}
 
 	public void addRightTreeFilter(ViewerFilter filter){
 		rightTable.addFilter(filter);
 	}
 
 	public void removeTreeFilter(ViewerFilter filter) {
 		rightTable.removeFilter(filter);
 	}
 
 	
 	public void setLabelProvider(ILabelProvider labelProvider){
 		this.labelProvider = labelProvider;
 	}
 
 	public void setInput(Object input){
 		Assert.isNotNull(contentProvider);
 		Assert.isNotNull(labelProvider);
 		leftTree.setContentProvider(contentProvider);
 		leftTree.setLabelProvider(labelProvider);
 		rightTable.setContentProvider(new ArrayContentProvider());
 		rightTable.setLabelProvider(labelProvider);
 		leftTree.setInput(input);
 	}
 	
 	public Composite getAdditionalComposite() {
 		return additionalComposite;
 	}
 
 	public void setLeftHeader(String title) {
 		leftTree.getTree().setHeaderVisible(true);
 		final TreeColumn columnName = new TreeColumn(leftTree.getTree(), SWT.NONE);
 		columnName.setText(title);
 		TableLayout layout = new TableLayout();
 		layout.addColumnData(new ColumnWeightData(1));
 		leftTree.getTree().setLayout(layout);
 	}
 
 	public void setRightHeader(String title) {
 		rightTable.getTable().setHeaderVisible(true);
 		final TableColumn columnName = new TableColumn(rightTable.getTable(), SWT.NONE);
 		columnName.setText(title);
 		TableLayout layout = new TableLayout();
 		layout.addColumnData(new ColumnWeightData(1));
 		rightTable.getTable().setLayout(layout);
 	}
 
 	public Viewer getRightTableViewer() {
 		return rightTable;
 	}
 	
 	public TreeViewer geLeftTreeViewer() {
 		return leftTree;
 	}
 
 	
 }
