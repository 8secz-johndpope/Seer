 /*******************************************************************************
  * Copyright (c) 2010 BestSolution.at and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
  ******************************************************************************/
 package org.eclipse.e4.tools.emf.ui.internal.common.component.virtual;
 
 import java.util.List;
 
 import org.eclipse.core.databinding.observable.list.IObservableList;
 import org.eclipse.core.databinding.observable.value.WritableValue;
 import org.eclipse.e4.tools.emf.ui.common.component.AbstractComponentEditor;
 import org.eclipse.e4.tools.emf.ui.internal.ObservableColumnLabelProvider;
 import org.eclipse.e4.tools.emf.ui.internal.common.ModelEditor;
 import org.eclipse.e4.tools.emf.ui.internal.common.VirtualEntry;
 import org.eclipse.e4.ui.model.application.ui.MElementContainer;
 import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
 import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
 import org.eclipse.e4.ui.model.application.ui.basic.impl.BasicPackageImpl;
 import org.eclipse.e4.ui.model.application.ui.impl.UiPackageImpl;
 import org.eclipse.emf.common.command.Command;
 import org.eclipse.emf.databinding.EMFDataBindingContext;
 import org.eclipse.emf.databinding.edit.EMFEditProperties;
 import org.eclipse.emf.databinding.edit.IEMFEditValueProperty;
 import org.eclipse.emf.ecore.EClass;
 import org.eclipse.emf.ecore.EObject;
 import org.eclipse.emf.ecore.util.EcoreUtil;
 import org.eclipse.emf.edit.command.AddCommand;
 import org.eclipse.emf.edit.command.MoveCommand;
 import org.eclipse.emf.edit.command.RemoveCommand;
 import org.eclipse.emf.edit.domain.EditingDomain;
 import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
 import org.eclipse.jface.viewers.ArrayContentProvider;
 import org.eclipse.jface.viewers.ComboViewer;
 import org.eclipse.jface.viewers.IStructuredSelection;
 import org.eclipse.jface.viewers.LabelProvider;
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.jface.viewers.TableViewer;
 import org.eclipse.jface.viewers.TableViewerColumn;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Label;
 
 public class VWindowEditor extends AbstractComponentEditor {
 	private Composite composite;
 	private EMFDataBindingContext context;
 	private ModelEditor editor;
 	private TableViewer viewer;
 
 	public VWindowEditor(EditingDomain editingDomain, ModelEditor editor) {
 		super(editingDomain);
 		this.editor = editor;
 	}
 
 	@Override
 	public Image getImage(Object element, Display display) {
 		return null;
 	}
 
 	@Override
 	public String getLabel(Object element) {
 		return "Windows";
 	}
 
 	@Override
 	public String getDetailLabel(Object element) {
 		return null;
 	}
 
 	@Override
 	public String getDescription(Object element) {
 		return "Windows Bla Bla Bla Bla Bla";
 	}
 
 	@Override
 	public Composite getEditor(Composite parent, Object object) {
 		if (composite == null) {
 			context = new EMFDataBindingContext();
 			composite = createForm(parent, context, getMaster());
 		}
 		VirtualEntry<?> o = (VirtualEntry<?>) object;
 		viewer.setInput(o.getList());
 		getMaster().setValue(o.getOriginalParent());
 		return composite;
 	}
 
 	private Composite createForm(Composite parent, EMFDataBindingContext context, WritableValue master) {
 		parent = new Composite(parent, SWT.NONE);
 		parent.setLayout(new GridLayout(3, false));
 
 		Label l = new Label(parent, SWT.NONE);
 		l.setText("Windows");
 		l.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
 
 		viewer = new TableViewer(parent);
 		ObservableListContentProvider cp = new ObservableListContentProvider();
 		viewer.setContentProvider(cp);
 
 		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
 		gd.heightHint = 300;
 		viewer.getControl().setLayoutData(gd);
 		viewer.getTable().setHeaderVisible(true);
 
 		{
 			IEMFEditValueProperty valProp = EMFEditProperties.value(getEditingDomain(), UiPackageImpl.Literals.UI_LABEL__LABEL);
 
 			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
 			column.getColumn().setText("Name");
 			column.getColumn().setWidth(180);
 			column.setLabelProvider(new ObservableColumnLabelProvider<MWindow>(valProp.observeDetail(cp.getKnownElements())));
 		}
 
 		{
 			IEMFEditValueProperty valProp = EMFEditProperties.value(getEditingDomain(), BasicPackageImpl.Literals.WINDOW__X);
 
 			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
 			column.getColumn().setText("X");
 			column.getColumn().setWidth(80);
 			column.setLabelProvider(new ObservableColumnLabelProvider<MWindow>(valProp.observeDetail(cp.getKnownElements())));
 		}
 
 		{
 			IEMFEditValueProperty valProp = EMFEditProperties.value(getEditingDomain(), BasicPackageImpl.Literals.WINDOW__Y);
 
 			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
 			column.getColumn().setText("Y");
 			column.getColumn().setWidth(80);
 			column.setLabelProvider(new ObservableColumnLabelProvider<MWindow>(valProp.observeDetail(cp.getKnownElements())));
 		}
 
 		{
 			IEMFEditValueProperty valProp = EMFEditProperties.value(getEditingDomain(), BasicPackageImpl.Literals.WINDOW__WIDTH);
 
 			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
 			column.getColumn().setText("Width");
 			column.getColumn().setWidth(100);
 			column.setLabelProvider(new ObservableColumnLabelProvider<MWindow>(valProp.observeDetail(cp.getKnownElements())));
 		}
 
 		{
 			IEMFEditValueProperty valProp = EMFEditProperties.value(getEditingDomain(), BasicPackageImpl.Literals.WINDOW__HEIGHT);
 
 			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
 			column.getColumn().setText("Height");
 			column.getColumn().setWidth(100);
 			column.setLabelProvider(new ObservableColumnLabelProvider<MWindow>(valProp.observeDetail(cp.getKnownElements())));
 		}
 
 		Composite buttonComp = new Composite(parent, SWT.NONE);
 		buttonComp.setLayoutData(new GridData(GridData.FILL, GridData.END, false, false));
 		GridLayout gl = new GridLayout(2,false);
 		gl.marginLeft = 0;
 		gl.marginRight = 0;
 		gl.marginWidth = 0;
 		gl.marginHeight = 0;
 		buttonComp.setLayout(gl);
 
 		Button b = new Button(buttonComp, SWT.PUSH | SWT.FLAT);
 		
 		b.setText("Up");
 		b.setImage(getImage(b.getDisplay(), ARROW_UP));
 		b.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false,2,1));
 		b.addSelectionListener(new SelectionAdapter() {
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				if( ! viewer.getSelection().isEmpty() ) {
 					IStructuredSelection s = (IStructuredSelection)viewer.getSelection();
 					if( s.size() == 1 ) {
 						Object obj = s.getFirstElement();
 						MElementContainer<?> container = (MElementContainer<?>) getMaster().getValue();
 						int idx = container.getChildren().indexOf(obj) - 1;
 						if( idx >= 0 ) {
 							Command cmd = MoveCommand.create(getEditingDomain(), getMaster().getValue(), UiPackageImpl.Literals.ELEMENT_CONTAINER__CHILDREN, obj, idx);
 							
 							if( cmd.canExecute() ) {
 								getEditingDomain().getCommandStack().execute(cmd);
 								viewer.setSelection(new StructuredSelection(obj));
 							}
 						}
 						
 					}
 				}
 			}
 		});
 
 		b = new Button(buttonComp, SWT.PUSH | SWT.FLAT);
 		b.setText("Down");
 		b.setImage(getImage(b.getDisplay(), ARROW_DOWN));
 		b.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false,2,1));
 		b.addSelectionListener(new SelectionAdapter() {
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				if( ! viewer.getSelection().isEmpty() ) {
 					IStructuredSelection s = (IStructuredSelection)viewer.getSelection();
 					if( s.size() == 1 ) {
 						Object obj = s.getFirstElement();
 						MElementContainer<?> container = (MElementContainer<?>) getMaster().getValue();
 						int idx = container.getChildren().indexOf(obj) + 1;
 						if( idx < container.getChildren().size() ) {
 							Command cmd = MoveCommand.create(getEditingDomain(), getMaster().getValue(), UiPackageImpl.Literals.ELEMENT_CONTAINER__CHILDREN, obj, idx);
 							
 							if( cmd.canExecute() ) {
 								getEditingDomain().getCommandStack().execute(cmd);
 								viewer.setSelection(new StructuredSelection(obj));
 							}
 						}
 						
 					}
 				}
 			}
 		});
 
 		final ComboViewer childrenDropDown = new ComboViewer(buttonComp);
 		childrenDropDown.getControl().setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
 		childrenDropDown.setContentProvider(new ArrayContentProvider());
 		childrenDropDown.setLabelProvider(new LabelProvider() {
 			@Override
 			public String getText(Object element) {
 				EClass eclass = (EClass) element;
 				return eclass.getName();
 			}
 		});
 		childrenDropDown.setInput(new EClass[] { BasicPackageImpl.Literals.WINDOW, BasicPackageImpl.Literals.TRIMMED_WINDOW });
 		childrenDropDown.setSelection(new StructuredSelection(BasicPackageImpl.Literals.WINDOW));
 		
 		b = new Button(buttonComp, SWT.PUSH | SWT.FLAT);
		b.setText("Add ...");
 		b.setImage(getImage(b.getDisplay(), TABLE_ADD_IMAGE));
 		b.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
 		b.addSelectionListener(new SelectionAdapter() {
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				EClass eClass = (EClass) ((IStructuredSelection)childrenDropDown.getSelection()).getFirstElement();
 				EObject handler = EcoreUtil.create(eClass);
 				
 				Command cmd = AddCommand.create(getEditingDomain(), getMaster().getValue(), UiPackageImpl.Literals.ELEMENT_CONTAINER__CHILDREN, handler);
 
 				if (cmd.canExecute()) {
 					getEditingDomain().getCommandStack().execute(cmd);
 					editor.setSelection(handler);
 				}
 			}
 		});
 
 		b = new Button(buttonComp, SWT.PUSH | SWT.FLAT);
 		b.setText("Remove");
 		b.setImage(getImage(b.getDisplay(), TABLE_DELETE_IMAGE));
 		b.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false,2,1));
 		b.addSelectionListener(new SelectionAdapter() {
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				if (!viewer.getSelection().isEmpty()) {
 					List<?> windows = ((IStructuredSelection) viewer.getSelection()).toList();
 					MElementContainer<?> container = (MElementContainer<?>) getMaster().getValue();
 					Command cmd = RemoveCommand.create(getEditingDomain(), container, UiPackageImpl.Literals.ELEMENT_CONTAINER__CHILDREN, windows);
 					if (cmd.canExecute()) {
 						getEditingDomain().getCommandStack().execute(cmd);
 						if( container.getChildren().size() > 0 ) {
 							viewer.setSelection(new StructuredSelection(container.getChildren().get(0)));
 						}
 					}
 				}
 			}
 		});
 
 		return parent;
 	}
 
 	@Override
 	public IObservableList getChildList(Object element) {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 }
