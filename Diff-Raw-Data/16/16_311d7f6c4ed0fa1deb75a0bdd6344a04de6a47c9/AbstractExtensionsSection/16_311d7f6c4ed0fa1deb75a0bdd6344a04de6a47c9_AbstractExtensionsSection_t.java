 package org.eclipse.wst.xsd.ui.internal.common.properties.sections;
 
 import java.util.List;
 import org.eclipse.draw2d.ColorConstants;
 import org.eclipse.gef.commands.Command;
 import org.eclipse.jface.action.MenuManager;
 import org.eclipse.jface.viewers.ILabelProvider;
 import org.eclipse.jface.viewers.ISelection;
 import org.eclipse.jface.viewers.ISelectionChangedListener;
 import org.eclipse.jface.viewers.ITreeContentProvider;
 import org.eclipse.jface.viewers.SelectionChangedEvent;
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.jface.viewers.TreeViewer;
 import org.eclipse.jface.window.Window;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.SashForm;
 import org.eclipse.swt.events.MouseTrackAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
 import org.eclipse.swt.widgets.TreeItem;
 import org.eclipse.ui.IEditorPart;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.forms.widgets.Section;
 import org.eclipse.ui.part.PageBook;
 import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
 import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
 import org.eclipse.wst.xsd.ui.internal.common.commands.AddExtensionCommand;
 import org.eclipse.wst.xsd.ui.internal.common.properties.sections.appinfo.AddExtensionsComponentDialog;
 import org.eclipse.wst.xsd.ui.internal.common.properties.sections.appinfo.DOMExtensionDetailsContentProvider;
 import org.eclipse.wst.xsd.ui.internal.common.properties.sections.appinfo.DOMExtensionItemEditManager;
 import org.eclipse.wst.xsd.ui.internal.common.properties.sections.appinfo.DOMExtensionItemMenuListener;
 import org.eclipse.wst.xsd.ui.internal.common.properties.sections.appinfo.ExtensionDetailsViewer;
 import org.eclipse.wst.xsd.ui.internal.common.properties.sections.appinfo.ExtensionsSchemasRegistry;
 import org.eclipse.wst.xsd.ui.internal.common.properties.sections.appinfo.SpecificationForExtensionsSchema;
 import org.w3c.dom.Element;
 
 public abstract class AbstractExtensionsSection extends AbstractSection
 {
   protected ExtensionDetailsViewer extensionDetailsViewer;
   protected TreeViewer extensionTreeViewer;
   protected ITreeContentProvider extensionTreeContentProvider;
   protected ILabelProvider extensionTreeLabelProvider;
   protected Label contentLabel;
   protected ISelectionChangedListener elementSelectionChangedListener;
   protected IDocumentChangedNotifier documentChangeNotifier;
   protected INodeAdapter internalNodeAdapter = new InternalNodeAdapter();
 
   private Composite page, pageBook2;
   private Button addButton, removeButton;
   private PageBook pageBook;
 
   /**
    * 
    */
   public AbstractExtensionsSection()
   {
     super();    
   }
   
   class InternalNodeAdapter implements INodeAdapter
   {
 
     public boolean isAdapterForType(Object type)
     {
       // we don't really need to implement this
       return true;
     }
 
     public void notifyChanged(INodeNotifier notifier, int eventType, Object changedFeature, Object oldValue, Object newValue, int pos)
     { 
       extensionTreeViewer.refresh();      
     }    
   }
 
   public void createContents(Composite parent)
   {
     // TODO (cs) add assertion
     if (extensionTreeContentProvider == null)
        return;
     
     IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
     documentChangeNotifier = (IDocumentChangedNotifier)editor.getAdapter(IDocumentChangedNotifier.class);
     
     if (documentChangeNotifier != null)
     {
       documentChangeNotifier.addListener(internalNodeAdapter);
     }  
     
     composite = getWidgetFactory().createFlatFormComposite(parent);
 
     GridLayout gridLayout = new GridLayout();
     gridLayout.marginTop = 0;
     gridLayout.marginBottom = 0;
     gridLayout.numColumns = 1;
     composite.setLayout(gridLayout);
 
     GridData gridData = new GridData();
 
     page = getWidgetFactory().createComposite(composite);
     gridLayout = new GridLayout();
     gridLayout.marginTop = 0;
     gridLayout.marginBottom = 0;
     gridLayout.numColumns = 1;
     page.setLayout(gridLayout);
 
     gridData = new GridData();
     gridData.grabExcessHorizontalSpace = true;
     gridData.grabExcessVerticalSpace = true;
     gridData.verticalAlignment = GridData.FILL;
     gridData.horizontalAlignment = GridData.FILL;
     page.setLayoutData(gridData);
 
     pageBook = new PageBook(page, SWT.FLAT);
     gridData = new GridData();
     gridData.grabExcessHorizontalSpace = true;
     gridData.grabExcessVerticalSpace = true;
     gridData.verticalAlignment = GridData.FILL;
     gridData.horizontalAlignment = GridData.FILL;
     pageBook.setLayoutData(gridData);
 
     pageBook2 = getWidgetFactory().createComposite(pageBook, SWT.FLAT);
 
     gridLayout = new GridLayout();
     gridLayout.marginHeight = 2;
     gridLayout.marginWidth = 2;
     gridLayout.numColumns = 1;
     pageBook2.setLayout(gridLayout);
 
     gridData = new GridData();
     gridData.grabExcessHorizontalSpace = true;
     gridData.grabExcessVerticalSpace = true;
     gridData.verticalAlignment = GridData.FILL;
     gridData.horizontalAlignment = GridData.FILL;
     pageBook2.setLayoutData(gridData);
 
     SashForm sashForm = new SashForm(pageBook2, SWT.HORIZONTAL);
     gridData = new GridData();
     gridData.grabExcessHorizontalSpace = true;
     gridData.grabExcessVerticalSpace = true;
     gridData.verticalAlignment = GridData.FILL;
     gridData.horizontalAlignment = GridData.FILL;
     sashForm.setLayoutData(gridData);
     sashForm.setForeground(ColorConstants.white);
     sashForm.setBackground(ColorConstants.white);
     Control[] children = sashForm.getChildren();
     for (int i = 0; i < children.length; i++)
     {
       children[i].setVisible(false);
     }
     Composite leftContent = getWidgetFactory().createComposite(sashForm, SWT.FLAT);
     gridLayout = new GridLayout();
     gridLayout.numColumns = 1;
     leftContent.setLayout(gridLayout);
 
     Section section = getWidgetFactory().createSection(leftContent, SWT.FLAT | Section.TITLE_BAR);
     section.setText("Extensions");
     section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
 
     Composite tableAndButtonComposite = getWidgetFactory().createComposite(leftContent, SWT.FLAT);
     tableAndButtonComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
     gridLayout = new GridLayout();
     gridLayout.numColumns = 2;
     tableAndButtonComposite.setLayout(gridLayout);    
     
     extensionTreeViewer = new TreeViewer(tableAndButtonComposite, SWT.FLAT | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.LINE_SOLID);
     MenuManager menuManager = new MenuManager();    
     extensionTreeViewer.getTree().setMenu(menuManager.createContextMenu(extensionTreeViewer.getTree()));
     menuManager.addMenuListener(new DOMExtensionItemMenuListener(extensionTreeViewer));
     
     gridLayout = new GridLayout();
     gridLayout.numColumns = 1;
     extensionTreeViewer.getTree().setLayout(gridLayout);
     gridData = new GridData();
     gridData.grabExcessHorizontalSpace = true;
     gridData.grabExcessVerticalSpace = true;
     gridData.verticalAlignment = GridData.FILL;
     gridData.horizontalAlignment = GridData.FILL;
    
     extensionTreeViewer.getTree().setLayoutData(gridData);
     extensionTreeViewer.setContentProvider(extensionTreeContentProvider);
     extensionTreeViewer.setLabelProvider(extensionTreeLabelProvider);
     elementSelectionChangedListener = new ElementSelectionChangedListener();
     extensionTreeViewer.addSelectionChangedListener(elementSelectionChangedListener);
     extensionTreeViewer.getTree().addMouseTrackListener(new MouseTrackAdapter()
     {
       public void mouseHover(org.eclipse.swt.events.MouseEvent e)
       {
         ISelection selection = extensionTreeViewer.getSelection();
         if (selection instanceof StructuredSelection)
         {
           Object obj = ((StructuredSelection) selection).getFirstElement();
           if (obj instanceof Element)
           {
             Element element = (Element) obj;
             ExtensionsSchemasRegistry registry = getExtensionsSchemasRegistry();
             // ApplicationSpecificSchemaProperties[] properties =
             // registry.getAllApplicationSpecificSchemaProperties();
             // ApplicationSpecificSchemaProperties[] properties =
             // (ApplicationSpecificSchemaProperties[])
             // registry.getAllApplicationSpecificSchemaProperties().toArray(new
             // ApplicationSpecificSchemaProperties[0]);
             List properties = registry.getAllExtensionsSchemasContribution();
 
             int length = properties.size();
             for (int i = 0; i < length; i++)
             {
               SpecificationForExtensionsSchema current = (SpecificationForExtensionsSchema) properties.get(i);
               if (current.getNamespaceURI().equals(element.getNamespaceURI()))
               {
                 extensionTreeViewer.getTree().setToolTipText(current.getDescription());
                 break;
               }
             }
           }
         }
       };
 
     });
     
     Composite buttonComposite = getWidgetFactory().createComposite(tableAndButtonComposite, SWT.FLAT);
     //ColumnLayout columnLayout = new ColumnLayout();
     //buttonComposite.setLayout(columnLayout);
     buttonComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
     gridLayout = new GridLayout();
     gridLayout.marginTop = 0;
     gridLayout.marginBottom = 0;
     gridLayout.numColumns = 1;
     gridLayout.makeColumnsEqualWidth = true;
     buttonComposite.setLayout(gridLayout);
     
     addButton = getWidgetFactory().createButton(buttonComposite, "Add...", SWT.FLAT);   
     addButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
     addButton.addSelectionListener(this);
     addButton.setToolTipText("Add Extension Component");
     //addButton.setLayoutData(new ColumnLayoutData(ColumnLayoutData.FILL));
     addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
     removeButton = getWidgetFactory().createButton(buttonComposite, "Remove", SWT.FLAT);
     removeButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
     removeButton.addSelectionListener(this);
     removeButton.setToolTipText("Remove Extension Component");
     //removeButton.setLayoutData(new ColumnLayoutData(ColumnLayoutData.FILL));
     
     Button up = getWidgetFactory().createButton(buttonComposite, "Up", SWT.FLAT);
     //up.setLayoutData(new ColumnLayoutData(ColumnLayoutData.FILL));
     up.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
     
     Button down = getWidgetFactory().createButton(buttonComposite, "Down", SWT.FLAT);
     //down.setLayoutData(new ColumnLayoutData(ColumnLayoutData.FILL));    
     down.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
 
     Composite rightContent = getWidgetFactory().createComposite(sashForm, SWT.FLAT);
     Section section2 = getWidgetFactory().createSection(rightContent, SWT.FLAT | Section.TITLE_BAR);
     section2.setText("Extension Details");
     section2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
     //contentLabel = getWidgetFactory().createLabel(rightContent, "Content");
 
     Composite testComp = getWidgetFactory().createComposite(rightContent, SWT.FLAT);
 
     gridLayout = new GridLayout();
     gridLayout.marginTop = 0;
     gridLayout.marginBottom = 0;
     gridLayout.marginLeft = 0;
     gridLayout.marginRight = 0;
     gridLayout.numColumns = 1;
     gridLayout.marginHeight = 3;
     gridLayout.marginWidth = 3;
     rightContent.setLayout(gridLayout);
 
     gridData = new GridData();
     gridData.grabExcessHorizontalSpace = true;
     gridData.grabExcessVerticalSpace = true;
     gridData.verticalAlignment = GridData.FILL;
     gridData.horizontalAlignment = GridData.FILL;
     rightContent.setLayoutData(gridData);
 
     gridLayout = new GridLayout();
     gridLayout.marginTop = 0;
     gridLayout.marginLeft = 0;
     gridLayout.marginRight = 0;
     gridLayout.marginBottom = 0;
     gridLayout.marginHeight = 3;
     gridLayout.marginWidth = 3;
     gridLayout.numColumns = 2;
     testComp.setLayout(gridLayout);
 
     gridData = new GridData();
     gridData.grabExcessHorizontalSpace = true;
     gridData.grabExcessVerticalSpace = true;
     gridData.verticalAlignment = GridData.FILL;
     gridData.horizontalAlignment = GridData.FILL;
     testComp.setLayoutData(gridData);
 
     createElementContentWidget(testComp);
 
     int[] weights = { 50, 50 };
     sashForm.setWeights(weights);
 
     pageBook.showPage(pageBook2);
   }
 
   protected void createElementContentWidget(Composite parent)
   {
     extensionDetailsViewer = new ExtensionDetailsViewer(parent, getWidgetFactory());
     extensionDetailsViewer.setEditManager(new DOMExtensionItemEditManager());
     extensionDetailsViewer.setContentProvider(new DOMExtensionDetailsContentProvider());    
     extensionDetailsViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
   }
 
   /*
    * @see org.eclipse.wst.common.ui.properties.internal.provisional.view.ITabbedPropertySection#refresh()
    */
   public void refresh()
   {
     setListenerEnabled(false);
     if (input != null)
     {
      Tree tree = extensionTreeViewer.getTree();
       extensionDetailsViewer.setInput(null);
      tree.removeAll();
       
       extensionTreeViewer.setInput(input);
 
      if (tree.getSelectionCount() == 0 && tree.getItemCount() > 0)
      {       
        TreeItem treeItem = tree.getItem(0);
         if (treeItem != null)
         {  
           extensionDetailsViewer.setInput(treeItem.getData());
           extensionDetailsViewer.refresh();
         }  
       }
     }
     setListenerEnabled(true);
 
   }
 
   public Composite getPage()
   {
     return page;
   }
 
   protected abstract AddExtensionCommand getAddExtensionCommand(Object o);
   protected abstract Command getRemoveExtensionCommand(Object o);  
   protected abstract ExtensionsSchemasRegistry getExtensionsSchemasRegistry();
   
   protected AddExtensionsComponentDialog createAddExtensionsComponentDialog()
   {
     return new AddExtensionsComponentDialog(composite.getShell(), getExtensionsSchemasRegistry());
   }
     
   public void widgetSelected(SelectionEvent event)
   {
     if (event.widget == addButton)
     {
       ExtensionsSchemasRegistry registry = getExtensionsSchemasRegistry();
       AddExtensionsComponentDialog dialog = createAddExtensionsComponentDialog();
 
       List properties = registry.getAllExtensionsSchemasContribution();
 
       dialog.setInput(properties);
       dialog.setBlockOnOpen(true);
 
       if (dialog.open() == Window.OK)
       {
         Object newSelection = null;          
         Object[] result = dialog.getResult();      
         if (result != null)
         {
           SpecificationForExtensionsSchema extensionsSchemaSpec = (SpecificationForExtensionsSchema) result[1];
           AddExtensionCommand addExtensionCommand = getAddExtensionCommand(result[0]);
           if (addExtensionCommand != null)
           {  
             addExtensionCommand.setSchemaProperties(extensionsSchemaSpec);
             if (getCommandStack() != null)
             {
               getCommandStack().execute(addExtensionCommand);
               newSelection = addExtensionCommand.getNewObject();
             }
           }
         }  
         extensionTreeViewer.refresh();
         refresh();
         if (newSelection != null)
         {  
           extensionTreeViewer.setSelection(new StructuredSelection(newSelection));
         }  
       }
 
     }
     else if (event.widget == removeButton)
     {
       ISelection selection = extensionTreeViewer.getSelection();
       
       if (selection instanceof StructuredSelection)
       {
         Object o = ((StructuredSelection) selection).getFirstElement();
         Command command = getRemoveExtensionCommand(o);            
         if (getCommandStack() != null)
         {
           getCommandStack().execute(command);
           extensionTreeViewer.setInput(input);
           extensionTreeViewer.refresh();
 
           if (extensionTreeViewer.getTree().getItemCount() > 0)
           {/*
                 // TODO (cs) I think this code is intended to set a selection
                 // now that an object can been removed ... need to fix this                
                 Object object = extensionTreeViewer.get
                 if (object != null)
                 {
                     extensionTreeViewer.setSelection(new StructuredSelection(object));
                 }
            */
           }
           else
           {
             extensionDetailsViewer.setInput(null);
           }
         }
       }
     }
     else if (event.widget == extensionTreeViewer.getTree())
     {
 
     }
   }
 
   public void widgetDefaultSelected(SelectionEvent event)
   {
 
   }
 
   public boolean shouldUseExtraSpace()
   {
     return true;
   }
 
   public void dispose()
   {
     documentChangeNotifier.removeListener(internalNodeAdapter);
   }
  
 
   Element selectedElement;
 
   class ElementSelectionChangedListener implements ISelectionChangedListener
   {
     public void selectionChanged(SelectionChangedEvent event)
     {
       ISelection selection = event.getSelection();
       if (selection instanceof StructuredSelection)
       {
         Object obj = ((StructuredSelection) selection).getFirstElement();
         if (obj instanceof Element)
         {
           selectedElement = (Element) obj;
           extensionDetailsViewer.setInput(obj);
           //extensionDetailsViewer.setASIElement(selectedElement);
           //extensionDetailsViewer.setCommandStack(getCommandStack());          
         }
       }
     }
   }
 
   public ITreeContentProvider getExtensionTreeContentProvider()
   {
     return extensionTreeContentProvider;
   }
 
   public void setExtensionTreeContentProvider(ITreeContentProvider extensionTreeContentProvider)
   {
     this.extensionTreeContentProvider = extensionTreeContentProvider;
   }
 
   public ILabelProvider getExtensionTreeLabelProvider()
   {
     return extensionTreeLabelProvider;
   }
 
   public void setExtensionTreeLabelProvider(ILabelProvider extensionTreeLabelProvider)
   {
     this.extensionTreeLabelProvider = extensionTreeLabelProvider;
   }
 }
