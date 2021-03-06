 package dk.itu.big_red.editors.signature;
 
 import java.beans.PropertyChangeListener;
 import java.util.ArrayList;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.draw2d.ColorConstants;
 import org.eclipse.draw2d.geometry.PointList;
 import org.eclipse.jface.preference.ColorSelector;
 import org.eclipse.jface.util.IPropertyChangeListener;
 import org.eclipse.jface.util.PropertyChangeEvent;
 import org.eclipse.jface.viewers.ISelection;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.FocusEvent;
 import org.eclipse.swt.events.FocusListener;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.events.SelectionListener;
 import org.eclipse.swt.graphics.Font;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.layout.RowLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.swt.widgets.Text;
 import org.eclipse.swt.widgets.Tree;
 import org.eclipse.swt.widgets.TreeItem;
 import org.eclipse.ui.IEditorInput;
 import org.eclipse.ui.IEditorSite;
 import org.eclipse.ui.ISelectionListener;
 import org.eclipse.ui.ISharedImages;
 import org.eclipse.ui.IWorkbenchPart;
 import org.eclipse.ui.IWorkbenchPartConstants;
 import org.eclipse.ui.PartInitException;
 import org.eclipse.ui.part.FileEditorInput;
 
 import dk.itu.big_red.application.plugin.RedPlugin;
 import dk.itu.big_red.editors.AbstractEditor;
 import dk.itu.big_red.editors.signature.SignatureEditorPolygonCanvas.SEPCListener;
 import dk.itu.big_red.model.Control;
 import dk.itu.big_red.model.Control.Kind;
 import dk.itu.big_red.model.Control.Shape;
 import dk.itu.big_red.model.Colourable;
 import dk.itu.big_red.model.PortSpec;
 import dk.itu.big_red.model.Signature;
 import dk.itu.big_red.model.changes.ChangeRejectedException;
 import dk.itu.big_red.model.import_export.SignatureXMLExport;
 import dk.itu.big_red.model.import_export.SignatureXMLImport;
 import dk.itu.big_red.utilities.Colour;
 import dk.itu.big_red.utilities.Lists;
 import dk.itu.big_red.utilities.io.IOAdapter;
 import dk.itu.big_red.utilities.resources.Project;
 import dk.itu.big_red.utilities.ui.EditorError;
 import dk.itu.big_red.utilities.ui.UI;
 
 public class SignatureEditor extends AbstractEditor
 implements ISelectionListener, PropertyChangeListener {
 	public static final String ID = "dk.itu.big_red.SignatureEditor";
 	
 	public SignatureEditor() {
 	}
 	
 	@Override
 	public void doSave(IProgressMonitor monitor) {
 		try {
 			IOAdapter io = new IOAdapter();
         	FileEditorInput i = (FileEditorInput)getEditorInput();
         	SignatureXMLExport ex = new SignatureXMLExport();
         	
         	ex.setModel(model).setOutputStream(io.getOutputStream()).
         		exportObject();
         	Project.setContents(i.getFile(), io.getInputStream());
         	
        	RedPlugin.getObjectService().setObject(i.getFile(), model);
     		setDirty(false);
         } catch (Exception ex) {
         	if (monitor != null)
         		monitor.setCanceled(true);
         	UI.openError("Unable to save the document.", ex);
         }
 	}
 
 	@Override
 	public void doSaveAs() {
 		// TODO Auto-generated method stub
 
 	}
 
 	@Override
 	public void init(IEditorSite site, IEditorInput input)
 			throws PartInitException {
 		setSite(site);
 		setInput(input);
 		firePropertyChange(IWorkbenchPartConstants.PROP_INPUT);
 		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
 	}
 
 	protected boolean dirty = false;
 	
 	protected void setDirty(boolean dirty) {
 		if (this.dirty != dirty) {
 			this.dirty = dirty;
 			firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
 		}
 	}
 	
 	@Override
 	public boolean isDirty() {
 		return dirty;
 	}
 
 	@Override
 	public boolean isSaveAsAllowed() {
 		// TODO Auto-generated method stub
 		return false;
 	}
 
 	private Signature model = null;
 	
 	private dk.itu.big_red.model.Control currentControl;
 	
 	private Tree controls;
 	private TreeItem currentControlItem;
 	private Button addControl, removeControl;
 	
 	private Text name, label;
 	private SignatureEditorPolygonCanvas appearance;
 	private Button ovalMode, polygonMode, resizable;
 	private Button activeKind, atomicKind, passiveKind;
 	
 	private Label
 		appearanceDescription, kindLabel, labelLabel,
 		outlineLabel, fillLabel, nameLabel, appearanceLabel;
 	private ColorSelector outline, fill;
 	
 	protected void setControl(Control c) {
 		if (currentControl != null)
 			currentControl.removePropertyChangeListener(this);
 		currentControl = c;
 		c.addPropertyChangeListener(this);
 		
 		controlToFields();
 	}
 	
 	private boolean uiUpdateInProgress = false;
 	
 	protected void controlToFields() {
 		uiUpdateInProgress = true;
 		
 		boolean polygon = (currentControl.getShape() == Shape.POLYGON);
 		
 		label.setText(currentControl.getLabel());
 		name.setText(currentControl.getName());
 		appearance.setMode(polygon ? Shape.POLYGON : Shape.OVAL);
 		if (polygon)
 			appearance.setPoints(currentControl.getPoints());
 		appearance.setPorts(currentControl.getPorts());
 		resizable.setSelection(currentControl.isResizable());
 		currentControlItem.setText(currentControl.getName());
 		
 		ovalMode.setSelection(!polygon);
 		polygonMode.setSelection(polygon);
 		
 		outline.setColorValue(currentControl.getOutlineColour().getRGB());
 		fill.setColorValue(currentControl.getFillColour().getRGB());
 		
 		activeKind.setSelection(currentControl.getKind() == Kind.ACTIVE);
 		atomicKind.setSelection(currentControl.getKind() == Kind.ATOMIC);
 		passiveKind.setSelection(currentControl.getKind() == Kind.PASSIVE);
 		
 		uiUpdateInProgress = false;
 	}
 	
 	/**
 	 * Indicates whether or not changes made to the UI should be propagated
 	 * to the current {@link Control}.
 	 * @return <code>true</code> if the {@link Control} is valid and is not
 	 * itself currently changing the UI, or <code>false</code> otherwise
 	 */
 	private boolean shouldPropagateUI() {
 		return (!uiUpdateInProgress && currentControl != null);
 	}
 	
 	private static Font smiff;
 	
 	private Composite parent, self;
 	
 	private void error(Throwable t) {
 		self.dispose(); self = null;
 		new EditorError(parent, RedPlugin.getThrowableStatus(t));
 	}
 	
 	@Override
 	public void createPartControl(Composite parent) {
 		this.parent = parent;
 		self = new Composite(parent, SWT.NONE);
 		self.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
 		
 		GridLayout gl = new GridLayout(2, false);
 		gl.marginTop = gl.marginLeft = gl.marginBottom = gl.marginRight = 
 			gl.horizontalSpacing = gl.verticalSpacing = 10;
 		self.setLayout(gl);
 		
 		Composite left = new Composite(self, 0);
 		left.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
 		GridLayout leftLayout = new GridLayout(1, false);
 		left.setLayout(leftLayout);
 		
 		controls = new Tree(left, SWT.SINGLE | SWT.BORDER | SWT.VIRTUAL);
 		GridData controlsLayoutData =
 			new GridData(SWT.FILL, SWT.FILL, true, true);
 		controlsLayoutData.widthHint = 100;
 		controls.setLayoutData(controlsLayoutData);
 		controls.addSelectionListener(new SelectionListener() {
 			
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				currentControlItem = controls.getSelection()[0];
 				setControl((Control)UI.data(currentControlItem, controlKey));
 				name.setFocus();
 				setEnablement(true);
 			}
 			
 			@Override
 			public void widgetDefaultSelected(SelectionEvent e) {
 				widgetSelected(e);
 			}
 		});
 		
 		Composite controlButtons = new Composite(left, SWT.NONE);
 		RowLayout controlButtonsLayout = new RowLayout();
 		controlButtons.setLayout(controlButtonsLayout);
 		controlButtons.setLayoutData(new GridData(SWT.END, SWT.TOP, true, false));
 		
 		addControl = new Button(controlButtons, SWT.NONE);
 		addControl.setImage(UI.getImage(ISharedImages.IMG_OBJ_ADD));
 		addControl.addSelectionListener(new SelectionListener() {
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				Control c = new Control();
 				model.addControl(c);
 				currentControlItem =
 					UI.data(new TreeItem(controls, SWT.NONE), controlKey, c);
 				setControl(c);
 				
 				controls.select(currentControlItem);
 				name.setFocus();
 				
 				setEnablement(true);
 				setDirty(true);
 			}
 			
 			@Override
 			public void widgetDefaultSelected(SelectionEvent e) {
 				return;
 			}
 		});
 		
 		removeControl = new Button(controlButtons, SWT.NONE);
 		removeControl.setImage(UI.getImage(ISharedImages.IMG_ELCL_REMOVE));
 		removeControl.addSelectionListener(new SelectionListener() {
 			
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				model.removeControl(currentControl);
 				currentControlItem.dispose();
 				
 				if (controls.getItemCount() > 0) {
 					controls.select(controls.getItem(0));
 					currentControlItem = controls.getItem(0);
 					currentControl =
 							(Control)UI.data(currentControlItem, controlKey);
 					controlToFields();
 					name.setFocus();
 				} else setEnablement(false);
 				
 				setDirty(true);
 			}
 			
 			@Override
 			public void widgetDefaultSelected(SelectionEvent e) {
 				widgetSelected(e);
 			}
 		});
 		
 		Composite right = new Composite(self, 0);
 		right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
 		GridLayout rightLayout = new GridLayout(2, false);
 		right.setLayout(rightLayout);
 		
 		abstract class TextListener implements SelectionListener, FocusListener {
 			abstract void go();
 			
 			@Override
 			public void focusGained(FocusEvent e) {
 				/* nothing */
 			}
 
 			@Override
 			public void focusLost(FocusEvent e) {
 				if (shouldPropagateUI())
 					go();
 			}
 
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				/* nothing */
 			}
 
 			@Override
 			public void widgetDefaultSelected(SelectionEvent e) {
 				if (shouldPropagateUI())
 					go();
 			}
 			
 		}
 		
 		TextListener nameListener = new TextListener() {
 			@Override
 			void go() {
 				if (!currentControl.getName().equals(name.getText()))
 					currentControl.setName(name.getText());
 			}
 		};
 		
 		nameLabel = UI.newLabel(right, SWT.NONE, "Name:");
 		name = new Text(right, SWT.BORDER);
 		name.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
 		name.addSelectionListener(nameListener);
 		name.addFocusListener(nameListener);
 		
 		TextListener labelListener = new TextListener() {
 			@Override
 			void go() {
 				if (!currentControl.getLabel().equals(label.getText()))
 					currentControl.setLabel(label.getText());
 			}
 		};
 		
 		labelLabel = UI.newLabel(right, SWT.NONE, "Label:");
 		label = new Text(right, SWT.BORDER);
 		label.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
 		label.addSelectionListener(labelListener);
 		label.addFocusListener(labelListener);
 		
 		kindLabel = UI.newLabel(right, SWT.NONE, "Kind:");
 		
 		Composite kindGroup = new Composite(right, SWT.NONE);
 		kindGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
 		kindGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
 		
 		atomicKind = UI.newButton(kindGroup, SWT.RADIO, "Atomic");
 		atomicKind.addSelectionListener(new SelectionAdapter() {
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				if (shouldPropagateUI())
 					currentControl.setKind(Kind.ATOMIC);
 			}
 		});
 		
 		activeKind = UI.newButton(kindGroup, SWT.RADIO, "Active");
 		activeKind.addSelectionListener(new SelectionAdapter() {
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				if (shouldPropagateUI())
 					currentControl.setKind(Kind.ACTIVE);
 			}
 		});
 		
 		passiveKind = UI.newButton(kindGroup, SWT.RADIO, "Passive");
 		passiveKind.addSelectionListener(new SelectionAdapter() {
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				if (shouldPropagateUI())
 					currentControl.setKind(Kind.PASSIVE);
 			}
 		});
 		
 		appearanceLabel = UI.newLabel(right, SWT.NONE, "Appearance:");
 		GridData appearanceLabelLayoutData = new GridData(SWT.FILL, SWT.FILL, false, true);
 		appearanceLabel.setLayoutData(appearanceLabelLayoutData);
 		
 		Composite appearanceGroup = new Composite(right, SWT.NONE);
 		GridData appearanceGroupLayoutData = new GridData(SWT.FILL, SWT.FILL, false, true);
 		appearanceGroup.setLayoutData(appearanceGroupLayoutData);
 		GridLayout appearanceGroupLayout = new GridLayout(1, false);
 		appearanceGroup.setLayout(appearanceGroupLayout);		
 		
 		/* XXX: the addition of this row leads to really weird oversizing of
 		 * the polygon canvas! */
 		Composite firstLine = new Composite(appearanceGroup, SWT.NONE);
 		firstLine.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
 		firstLine.setLayout(new RowLayout(SWT.HORIZONTAL));
 		
 		ovalMode = UI.newButton(firstLine, SWT.RADIO, "Oval");
 		ovalMode.addSelectionListener(new SelectionAdapter() {
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				if (shouldPropagateUI())
 					currentControl.setShape(Shape.OVAL);
 			}
 		});
 		
 		polygonMode = UI.newButton(firstLine, SWT.RADIO, "Polygon");
 		polygonMode.setSelection(true);
 		polygonMode.addSelectionListener(new SelectionAdapter() {
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				if (shouldPropagateUI())
 					currentControl.setShape(Shape.POLYGON);
 			}
 		});
 		
 		resizable = UI.newButton(firstLine, SWT.CHECK, "Resizable?");
 		resizable.addSelectionListener(new SelectionAdapter() {
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				if (shouldPropagateUI())
 					currentControl.setResizable(resizable.getSelection());
 			}
 		});
 		
 		appearance = new SignatureEditorPolygonCanvas(appearanceGroup, SWT.BORDER);
 		GridData appearanceLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
 		appearanceLayoutData.widthHint = 100;
 		appearanceLayoutData.heightHint = 100;
 		appearance.setLayoutData(appearanceLayoutData);
 		appearance.setBackground(ColorConstants.listBackground);
 		appearance.addListener(new SEPCListener() {
 			
 			@Override
 			public void portChange() {
 				if (!shouldPropagateUI())
 					return;
 				ArrayList<PortSpec> toCopy = Lists.copy(appearance.getPorts());
 				for (PortSpec p : Lists.copy(currentControl.getPorts()))
 					currentControl.removePort(p.getName());
 				for (PortSpec p : toCopy)
 					currentControl.addPort(
 						new PortSpec(p.getName(), p.getSegment(),
 								p.getDistance()));
 			}
 			
 			@Override
 			public void pointChange() {
 				if (!shouldPropagateUI())
 					return;
 				currentControl.setPoints(appearance.getPoints().getCopy());
 			}
 		});
 		
 		if (smiff == null)
 			smiff = UI.tweakFont(appearanceLabel.getFont(), 8, SWT.ITALIC);
 		
 		appearanceDescription = UI.newLabel(appearanceGroup, SWT.CENTER | SWT.WRAP,
 			"Click to add a new point. Double-click a point to delete it. " +
 				"Move elements by clicking and dragging. " +
 				"Right-click for more options.");
 		GridData appearanceDescriptionData = new GridData();
 		appearanceDescriptionData.verticalAlignment = SWT.TOP;
 		appearanceDescriptionData.horizontalAlignment = SWT.FILL;
 		appearanceDescriptionData.widthHint = 0;
 		appearanceDescription.setLayoutData(appearanceDescriptionData);
 		appearanceDescription.setFont(smiff);
 		
 		outlineLabel = UI.newLabel(right, SWT.NONE, "Outline:");
 		outline = new ColorSelector(right);
 		outline.getButton().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
 		outline.addListener(new IPropertyChangeListener() {
 			@Override
 			public void propertyChange(PropertyChangeEvent event) {
 				if (!shouldPropagateUI())
 					return;
 				try {
 					model.tryApplyChange(
 							currentControl.changeOutlineColour(
 									new Colour(outline.getColorValue())));
 				} catch (ChangeRejectedException cre) {
 					cre.printStackTrace();
 				}
 			}
 		});
 		
 		fillLabel = UI.newLabel(right, SWT.NONE, "Fill:");
 		fill = new ColorSelector(right);
 		fill.getButton().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
 		fill.addListener(new IPropertyChangeListener() {
 			@Override
 			public void propertyChange(PropertyChangeEvent event) {
 				if (!shouldPropagateUI())
 					return;
 				try {
 					model.tryApplyChange(
 							currentControl.changeFillColour(
 									new Colour(fill.getColorValue())));
 				} catch (ChangeRejectedException cre) {
 					cre.printStackTrace();
 				}
 			}
 		});
 		
 		setEnablement(false);
 		initialiseSignatureEditor();
 	}
 
 	private void setEnablement(boolean enabled) {
 		UI.setEnabled(enabled,
 			name, label, appearance, appearanceDescription, resizable,
 			atomicKind, activeKind, passiveKind, outline.getButton(),
 			outlineLabel, fill.getButton(), ovalMode, fillLabel, polygonMode,
 			kindLabel, nameLabel, appearanceLabel, labelLabel);
 	}
 	
 	private static final String controlKey = ID + ".control";
 	
 	private void initialiseSignatureEditor() {
 		IEditorInput input = getEditorInput();
 		setPartName(input.getName());
 		
 		if (input instanceof FileEditorInput) {
 			FileEditorInput fi = (FileEditorInput)input;
 			try {
 				model = SignatureXMLImport.importFile(fi.getFile());
 			} catch (Exception e) {
 				error(e);
 				return;
 			}
 		}
 		
 		for (dk.itu.big_red.model.Control c : model.getControls())
 			UI.data(new TreeItem(controls, 0), controlKey, c).setText(c.getName());
 	}
 
 	@Override
 	public void setFocus() {
 		// TODO Auto-generated method stub
 
 	}
 
 	@Override
 	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
 		// TODO Auto-generated method stub
 		
 	}
 
 	@Override
 	public void propertyChange(java.beans.PropertyChangeEvent evt) {
 		if (evt.getSource().equals(currentControl)) {
 			if (uiUpdateInProgress)
 				return;
 			uiUpdateInProgress = true;
 			try {
 				String propertyName = evt.getPropertyName();
 				Object newValue = evt.getNewValue();
 				if (propertyName.equals(Control.PROPERTY_LABEL)) {
 					label.setText((String)newValue);
 				} else if (propertyName.equals(Control.PROPERTY_NAME)) {
 					name.setText((String)newValue);
 					currentControlItem.setText((String)newValue);
 				} else if (propertyName.equals(Control.PROPERTY_SHAPE)) {
 					appearance.setMode((Shape)newValue);
 					ovalMode.setSelection(Shape.OVAL.equals(newValue));
 					polygonMode.setSelection(Shape.POLYGON.equals(newValue));
 				} else if (propertyName.equals(Control.PROPERTY_POINTS)) {
 					if (appearance.getMode() == Shape.POLYGON)
 						appearance.setPoints((PointList)newValue);
 				} else if (propertyName.equals(Control.PROPERTY_PORT)) {
 					appearance.setPorts(currentControl.getPorts());
 				} else if (propertyName.equals(Control.PROPERTY_RESIZABLE)) {
 					resizable.setSelection((Boolean)newValue);
 				} else if (propertyName.equals(Colourable.PROPERTY_FILL)) {
 					fill.setColorValue(((Colour)newValue).getRGB());
 				} else if (propertyName.equals(Colourable.PROPERTY_OUTLINE)) {
 					outline.setColorValue(((Colour)newValue).getRGB());
 				} else if (propertyName.equals(Control.PROPERTY_KIND)) {
 					activeKind.setSelection(Kind.ACTIVE.equals(newValue));
 					atomicKind.setSelection(Kind.ATOMIC.equals(newValue));
 					passiveKind.setSelection(Kind.PASSIVE.equals(newValue));
 				}
 			} finally {
 				uiUpdateInProgress = false;
 			}
 		}
 		setDirty(true);
 	}
 
 	@Override
 	protected void initializeActionRegistry() {
 		super.initializeActionRegistry();
 	}
 
 	@Override
 	protected void createActions() {
 		// TODO Auto-generated method stub
 		
 	}
 }
