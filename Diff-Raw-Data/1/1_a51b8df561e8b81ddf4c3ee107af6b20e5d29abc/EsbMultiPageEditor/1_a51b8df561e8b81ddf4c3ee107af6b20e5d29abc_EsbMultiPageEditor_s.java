 /*******************************************************************************
  * Copyright (c) 2000, 2006 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.wso2.developerstudio.eclipse.gmf.esb.diagram.part;
 
 
 import java.io.ByteArrayInputStream;
 import java.io.File;
 import java.io.InputStream;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 import java.util.UUID;
 
 import org.eclipse.core.internal.resources.ModelObject;
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IFolder;
 import org.eclipse.core.resources.IMarker;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.core.runtime.Path;
 import org.eclipse.emf.common.util.EList;
 import org.eclipse.emf.common.util.URI;
 import org.eclipse.emf.ecore.EObject;
 import org.eclipse.emf.ecore.resource.Resource;
 import org.eclipse.emf.ecore.resource.ResourceSet;
 import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
 import org.eclipse.gef.EditPart;
 import org.eclipse.gef.commands.CompoundCommand;
 import org.eclipse.gmf.runtime.common.core.command.ICommand;
 import org.eclipse.gmf.runtime.diagram.ui.commands.DeferredCreateConnectionViewAndElementCommand;
 import org.eclipse.gmf.runtime.diagram.ui.commands.ICommandProxy;
 import org.eclipse.gmf.runtime.diagram.ui.requests.CreateConnectionViewAndElementRequest;
 import org.eclipse.gmf.runtime.emf.core.util.EObjectAdapter;
 import org.eclipse.gmf.runtime.emf.type.core.IHintedType;
 import org.eclipse.gmf.runtime.notation.impl.NodeImpl;
 import org.eclipse.jface.dialogs.ErrorDialog;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.ui.IEditorInput;
 import org.eclipse.ui.IEditorPart;
 import org.eclipse.ui.IEditorReference;
 import org.eclipse.ui.IEditorSite;
 import org.eclipse.ui.IFileEditorInput;
 import org.eclipse.ui.IWorkbench;
 import org.eclipse.ui.IWorkbenchWindow;
 import org.eclipse.ui.PartInitException;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.WorkbenchException;
 import org.eclipse.ui.ide.FileStoreEditorInput;
 import org.eclipse.ui.ide.IDE;
 import org.eclipse.ui.ide.IGotoMarker;
 import org.eclipse.ui.part.FileEditorInput;
 import org.eclipse.ui.part.MultiPageEditorPart;
 import org.wso2.developerstudio.eclipse.gmf.esb.EsbDiagram;
 import org.wso2.developerstudio.eclipse.gmf.esb.EsbElement;
 import org.wso2.developerstudio.eclipse.gmf.esb.EsbLink;
 import org.wso2.developerstudio.eclipse.gmf.esb.EsbServer;
 import org.wso2.developerstudio.eclipse.gmf.esb.Sequence;
 import org.wso2.developerstudio.eclipse.gmf.esb.Sequences;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.Activator;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.edit.parts.EsbServerContentsCompartmentEditPart;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.edit.parts.EsbServerEditPart;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.edit.parts.LogMediatorEditPart;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.edit.parts.LogMediatorInputConnectorEditPart;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.edit.parts.MediatorFlowEditPart;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.edit.parts.MediatorFlowMediatorFlowCompartmentEditPart;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.edit.parts.ProxyOutputConnectorEditPart;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.edit.parts.ProxyServiceContainerEditPart;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.edit.parts.ProxyServiceEditPart;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.edit.parts.ProxyServiceSequenceAndEndpointContainerEditPart;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.edit.parts.SequenceEditPart;
 import org.wso2.developerstudio.eclipse.gmf.esb.diagram.providers.EsbElementTypes;
 import org.wso2.developerstudio.eclipse.gmf.esb.persistence.EsbModelTransformer;
 import org.wso2.developerstudio.eclipse.gmf.esb.persistence.SequenceInfo;
 import org.wso2.developerstudio.eclipse.logging.core.IDeveloperStudioLog;
 import org.wso2.developerstudio.eclipse.logging.core.Logger;
 
 import static org.wso2.developerstudio.eclipse.gmf.esb.diagram.custom.EditorUtils.*;
 
 /**
  * The main editor class which contains design view and source view
  * <ul>
  * <li>page 0 graphical view
  * <li>page 1 source view
  * </ul>
  */
 public class EsbMultiPageEditor extends MultiPageEditorPart implements
         IGotoMarker {
 
     /** Our all new graphical editor */
     private EsbDiagramEditor graphicalEditor;
     
 	/**
 	 * {@link ModelObject} source editor.
 	 */
 	private EsbObjectSourceEditor sourceEditor;
     
 	/**
 	 * Name of the directory which holds temporary files.
 	 */
 	public static final String TEMPORARY_RESOURCES_DIRECTORY = ".org.wso2.developerstudio.eclipse.esb";
 
 	/**
 	 * Design view page index.
 	 */
 	private static final int DESIGN_VIEW_PAGE_INDEX = 0;
 
 	/**
 	 * Source view page index.
 	 */
 	private static final int SOURCE_VIEW_PAGE_INDEX = 1;
 	
 	/**
 	 * Properties view page index.
 	 */
 	private static final int PROPERTY_VIEW_PAGE_INDEX = 2;
 	
 	/**
 	 * Used to hold temporary files.
 	 */
 	private Set<IFile> tempFiles = new HashSet<IFile>();
 	
 	private static IDeveloperStudioLog log = Logger.getLog(Activator.PLUGIN_ID);
 
     /**
      * Creates a multi-page editor
      */
     public EsbMultiPageEditor() {
         super();
         IWorkbench workbench = PlatformUI.getWorkbench();
         IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
          
         // open the perspective "wso2 esb graphical"
         // TODO review if this is the best place for switching
         try {
 			workbench.showPerspective("org.wso2.developerstudio.eclipse.gmf.esb.diagram.custom.perspective", window);
 		} catch (WorkbenchException e) {
 		}
 
     }
  
 
     /**
      * Creates page 0 of the multi-page editor,
      * which contains a out graphical diagram
      */
     void createPage0() {
         try {
             graphicalEditor = new EsbDiagramEditor(this);
             addPage(DESIGN_VIEW_PAGE_INDEX, graphicalEditor, getEditorInput());
             setPageText(DESIGN_VIEW_PAGE_INDEX, "Design"); //$NON-NLS-1$
         } catch (PartInitException e) {
             ErrorDialog
                     .openError(
                             getSite().getShell(),
                             "ErrorCreatingNestedEditor", null, e.getStatus());
         }
         
         EsbPaletteFactory esbPaletteFactory=new EsbPaletteFactory();
         esbPaletteFactory.addDefinedSequences(getEditor(0));
         esbPaletteFactory.addDefinedEndpoints(getEditor(0));
         esbPaletteFactory.updateToolPaletteItems(graphicalEditor);
     }
 
     /**
      * Creates page 1 of the multi-page editor,
      * which allows you to edit the xml.
      */
     void createPage1() {   
     /*	
     	try{
     	TextEditor sourceEditor1 = new TextEditor();
 		addPage(sourceEditor1,getEditorInput());
 		setPageText(SOURCE_VIEW_PAGE_INDEX,	"Source");
     	}
     	catch (Exception ex) {
     		System.out.println("Error while creating source view");
     	}*/
     	
     	
 		try {
 			sourceEditor = new EsbObjectSourceEditor(
 					getTemporaryFile("xml"));
 			addPage(SOURCE_VIEW_PAGE_INDEX,
 					sourceEditor.getEditor(),
 					sourceEditor.getInput());
 			setPageText(SOURCE_VIEW_PAGE_INDEX,	"Source");
 
 			// Initialize source editor.
 			//updateSourceEditor();
 		} catch (Exception ex) {
 			//TODO: Get rid of this
 			ex.printStackTrace();
 			//log.error(
 			//		"Error while initializing source viewer control.",
 			//		ex);
 		}	
 		
     }
     
   /*  private void createModel(IEditorInput editorInput) throws PartInitException{
 		URI resourceURI = EditUIUtil.getURI(getEditorInput());
 		Resource resource = null;
 		
 		EsbResourceFactoryImpl fac = new EsbResourceFactoryImpl();
 		resource = fac.createResource(resourceURI);
 		
 		graphicalEditor = new EsbDiagramEditor();
 		graphicalEditor.setDocumentProvider(editorInput);
 		TransactionalEditingDomain  t=graphicalEditor.getEditingDomain();
 		t.getResourceSet().getResources().add(resource);
 		try {
 			// Load the resource through the editing domain.
 			//
 			resource = graphicalEditor.getEditingDomain().getResourceSet().getResource(resourceURI, true);
 		}
 		catch (Exception e) {
 			resource = graphicalEditor.getEditingDomain().getResourceSet().getResource(resourceURI, false);
 		}
     }*/
 
     /**
      * Creates page 2 of the multi-page editor,
      * which shows the sorted text.
      */
     void createPage2() {
         Composite composite = new Composite(getContainer(), SWT.NONE);
         GridLayout layout = new GridLayout();
         composite.setLayout(layout);
         layout.numColumns = 2;
 
         //Label demoLabel = new Label(composite, SWT.NONE);
         GridData gd = new GridData(GridData.BEGINNING);
         gd.horizontalSpan = 2;
         //demoLabel.setLayoutData(gd);
         //demoLabel.setText("ChangeFont");
 
         int index = addPage(composite);
         setPageText(index, "Properties");
     }
     
 	/**
 	 * Utility method for obtaining a reference to a temporary file with the
 	 * given extension.
 	 *
 	 * @param extension
 	 *            extension of the temporary file.
 	 * @return {@link IFile} instance corresponding to the spefied temporary
 	 *         file.
 	 * @throws Exception
 	 *             if a temporary file cannot be created.
 	 */
 	private IFile getTemporaryFile(String extension) throws Exception {
 		String fileName = String.format("%s.%s", UUID.randomUUID().toString(),
 				extension);
 		IFile tempFile = getTemporaryDirectory().getFile(fileName);
 		if (!tempFile.exists()) {
 			tempFile.create(new ByteArrayInputStream(new byte[0]), true, null);
 		}
 		tempFiles.add(tempFile);
 		return tempFile;
 	}
 
 	/**
 	 * Utility method for obtaining a reference to the temporary files
 	 * directory.
 	 *
 	 * @return reference to the temporary files directory inside the current
 	 *         project.
 	 * @throws Exception
 	 *             if an error occurs while creating the temporary resources
 	 *             directory.
 	 */
 	private IFolder getTemporaryDirectory() throws Exception {
 		IEditorInput editorInput = getEditorInput();
 		if (editorInput instanceof IFileEditorInput || editorInput instanceof FileStoreEditorInput) {
 			
 			IProject tempProject = ResourcesPlugin.getWorkspace().getRoot().getProject(".tmp");
 			
 			if (!tempProject.exists()){
 				tempProject.create(new NullProgressMonitor());
 			}
 			
 			if (!tempProject.isOpen()){
 				tempProject.open(new NullProgressMonitor());
 			}
 			
 			if (!tempProject.isHidden()) {
 				tempProject.setHidden(true);
 			}
 
 			IFolder folder = tempProject.getFolder(TEMPORARY_RESOURCES_DIRECTORY);
 			
 			if (!folder.exists()) {
 				folder.create(true, true, new NullProgressMonitor());
 			}
 			
 			return folder;
 		} else {
 			throw new Exception(
 					"Unable to create temporary resources directory.");
 		}
 	}
 
     /**
      * Creates the pages of the multi-page editor.
      */
     protected void createPages() {
         createPage0();
         createPage1();
         //createPage2();
     }
     
 	/**
 	 * This is used to track the active viewer. <!-- begin-user-doc --> <!--
 	 * end-user-doc -->
 	 */
 	
 	protected void pageChange(int pageIndex) {
 		super.pageChange(pageIndex);
 
 		// I do not understand why this is necessary (emf generated code).
 		// if (contentOutlinePage != null) {
 		// handleContentOutlineSelection(contentOutlinePage.getSelection());
 		// }
 
 		// Invoke the appropriate handler method.
 		switch (pageIndex) {
 		case DESIGN_VIEW_PAGE_INDEX: {
 			handleDesignViewActivatedEvent(); 
 			break;
 		}
 		case SOURCE_VIEW_PAGE_INDEX: {
 			updateSequenceDetails();
 			handleSourceViewActivatedEvent();
 			break;
 
 		}
 		}
 	}
 	
 	/**
 	 * Performs necessary house-keeping tasks whenever the design view is
 	 * activated.
 	 */
 	private void handleDesignViewActivatedEvent() {
 
 //		if (null != sourceEditor.getObject()) {
 //			rebuildModelObject(objectSourceEditor.getObject());
 //		}
 		
 		String xmlSource = sourceEditor.getDocument().get();
 		if(xmlSource!=null && xmlSource.equals(xmlSource)){
 			  rebuildModelObject(xmlSource);
 		} 
 		
 	}
 
 	/**
 	 * Performs necessary house-keeping tasks whenever the source view is
 	 * activated.
 	 */
 	private void handleSourceViewActivatedEvent() {
 //		if (null == contentOutlinePage) {
 //			// Need to sync the soure editor explicitly.
 		
 			updateSourceEditor();
 //		}
 	}
 
     private void updateSourceEditor() {
     	EsbDiagram diagram = (EsbDiagram) graphicalEditor.getDiagram().getElement();
 		EsbServer server = diagram.getServer();	
 		sourceEditor.update(server);		
 	}
     
     
 	private void updateAssociatedXMLFile(IProgressMonitor monitor) {
 		EsbDiagram diagram = (EsbDiagram) graphicalEditor.getDiagram().getElement();
 		EsbServer server = diagram.getServer();
 		IEditorInput editorInput = getEditorInput();
 		
 		if (editorInput instanceof IFileEditorInput) {
 			IFile diagramFile = ((FileEditorInput) editorInput).getFile();
 			String xmlFilePath = diagramFile.getFullPath().toString();
 			xmlFilePath = xmlFilePath
 					.replaceFirst("/graphical-synapse-config/", "/synapse-config/")
 					.replaceFirst("/endpoints/endpoint_", "/endpoints/")
 					.replaceFirst("/local-entries/localentry_", "/local-entries/")
 					.replaceFirst("/proxy-services/proxy_", "/proxy-services/")
 					.replaceFirst("/sequences/sequence_", "/sequences/")
 					.replaceAll(".esb_diagram$", ".xml");
 			IFile xmlFile = diagramFile.getWorkspace().getRoot().getFile(new Path(xmlFilePath));
 			try {
 				String source = EsbModelTransformer.instance.designToSource(server);
 				if (source == null) {
 					log.warn("Could get source");
 					return;
 				}
 				InputStream is = new ByteArrayInputStream(source.getBytes());
 				if (xmlFile.exists()) {
 					xmlFile.setContents(is, true, true, monitor);
 				} else {
 					xmlFile.create(is, true, monitor);
 				}
 
 			} catch (Exception e) {
 				log.warn("Could not save file " + xmlFile);
 			}
 		}
 	}
     
 
 	/**
      * Saves the multi-page editor's document.
      */
     public void doSave(IProgressMonitor monitor) {
         getEditor(0).doSave(monitor);
         updateAssociatedXMLFile(monitor);
     }
     
 	/**
 	 * Source view is currently read-only, so for now we only handling design
 	 * view's dirty property
 	 */
 	public boolean isDirty() {
 		if (getEditor(0) instanceof EsbDiagramEditor) {
 			return getEditor(0).isDirty();
 		}
 		return super.isDirty();
 	}
 
     /**
      * Saves the multi-page editor's document as another file.
      * Also updates the text for page 0's tab, and updates this multi-page editor's input
      * to correspond to the nested editor's.
      */
     public void doSaveAs() {
         IEditorPart editor = getEditor(0);
         editor.doSaveAs();
         setPageText(0, editor.getTitle());
         setInput(editor.getEditorInput());
     }
 
     /**
      * The <code>MultiPageEditorExample</code> implementation of this method
      * checks that the input is an instance of <code>IFileEditorInput</code>.
      */
     public void init(IEditorSite site, IEditorInput editorInput)
             throws PartInitException {    	
     	
     	/*setSite(site);
 		setInputWithNotify(editorInput);
 		setPartName(editorInput.getName());*/
     	
         if (!(editorInput instanceof IFileEditorInput))
             throw new PartInitException("InvalidInput"); //$NON-NLS-1$     
         
        // createModel(editorInput);
        super.init(site, editorInput);
        setTitle(editorInput.getName());
        
     }    
     
 
     /* (non-Javadoc)
      * Method declared on IEditorPart.
      */
     public boolean isSaveAsAllowed() {
         return true;
     }
 
 
     /* (non-Javadoc)
      * @see org.eclipse.ui.ide.IGotoMarker
      */
     public void gotoMarker(IMarker marker) {
         setActivePage(DESIGN_VIEW_PAGE_INDEX);
         IDE.gotoMarker(graphicalEditor, marker);
     }
     
     
     private void updateSequenceDetails(){
 
 		IEditorPart editorPart = null;
 		IProject activeProject = null;
 		IEditorReference editorReferences[] = PlatformUI.getWorkbench()
 				.getActiveWorkbenchWindow().getActivePage()
 				.getEditorReferences();
 		for (int i = 0; i < editorReferences.length; i++) {
 			IEditorPart editor = editorReferences[i].getEditor(false);
 
 			if (editor != null) {
 				editorPart = editor.getSite().getWorkbenchWindow()
 						.getActivePage().getActiveEditor();
 			}
 
 			if (editorPart != null) {
 				IFileEditorInput input = (IFileEditorInput) editorPart
 						.getEditorInput();
 				IFile file = input.getFile();
 				activeProject = file.getProject();
 
 			}
 
 		}
 		
 		List<Sequence> childNodes = new ArrayList<Sequence>();		
 		
 		for (int i = 0; i < graphicalEditor.getDiagramEditPart().getViewer()
 				.getEditPartRegistry().size(); ++i) {
 
 			EditPart element = (EditPart) graphicalEditor.getDiagramEditPart()
 					.getViewer().getEditPartRegistry().values().toArray()[i];
 			if (element instanceof SequenceEditPart) {
 				if (((NodeImpl) ((SequenceEditPart) element).getModel())
 						.getElement() instanceof Sequence) {
 					childNodes
 							.add((Sequence) ((NodeImpl) ((SequenceEditPart) element)
 									.getModel()).getElement());
 				}
 			}
 		}		
 		
 		for (Sequence childNode : childNodes) {
 				String name = childNode.getName();
 				IPath location = new Path(SEQUENCE_RESOURCE_DIR + "/" + "sequence_"
 						+ name + ".esb_diagram");
 				IFile file = activeProject.getFile(location);
 
 				ResourceSet resourceSet = new ResourceSetImpl();
 				Resource resource = null;
 
 				File f = new File(file.getLocationURI().getPath());
 				URI uri = URI.createFileURI(f.getAbsolutePath());
 
 				if (!f.exists()) {
 					System.out.println(file.getLocationURI().getPath()
 							+ " does not exist");
 
 				} else {
 
 					resource = resourceSet.getResource(uri, true);
 
 					EsbDiagram s = (EsbDiagram) ((org.eclipse.gmf.runtime.notation.impl.DiagramImpl) resource
 							.getContents().get(0)).getElement();
 					EList<EsbElement> children = s.getServer().getChildren();
 					for (EsbElement esbElement : children) {
 						if (esbElement instanceof Sequences){
 							Sequences sequence = (Sequences) esbElement;
 							EsbLink incomingLink = sequence.getOutputConnector().getOutgoingLink();
 							SequenceInfo.sequenceMap.put(name, incomingLink);
 						}
 					}
 //					if (s.getSequence().getInput().getOutgoingLink() != null) {
 //						EsbLink incomingLink = s.getSequence().getInput()
 //								.getOutgoingLink();
 //						SequenceInfo.sequenceMap.put(name, incomingLink);
 //					}
 				}
 		}
     }
     
     
 	void rebuildModelObject(String xml) {
 		try {
 			EsbDiagram esbDiagram = (EsbDiagram) graphicalEditor.getDiagram()
 					.getElement();
 			EsbServer esbServer = esbDiagram.getServer();
 			EsbServer sourceToDesign = EsbModelTransformer.instance
 					.sourceToDesign(xml, esbServer);
 
 			if (((EditPart) ((EsbServerEditPart) graphicalEditor
 					.getDiagramEditPart().getChildren().get(0)).getChildren()
 					.get(0)).getChildren().size() != 0) {
 
 				if (((EditPart) ((EsbServerEditPart) graphicalEditor
 						.getDiagramEditPart().getChildren().get(0))
 						.getChildren().get(0)).getChildren().get(0) instanceof ProxyServiceEditPart) {
 
 					
 					MediatorFlowMediatorFlowCompartmentEditPart compartmentEditPart=((MediatorFlowMediatorFlowCompartmentEditPart) ((MediatorFlowEditPart) ((ProxyServiceSequenceAndEndpointContainerEditPart) ((ProxyServiceContainerEditPart) ((ProxyServiceEditPart) ((EditPart) ((EsbServerEditPart) graphicalEditor
 							.getDiagramEditPart().getChildren().get(0))
 							.getChildren().get(0)).getChildren().get(0))
 							.getChildren().get(4)).getChildren().get(0))
 							.getChildren().get(0)).getChildren().get(0));
 					
 					if(compartmentEditPart.getChildren().size()!=0){
 					
 					if(compartmentEditPart.getChildren().get(0) instanceof LogMediatorEditPart){
 						LogMediatorEditPart logEditPart = (LogMediatorEditPart)compartmentEditPart
 							.getChildren().get(0);
 					
 
 					LogMediatorInputConnectorEditPart logInputConnectorEditpart = (LogMediatorInputConnectorEditPart) logEditPart
 							.getChildren().get(1);
 
 					CompoundCommand cc = new CompoundCommand(
 							"Create Subtopic and Link");
 
 					ProxyServiceEditPart proxyServiceEditPart = (ProxyServiceEditPart) ((EsbServerContentsCompartmentEditPart) ((EsbServerEditPart) graphicalEditor
 							.getDiagramEditPart().getChildren().get(0))
 							.getChildren().get(0)).getChildren().get(0);
 
 					ICommand createSubTopicsCmd = new DeferredCreateConnectionViewAndElementCommand(
 							new CreateConnectionViewAndElementRequest(
 									EsbElementTypes.EsbLink_4001,
 									((IHintedType) EsbElementTypes.EsbLink_4001)
 											.getSemanticHint(),
 									proxyServiceEditPart
 											.getDiagramPreferencesHint()),
 							new EObjectAdapter(
 									(EObject) ((ProxyOutputConnectorEditPart) proxyServiceEditPart
 											.getChildren().get(1)).getModel()),
 							new EObjectAdapter(
 									(EObject) (logInputConnectorEditpart)
 											.getModel()),
 							proxyServiceEditPart.getViewer());
 
 					cc.add(new ICommandProxy(createSubTopicsCmd));
 
 					proxyServiceEditPart.getDiagramEditDomain()
 							.getDiagramCommandStack().execute(cc);
 				}
 				}
 				}
 			}
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 	}
 }
