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
 package org.eclipse.team.internal.ccvs.ui.repo;
 
 import java.lang.reflect.InvocationTargetException;
 import java.util.*;
 import java.util.List;
 
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.IAdaptable;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.jface.dialogs.*;
 import org.eclipse.jface.dialogs.Dialog;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.*;
 import org.eclipse.team.core.RepositoryProvider;
 import org.eclipse.team.core.TeamException;
 import org.eclipse.team.internal.ccvs.core.*;
 import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
 import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
 import org.eclipse.team.internal.ccvs.ui.*;
 import org.eclipse.team.internal.ccvs.ui.Policy;
 import org.eclipse.team.internal.ccvs.ui.wizards.ConfigurationWizardMainPage;
 import org.eclipse.team.internal.ui.dialogs.DetailsDialogWithProjects;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.actions.WorkspaceModifyOperation;
 import org.eclipse.ui.dialogs.PropertyPage;
 import org.eclipse.ui.help.WorkbenchHelp;
 
 public class CVSRepositoryPropertiesPage extends PropertyPage {
 	ICVSRepositoryLocation location;
 	
 	// Widgets
 	Text userText;
 	Text passwordText;
 	Combo methodType;
 	Text hostText;
 	Text pathText;
 	// Port
 	private Text portText;
 	private Button useDefaultPort;
 	private Button useCustomPort;
 	
 	// Caching password
 	private Button allowCachingButton;
 	private boolean allowCaching = false;
 	
 	boolean passwordChanged;
 	boolean connectionInfoChanged;
 
 	IUserInfo info;
 
 	// Label
 	private Button useLocationAsLabel;
 	private Button useCustomLabel;
 	private Text labelText;
 			
 	/*
 	 * @see PreferencesPage#createContents
 	 */
 	protected Control createContents(Composite parent) {
 		initialize();
 		
 		Composite composite = new Composite(parent, SWT.NULL);
 		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
 		GridLayout layout = new GridLayout();
 		layout.numColumns = 3;
 		composite.setLayout(layout);
 
 		// Repository Label
 		// create a composite to ensure the radio buttons come in the correct order
 		Composite labelGroup = new Composite(composite, SWT.NONE);
 		GridData data = new GridData();
 		data.horizontalSpan = 3;
 		labelGroup.setLayoutData(data);
 		layout = new GridLayout();
 		layout.numColumns = 3;
 		layout.marginHeight = 0;
 		layout.marginWidth = 0;
 		labelGroup.setLayout(layout);
 		Listener labelListener = new Listener() {
 			public void handleEvent(Event event) {
 				updateWidgetEnablements();
 			}
 		};
 		useLocationAsLabel = createRadioButton(labelGroup, Policy.bind("CVSRepositoryPropertiesPage.useLocationAsLabel"), 3); //$NON-NLS-1$
 		useCustomLabel = createRadioButton(labelGroup, Policy.bind("CVSRepositoryPropertiesPage.useCustomLabel"), 1); //$NON-NLS-1$
 		useCustomLabel.addListener(SWT.Selection, labelListener);
 		labelText = createTextField(labelGroup);
 		labelText.addListener(SWT.Modify, labelListener);
 		
 		// Add some extra space
 		createLabel(composite, "", 3); //$NON-NLS-1$
 		
 		createLabel(composite, Policy.bind("CVSPropertiesPage.connectionType"), 1); //$NON-NLS-1$
 		methodType = createCombo(composite);
 		
 		createLabel(composite, Policy.bind("CVSPropertiesPage.user"), 1); //$NON-NLS-1$
 		userText = createTextField(composite);
 		
 		createLabel(composite, Policy.bind("CVSPropertiesPage.password"), 1); //$NON-NLS-1$
 		passwordText = createPasswordField(composite);
 			
 		createLabel(composite, Policy.bind("CVSPropertiesPage.host"), 1); //$NON-NLS-1$
 		hostText = createTextField(composite);
 		
 		createLabel(composite, Policy.bind("CVSPropertiesPage.path"), 1); //$NON-NLS-1$
 		pathText = createTextField(composite);
 		
 		// Port number
 		// create a composite to ensure the radio buttons come in the correct order
 		Composite portGroup = new Composite(composite, SWT.NONE);
 		data = new GridData();
 		data.horizontalSpan = 3;
 		portGroup.setLayoutData(data);
 		layout = new GridLayout();
 		layout.numColumns = 3;
 		layout.marginHeight = 0;
 		layout.marginWidth = 0;
 		portGroup.setLayout(layout);
 		useDefaultPort = createRadioButton(portGroup, Policy.bind("ConfigurationWizardMainPage.useDefaultPort"), 3); //$NON-NLS-1$
 		useCustomPort = createRadioButton(portGroup, Policy.bind("ConfigurationWizardMainPage.usePort"), 1); //$NON-NLS-1$
 		portText = createTextField(portGroup);
 
 		// Add some extra space
 		createLabel(composite, "", 3); //$NON-NLS-1$
 
 		allowCachingButton = new Button(composite, SWT.CHECK);
 		allowCachingButton.setText(Policy.bind("UserValidationDialog.6")); //$NON-NLS-1$
 		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
 		data.horizontalSpan = 3;
 		allowCachingButton.setLayoutData(data);
 		allowCachingButton.addSelectionListener(new SelectionAdapter() {
 			public void widgetSelected(SelectionEvent e) {
 				allowCaching = allowCachingButton.getSelection();
 			}
 		});
 	
 		Composite warningComposite = new Composite(composite, SWT.NONE);
 		layout = new GridLayout();
 		layout.numColumns = 2;
 		layout.marginHeight = 0;
 		layout.marginHeight = 0;
 		warningComposite.setLayout(layout);
 		data = new GridData(GridData.FILL_HORIZONTAL);
 		data.horizontalSpan = 3;
 		warningComposite.setLayoutData(data);
 		Label warningLabel = new Label(warningComposite, SWT.NONE);
 		warningLabel.setImage(Dialog.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
 		warningLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_BEGINNING));
 		Label warningText = new Label(warningComposite, SWT.WRAP);
 		warningText.setText(Policy.bind("UserValidationDialog.7")); //$NON-NLS-1$
 		data = new GridData(GridData.FILL_HORIZONTAL);
 		data.widthHint = 300;
 		warningText.setLayoutData(data);
 		
 		// Add some extra space
 		createLabel(composite, "", 3); //$NON-NLS-1$
 		
 		initializeValues();
 		updateWidgetEnablements();
 		Listener connectionInfoChangedListener = new Listener() {
 			public void handleEvent(Event event) {
 				connectionInfoChanged = true;
 				updateWidgetEnablements();
 			}
 		};
 		passwordText.addListener(SWT.Modify, new Listener() {
 			public void handleEvent(Event event) {
				passwordChanged = true;
 			}
 		});
 		userText.addListener(SWT.Modify, connectionInfoChangedListener);
 		methodType.addListener(SWT.Modify, connectionInfoChangedListener);
 		hostText.addListener(SWT.Modify, connectionInfoChangedListener);
 		portText.addListener(SWT.Modify, connectionInfoChangedListener);
 		useCustomPort.addListener(SWT.Selection, connectionInfoChangedListener);
 		pathText.addListener(SWT.Modify, connectionInfoChangedListener);
 		
 		WorkbenchHelp.setHelp(getControl(), IHelpContextIds.REPOSITORY_LOCATION_PROPERTY_PAGE);
         Dialog.applyDialogFont(parent);
 		return composite;
 	}
 	
 	/**
 	 * Utility method that creates a combo box
 	 *
 	 * @param parent  the parent for the new label
 	 * @return the new widget
 	 */
 	protected Combo createCombo(Composite parent) {
 		Combo combo = new Combo(parent, SWT.READ_ONLY);
 		GridData data = new GridData(GridData.FILL_HORIZONTAL);
 		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
 		data.horizontalSpan = 2;
 		combo.setLayoutData(data);
 		return combo;
 	}
 	/**
 	 * Utility method that creates a label instance
 	 * and sets the default layout data.
 	 *
 	 * @param parent  the parent for the new label
 	 * @param text  the text for the new label
 	 * @return the new label
 	 */
 	protected Label createLabel(Composite parent, String text, int span) {
 		Label label = new Label(parent, SWT.LEFT);
 		label.setText(text);
 		GridData data = new GridData();
 		data.horizontalSpan = span;
 		data.horizontalAlignment = GridData.FILL;
 		label.setLayoutData(data);
 		return label;
 	}
 	/**
 	 * Create a text field specific for this application
 	 *
 	 * @param parent  the parent of the new text field
 	 * @return the new text field
 	 */
 	protected Text createTextField(Composite parent) {
 		Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
 		return layoutTextField(text);
 	}
 	/**
 	 * Create a password field specific for this application
 	 *
 	 * @param parent  the parent of the new text field
 	 * @return the new text field
 	 */
 	protected Text createPasswordField(Composite parent) {
 		Text text = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
 		return layoutTextField(text);
 	}
 	/**
 	 * Layout a text or password field specific for this application
 	 *
 	 * @param parent  the parent of the new text field
 	 * @return the new text field
 	 */
 	protected Text layoutTextField(Text text) {
 		GridData data = new GridData(GridData.FILL_HORIZONTAL);
 		data.verticalAlignment = GridData.CENTER;
 		data.grabExcessVerticalSpace = false;
 		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
 		data.horizontalSpan = 2;
 		text.setLayoutData(data);
 		return text;
 	}
 
 	/**
 	 * Utility method to create a radio button
 	 * 
 	 * @param parent  the parent of the radio button
 	 * @param label  the label of the radio button
 	 * @param span  the number of columns to span
 	 * @return the created radio button
 	 */
 	protected Button createRadioButton(Composite parent, String label, int span) {
 		Button button = new Button(parent, SWT.RADIO);
 		button.setText(label);
 		GridData data = new GridData();
 		data.horizontalSpan = span;
 		button.setLayoutData(data);
 		return button;
 	}
 	/**
 	 * Initializes the page
 	 */
 	private void initialize() {
 		location = null;
 		IAdaptable element = getElement();
 		if (element instanceof ICVSRepositoryLocation) {
 			location = (ICVSRepositoryLocation)element;
 		} else {
 			Object adapter = element.getAdapter(ICVSRepositoryLocation.class);
 			if (adapter instanceof ICVSRepositoryLocation) {
 				location = (ICVSRepositoryLocation)adapter;
 			}
 		}
 	}
 	/**
 	 * Set the initial values of the widgets
 	 */
 	private void initializeValues() {
 		passwordChanged = false;
 		connectionInfoChanged = false;
 		
 		IConnectionMethod[] methods = CVSRepositoryLocation.getPluggedInConnectionMethods();
 		for (int i = 0; i < methods.length; i++) {
 			methodType.add(methods[i].getName());
 		}
 		String method = location.getMethod().getName();
 		methodType.select(methodType.indexOf(method));
 		info = location.getUserInfo(true);
 		userText.setText(info.getUsername());
		passwordText.setText("*********"); //$NON-NLS-1$
 		hostText.setText(location.getHost());
 		int port = location.getPort();
 		if (port == ICVSRepositoryLocation.USE_DEFAULT_PORT) {
 			useDefaultPort.setSelection(true);
 			useCustomPort.setSelection(false);
 			portText.setEnabled(false);
 		} else {
 			useDefaultPort.setSelection(false);
 			useCustomPort.setSelection(true);
 			portText.setText("" + port); //$NON-NLS-1$
 		}
 		pathText.setText(location.getRootDirectory());
 		allowCachingButton.setSelection(location.getUserInfoCached());
 		
 		// get the repository label
 		String label = null;
 		RepositoryRoot root = CVSUIPlugin.getPlugin().getRepositoryManager().getRepositoryRootFor(location);
 		label = root.getName();
 		useLocationAsLabel.setSelection(label == null);
 		useCustomLabel.setSelection(!useLocationAsLabel.getSelection());
 		if (label == null) {
 			label = location.getLocation();
 		}
 		labelText.setText(label);
 	}
 	
 	private boolean performConnectionInfoChanges() {
 		// Set the caching mode of the location
 		if (!connectionInfoChanged) {
 			location.setAllowCaching(allowCaching);
 			if (!passwordChanged) {
 				((CVSRepositoryLocation)location).updateCache();
 			}
 		}
 		// Don't do anything else if there wasn't a password or connection change
 		if (!passwordChanged && !connectionInfoChanged) return true;
 		
 		try {
 			// Check if the password was the only thing to change.
 			if (passwordChanged && !connectionInfoChanged) {
 				CVSRepositoryLocation oldLocation = (CVSRepositoryLocation)location;
 				oldLocation.setPassword(getNewPassword());
 				if (allowCaching) {
 					oldLocation.updateCache();
 				}
 				passwordChanged = false;
 				return true;
 			}
 		
 			// Otherwise change the connection info and the password
 			// This operation is done inside a workspace operation in case the sharing
 			// info for existing projects is changed
 			if (!(location.getHost().equals(hostText.getText()) && location.getRootDirectory().equals(pathText.getText()))) {
 				// The host or path has changed
 				if (!MessageDialog.openConfirm(getShell(), 
 						Policy.bind("CVSRepositoryPropertiesPage.0"),  //$NON-NLS-1$
 						Policy.bind("CVSRepositoryPropertiesPage.1"))) { //$NON-NLS-1$
 					return false;
 				}
 			}
 			final boolean[] result = new boolean[] { false };
 			PlatformUI.getWorkbench().getProgressService().run(false, false, new WorkspaceModifyOperation(null) {
 				public void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
 					try {
 						// Create a new repository location with the new information
 						CVSRepositoryLocation newLocation = CVSRepositoryLocation.fromProperties(createProperties());
 						location.setAllowCaching(allowCaching);
 						try {
 							// For each project shared with the old location, set connection info to the new one
 							List projects = new ArrayList();
 							IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
 							for (int i = 0; i < allProjects.length; i++) {
 								RepositoryProvider teamProvider = RepositoryProvider.getProvider(allProjects[i], CVSProviderPlugin.getTypeId());
 								if (teamProvider != null) {
 									CVSTeamProvider cvsProvider = (CVSTeamProvider)teamProvider;
 									if (cvsProvider.getCVSWorkspaceRoot().getRemoteLocation().equals(location)) {
 										projects.add(allProjects[i]);
 									}
 								}
 							}
 							if (projects.size() > 0) {
 								// To do: warn the user
 								DetailsDialogWithProjects dialog = new DetailsDialogWithProjects(
 									getShell(), 
 									Policy.bind("CVSRepositoryPropertiesPage.Confirm_Project_Sharing_Changes_1"), //$NON-NLS-1$
 									Policy.bind("CVSRepositoryPropertiesPage.There_are_projects_in_the_workspace_shared_with_this_repository_2"), //$NON-NLS-1$
 									Policy.bind("CVSRepositoryPropertiesPage.sharedProject", location.toString()), //$NON-NLS-1$
 									(IProject[]) projects.toArray(new IProject[projects.size()]),
 									true,
 									DetailsDialogWithProjects.DLG_IMG_WARNING);
 								int r = dialog.open();
 								if (r != DetailsDialogWithProjects.OK) {
 									result[0] = false;
 									return;
 								}
 								monitor.beginTask(null, 1000 * projects.size());
 								try {
 									Iterator it = projects.iterator();
 									while (it.hasNext()) {
 										IProject project = (IProject)it.next();
 										RepositoryProvider teamProvider = RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId());
 										CVSTeamProvider cvsProvider = (CVSTeamProvider)teamProvider;
 										cvsProvider.setRemoteRoot(newLocation, Policy.subMonitorFor(monitor, 1000));
 									}
 								} finally {
 									monitor.done();
 								}
 							}
 							
 							// Dispose the old repository location
 							CVSUIPlugin.getPlugin().getRepositoryManager().replaceRepositoryLocation(location, newLocation);
 							
 						} finally {
 							// Even if we failed, ensure that the new location appears in the repo view.
 							newLocation = (CVSRepositoryLocation)KnownRepositories.getInstance().addRepository(newLocation, !KnownRepositories.getInstance().isKnownRepository(newLocation.getLocation()));
 						}
 						
 						// Set the location of the page to the new location in case Apply was chosen
 						location = newLocation;
 						connectionInfoChanged = false;
 						passwordChanged = false;
 					} catch (TeamException e) {
 						throw new InvocationTargetException(e);
 					}
 					result[0] = true;
 				}
 			});
 			return result[0];
 		} catch (InvocationTargetException e) {
 			handle(e);
 		} catch (InterruptedException e) {
 		}
 		return false; /* we only get here if an exception occurred */
 	}
 	
 	private void performNonConnectionInfoChanges() {
 		recordNewLabel((CVSRepositoryLocation)location);
 	}
 	/*
 	 * @see PreferencesPage#performOk
 	 */
 	public boolean performOk() {
 		if (performConnectionInfoChanges()) {
 			performNonConnectionInfoChanges();
 			return true;
 		}
 		return false;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
 	 */
 	protected void performDefaults() {
 		super.performDefaults();
 		initializeValues();
 	}
 	
 	/**
 	 * Shows the given errors to the user.
 	 */
 	protected void handle(Throwable e) {
 		CVSUIPlugin.openError(getShell(), null, null, e);
 	}
 	
 	/**
 	 * Updates widget enablements and sets error message if appropriate.
 	 */
 	protected void updateWidgetEnablements() {
 		if (useLocationAsLabel.getSelection()) {
 			labelText.setEnabled(false);
 		} else {
 			labelText.setEnabled(true);
 		}
 		if (useDefaultPort.getSelection()) {
 			portText.setEnabled(false);
 		} else {
 			portText.setEnabled(true);
 		}
 		validateFields();
 	}
 	
 	private void validateFields() {
 		if (labelText.isEnabled()) {
 			if (labelText.getText().length() == 0) {
 				setValid(false);
 				return;
 			}
 		}
 		String user = userText.getText();
 		IStatus status = ConfigurationWizardMainPage.validateUserName(user);
 		if (!isStatusOK(status)) {
 			return;
 		}
 
 		String host = hostText.getText();
 		status = ConfigurationWizardMainPage.validateHost(host);
 		if (!isStatusOK(status)) {
 			return;
 		}
 
 		if (portText.isEnabled()) {
 			String port = portText.getText();
 			status = ConfigurationWizardMainPage.validatePort(port);
 			if (!isStatusOK(status)) {
 				return;
 			}
 		}
 
 		String pathString = pathText.getText();
 		if (!isStatusOK(status)) {
 			return;
 		}
 		
 		setErrorMessage(null);
 		setValid(true);
 	}
 	
 	private boolean isStatusOK(IStatus status) {
 		if (!status.isOK()) {
 			setErrorMessage(status.getMessage());
 			setValid(false);
 			return false;
 		}
 		return true;
 	}
 	
 	private void recordNewLabel(CVSRepositoryLocation location) {
 		String newLabel = getNewLabel(location);
 		if (newLabel == null) {
 			String oldLabel = getOldLabel(location);
 			if (oldLabel == null || oldLabel.equals(location.getLocation())) {
 				return;
 			}
 		} else if (newLabel.equals(getOldLabel(location))) {
 			return;
 		}
 		try {
 			CVSUIPlugin.getPlugin().getRepositoryManager().setLabel(location, newLabel);
 		} catch (CVSException e) {
 			CVSUIPlugin.log(e);
 		}
 	}
 	private String getOldLabel(CVSRepositoryLocation location) {
 		return CVSUIPlugin.getPlugin().getRepositoryManager().getRepositoryRootFor(location).getName();
 	}
 	private String getNewLabel(CVSRepositoryLocation location) {
 		String label = null;
 		if (useCustomLabel.getSelection()) {
 			label = labelText.getText();
 			if (label.equals(location.getLocation())) {
 				label = null;
 			}
 		}
 		return label;
 	}
 	/* internal use only */ String getNewPassword() {
 		return passwordText.getText();
 	}
 	
 	private Properties createProperties() {
 		Properties result = new Properties();
 		result.setProperty("connection", methodType.getText()); //$NON-NLS-1$
 		result.setProperty("user", userText.getText()); //$NON-NLS-1$
		result.setProperty("password", passwordText.getText()); //$NON-NLS-1$
 		result.setProperty("host", hostText.getText()); //$NON-NLS-1$
 		if (useCustomPort.getSelection()) {
 			result.setProperty("port", portText.getText()); //$NON-NLS-1$
 		}
 		result.setProperty("root", pathText.getText()); //$NON-NLS-1$
 		return result;
 	}
 }
 
