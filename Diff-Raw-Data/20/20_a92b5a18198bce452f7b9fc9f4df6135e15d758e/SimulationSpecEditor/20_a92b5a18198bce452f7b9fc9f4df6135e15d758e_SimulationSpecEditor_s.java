 package dk.itu.big_red.editors.simulation_spec;
 
 import java.beans.PropertyChangeEvent;
 import java.beans.PropertyChangeListener;
 import java.io.OutputStream;
 import java.util.ArrayDeque;
 import java.util.ArrayList;
 import java.util.Iterator;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.draw2d.ColorConstants;
 import org.eclipse.gef.ui.actions.ActionRegistry;
 import org.eclipse.jface.dialogs.Dialog;
 import org.eclipse.jface.viewers.ComboViewer;
 import org.eclipse.jface.viewers.IStructuredSelection;
 import org.eclipse.jface.viewers.LabelProvider;
 import org.eclipse.jface.viewers.ListViewer;
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.layout.RowLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.ui.IEditorInput;
 import org.eclipse.ui.IEditorSite;
 import org.eclipse.ui.ISharedImages;
 import org.eclipse.ui.PartInitException;
 import org.eclipse.ui.part.FileEditorInput;
 
 import dk.itu.big_red.application.plugin.RedPlugin;
 import dk.itu.big_red.editors.AbstractEditor;
 import dk.itu.big_red.editors.assistants.RedoProxyAction;
 import dk.itu.big_red.editors.assistants.UndoProxyAction;
 import dk.itu.big_red.editors.assistants.RedoProxyAction.IRedoImplementor;
 import dk.itu.big_red.editors.assistants.UndoProxyAction.IUndoImplementor;
 import dk.itu.big_red.import_export.Export;
 import dk.itu.big_red.import_export.ExportFailedException;
 import dk.itu.big_red.import_export.Import;
 import dk.itu.big_red.import_export.ImportFailedException;
 import dk.itu.big_red.model.Bigraph;
 import dk.itu.big_red.model.ReactionRule;
 import dk.itu.big_red.model.Signature;
 import dk.itu.big_red.model.SimulationSpec;
 import dk.itu.big_red.model.changes.Change;
 import dk.itu.big_red.model.changes.ChangeRejectedException;
 import dk.itu.big_red.model.import_export.SimulationSpecXMLExport;
 import dk.itu.big_red.tools.BasicCommandLineInteractionManager;
 import dk.itu.big_red.tools.ConfigurationElementInteractionManagerFactory;
 import dk.itu.big_red.tools.IInteractionManager;
 import dk.itu.big_red.tools.IInteractionManagerFactory;
 import dk.itu.big_red.utilities.ValidationFailedException;
 import dk.itu.big_red.utilities.resources.IFileBackable;
 import dk.itu.big_red.utilities.resources.ResourceTreeSelectionDialog;
 import dk.itu.big_red.utilities.resources.ResourceTreeSelectionDialog.Mode;
 import dk.itu.big_red.utilities.resources.Types;
 import dk.itu.big_red.utilities.ui.ResourceSelector;
 import dk.itu.big_red.utilities.ui.ResourceSelector.ResourceListener;
 import dk.itu.big_red.utilities.ui.jface.ListContentProvider;
 import dk.itu.big_red.utilities.ui.UI;
 
 public class SimulationSpecEditor extends AbstractEditor
 implements IUndoImplementor, IRedoImplementor, PropertyChangeListener {
 	private static class SimpleExportInteractionManagerFactory
 		extends ConfigurationElementInteractionManagerFactory {
 		
 		public SimpleExportInteractionManagerFactory(IConfigurationElement ice) {
 			super(ice);
 		}
 		
 		@SuppressWarnings("unchecked")
 		@Override
 		public IInteractionManager createInteractionManager() {
 			return new BasicCommandLineInteractionManager(
 					(Export<SimulationSpec>)RedPlugin.instantiate(getCE()));
 		}
 	}
 	
 	@Override
 	public void doActualSave(OutputStream os) throws ExportFailedException {
     	new SimulationSpecXMLExport().setModel(getModel()).setOutputStream(os).
     		exportObject();
     	savePoint = undoBuffer.peek();
 		checkDirt();
 	}
 
 	@Override
 	public void init(IEditorSite site, IEditorInput input)
 			throws PartInitException {
 		setSite(site);
 		setInputWithNotify(input);
 	}
 
 	private Change savePoint = null;
 	private ArrayDeque<Change>
 			undoBuffer = new ArrayDeque<Change>(),
 			redoBuffer = new ArrayDeque<Change>();
 	
 	@Override
 	public boolean canUndo() {
 		return (undoBuffer.size() != 0);
 	}
 	
 	@Override
 	public boolean canRedo() {
 		return (redoBuffer.size() != 0);
 	}
 	
 	private void doChange(Change c) {
 		try {
 			model.tryApplyChange(c);
 			redoBuffer.clear();
 			undoBuffer.push(c);
 		} catch (ChangeRejectedException cre) {
 			cre.killVM();
 		}
 		checkDirt();
 		updateActions(stackActions);
 	}
 	
 	@Override
 	public void undo() {
 		try {
 			if (!canUndo())
 				return;
 			Change c;
 			redoBuffer.push(c = undoBuffer.pop());
 			model.tryApplyChange(c.inverse());
 		} catch (ChangeRejectedException cre) {
 			/* should never happen */
 			cre.killVM();
 		}
 		checkDirt();
 		updateActions(stackActions);
 	}
 	
 	@Override
 	public void redo() {
 		try {
 			if (!canRedo())
 				return;
 			Change c;
 			model.tryApplyChange(c = redoBuffer.pop());
 			undoBuffer.push(c);
 		} catch (ChangeRejectedException cre) {
 			/* should never happen */
 			cre.killVM();
 		}
 		checkDirt();
 		updateActions(stackActions);
 	}
 	
 	private void checkDirt() {
 		boolean newDirty = (undoBuffer.peek() != savePoint);
 		if (newDirty != dirty) {
 			dirty = newDirty;
 			firePropertyChange(PROP_DIRTY);
 		}
 	}
 	
 	private SimulationSpec model = null;
 	
 	@Override
 	protected SimulationSpec getModel() {
 		return model;
 	}
 	
 	private boolean uiUpdateInProgress = false;
 	
 	@Override
 	protected void initialiseActual() throws Throwable {
 		IEditorInput input = getEditorInput();
 		if (input instanceof FileEditorInput) {
 			FileEditorInput fi = (FileEditorInput)input;
 			try {
 				model = (SimulationSpec)Import.fromFile(fi.getFile());
 			} catch (ImportFailedException e) {
 	    		e.printStackTrace();
 	    		Throwable cause = e.getCause();
 	    		if (cause instanceof ValidationFailedException) {
 	    			throw cause;
 	    		} else throw e;
 	    	}
 		}
 		if (model == null)
 			model = new SimulationSpec();
 		
 		rules.setInput(model);
 		model.addPropertyChangeListener(this);
 		modelToControls();
 	}
 	
 	private void modelToControls() {
 		uiUpdateInProgress = true;
 		
 		Signature s = model.getSignature();
 		if (s != null)
 			signatureSelector.setResource(s.getFile());
 		
 		Bigraph b = model.getModel();
 		if (b != null)
 			modelSelector.setResource(b.getFile());
 		
 		uiUpdateInProgress = false;
 	}
 	
 	private boolean dirty = false;
 	
 	@Override
 	public boolean isDirty() {
 		return dirty;
 	}
 	
 	private static ArrayList<IInteractionManagerFactory> getIMFactories() {
 		ArrayList<IInteractionManagerFactory> factories =
 				new ArrayList<IInteractionManagerFactory>();
 		
 		for (IConfigurationElement ce :
 			 RedPlugin.getConfigurationElementsFor("dk.itu.big_red.externalTools"))
 			factories.add(new ConfigurationElementInteractionManagerFactory(ce));
 		
 		for (IConfigurationElement ce :
 		     RedPlugin.getConfigurationElementsFor(Export.EXTENSION_POINT)) {
 			String exports = ce.getAttribute("exports");
 			if (exports.equals(SimulationSpec.class.getCanonicalName()))
 				factories.add(new SimpleExportInteractionManagerFactory(ce));
 		}
 		return factories;
 	}
 	
 	private ResourceSelector signatureSelector, modelSelector;
 	private ListViewer rules;
 	private Button export;
 	
 	private void recalculateExportEnabled() {
 		export.setEnabled(
 			signatureSelector.getResource() != null &&
 			modelSelector.getResource() != null);
 	}
 	
 	@Override
 	public void createPartControl(Composite parent) {
 		Composite self = new Composite(parent, SWT.NONE);
 		self.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
 		setComposite(self);
 		
		GridLayout gl = new GridLayout(4, false);
 		gl.marginTop = gl.marginLeft = gl.marginBottom = gl.marginRight = 
 			gl.horizontalSpacing = gl.verticalSpacing = 10;
 		self.setLayout(gl);
 		
 		UI.newLabel(self, SWT.RIGHT, "Signature:").setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
 		signatureSelector = new ResourceSelector(self,
 			((FileEditorInput)getEditorInput()).getFile().getProject(),
 			Mode.FILE, Types.SIGNATURE_XML);
 		signatureSelector.getButton().setLayoutData(
			new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
 		signatureSelector.addListener(new ResourceListener() {
 			@Override
 			public void resourceChanged(IResource oldValue, IResource newValue) {
 				recalculateExportEnabled();
 				try {
 					if (uiUpdateInProgress)
 						return;
 					Signature s = null;
 					if (newValue != null)
 						s = (Signature)Import.fromFile((IFile)newValue);
 					doChange(getModel().changeSignature(s));
 				} catch (ImportFailedException ife) {
 					ife.printStackTrace();
 				}
 			}
 		});
 		
 		UI.newLabel(self, SWT.RIGHT, "Reaction rules:").setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
 		rules = new ListViewer(self);
 		UI.setProviders(rules, new SimulationSpecRRContentProvider(rules),
 			new LabelProvider() {
 				@Override
 				public String getText(Object element) {
 					return ((IFileBackable)element).getFile().
 							getProjectRelativePath().toString();
 				}
 		});
 		rules.getList().setLayoutData(
 				new GridData(SWT.FILL, SWT.FILL, true, true));
 		
 		Composite br = new Composite(self, SWT.NONE);
		br.setBackground(ColorConstants.red);
		br.setLayoutData(new GridData(SWT.END, SWT.BOTTOM, false, false, 2, 1));
 		RowLayout brl = new RowLayout(SWT.VERTICAL);
 		brl.marginBottom = brl.marginLeft = brl.marginRight =
 				brl.marginTop = 0;
 		brl.pack = false;
 		br.setLayout(brl);
 		
 		Button b = UI.newButton(br, SWT.NONE, "&Add...");
 		b.setImage(UI.getImage(ISharedImages.IMG_OBJ_ADD));
 		b.addSelectionListener(new SelectionAdapter() {
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				ResourceTreeSelectionDialog rtsd =
 					new ResourceTreeSelectionDialog(
 						getSite().getShell(),
 						((FileEditorInput)getEditorInput()).getFile().getProject(),
 						Mode.FILE, Types.RULE_XML);
 				rtsd.setBlockOnOpen(true);
 				if (rtsd.open() == Dialog.OK) {
 					IFile f = (IFile)rtsd.getFirstResult();
 					try {
 						ReactionRule r = (ReactionRule)Import.fromFile(f);
 						doChange(model.changeAddRule(r));
 					} catch (ImportFailedException ife) {
 						ife.printStackTrace();
 					}
 				}
 			}
 		});
 		
 		b = UI.newButton(br, SWT.NONE, "&Remove...");
 		b.setImage(UI.getImage(ISharedImages.IMG_ELCL_REMOVE));
 		b.addSelectionListener(new SelectionAdapter() {
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				Iterator<?> it =
 					((IStructuredSelection)rules.getSelection()).iterator();
 				while (it.hasNext())
 					doChange(getModel().
 							changeRemoveRule((ReactionRule)it.next()));
 			}
 		});
 		
 		UI.newLabel(self, SWT.RIGHT, "Model:").setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
 		modelSelector = new ResourceSelector(self,
 			((FileEditorInput)getEditorInput()).getFile().getProject(),
 			Mode.FILE, Types.BIGRAPH_XML);
 		modelSelector.getButton().setLayoutData(
			new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
 		modelSelector.addListener(new ResourceListener() {
 			@Override
 			public void resourceChanged(IResource oldValue, IResource newValue) {
 				recalculateExportEnabled();
 				try {
 					if (uiUpdateInProgress)
 						return;
 					Bigraph b = null;
 					if (newValue != null)
 						b = (Bigraph)Import.fromFile((IFile)newValue);
 					doChange(getModel().changeModel(b));
 				} catch (ImportFailedException ife) {
 					ife.printStackTrace();
 				}
 			}
 		});
 		
		new Label(self, SWT.HORIZONTAL | SWT.SEPARATOR).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
 		
 		UI.newLabel(self, SWT.RIGHT, "Tool:").setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
 		final ComboViewer cv = UI.setProviders(new ComboViewer(self),
 			new ListContentProvider(), new LabelProvider() {
 				@Override
 				public String getText(Object element) {
 					return ((IInteractionManagerFactory)element).getName();
 				}
 			});
		cv.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
 		ArrayList<IInteractionManagerFactory> exporters = getIMFactories();
 		cv.setInput(exporters);
 		cv.setSelection(new StructuredSelection(exporters.get(0)));
 		
 		export = UI.newButton(self, SWT.NONE, "&Export...");
 		export.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
 		export.addSelectionListener(new SelectionAdapter() {
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				IInteractionManager im =
 					((IInteractionManagerFactory)
 						((IStructuredSelection)cv.getSelection()).
 							getFirstElement()).createInteractionManager();
 				im.setSimulationSpec(getModel());
 				im.run();
 			}
 		});
 		export.setEnabled(false);
 		
 		initialise();
 	}
 
 	private ArrayList<String> stackActions = new ArrayList<String>();
 	
 	@Override
 	protected void createActions() {
 		ActionRegistry registry = getActionRegistry();
 		
 		registerActions(registry, stackActions,
 				new UndoProxyAction(this), new RedoProxyAction(this));
 	}
 	
 	@Override
 	protected void initializeActionRegistry() {
 		super.initializeActionRegistry();
 		updateActions(stackActions);
 	}
 	
 	@Override
 	public void setFocus() {
 		// TODO Auto-generated method stub
 		UI.getWorkbenchPage().activate(this);
 	}
 	
 	@Override
 	public void propertyChange(PropertyChangeEvent evt) {
 		if (evt.getSource() != getModel() || uiUpdateInProgress)
 			return;
 		uiUpdateInProgress = true;
 		try {
 			String propertyName = evt.getPropertyName();
 			Object newValue = evt.getNewValue();
 			uiUpdateInProgress = true;
 			if (propertyName.equals(SimulationSpec.PROPERTY_SIGNATURE)) {
 				Signature s = (Signature)newValue;
 				signatureSelector.setResource((s != null ? s.getFile() : null));
 			} else if (propertyName.equals(SimulationSpec.PROPERTY_MODEL)) {
 				Bigraph b = (Bigraph)newValue;
 				modelSelector.setResource((b != null ? b.getFile() : null));
 			}
 		} finally {
 			uiUpdateInProgress = false;
 		}
 	}
 }
