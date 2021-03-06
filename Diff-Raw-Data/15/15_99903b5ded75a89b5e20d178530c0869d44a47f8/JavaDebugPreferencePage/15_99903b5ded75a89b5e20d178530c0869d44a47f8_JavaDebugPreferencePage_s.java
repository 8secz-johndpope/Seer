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
 package org.eclipse.jdt.internal.debug.ui;
 
  
import com.ibm.icu.text.MessageFormat;

 import org.eclipse.core.runtime.Preferences;
 import org.eclipse.jdt.debug.core.IJavaBreakpoint;
 import org.eclipse.jdt.debug.core.JDIDebugModel;
 import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
 import org.eclipse.jdt.launching.JavaRuntime;
 import org.eclipse.jface.preference.FieldEditor;
 import org.eclipse.jface.preference.IPreferenceStore;
 import org.eclipse.jface.preference.IntegerFieldEditor;
 import org.eclipse.jface.preference.PreferencePage;
 import org.eclipse.jface.preference.StringFieldEditor;
 import org.eclipse.jface.util.IPropertyChangeListener;
 import org.eclipse.jface.util.PropertyChangeEvent;
 import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
 import org.eclipse.swt.graphics.Font;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Group;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.ui.IWorkbench;
 import org.eclipse.ui.IWorkbenchPreferencePage;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.dialogs.PreferenceLinkArea;
 import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
 
 /**
  * Preference page for debug preferences that apply specifically to
  * Java Debugging.
  */
 public class JavaDebugPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IPropertyChangeListener {
 	
 	/**
 	 * This class exists to provide visibility to the
 	 * <code>refreshValidState</code> method and to perform more intelligent
 	 * clearing of the error message.
 	 */
 	protected class JavaDebugIntegerFieldEditor extends IntegerFieldEditor {						
 		
 		public JavaDebugIntegerFieldEditor(String name, String labelText, Composite parent) {
 			super(name, labelText, parent);
 		}
 
 		protected void refreshValidState() {
 			super.refreshValidState();
 		}
 
 		protected void clearErrorMessage() {
 			if (canClearErrorMessage()) {
 				super.clearErrorMessage();
 			}
 		}
 	}
 	
 	// Suspend preference widgets
 	private Button fSuspendButton;
 	private Button fSuspendOnCompilationErrors;
 	private Button fSuspendDuringEvaluations;
 	private Button fOpenInspector;
 	private Button fPromptUnableToInstallBreakpoint;
	private CCombo fSuspendVMorThread;
 	
 	// Hot code replace preference widgets
 	private Button fAlertHCRButton;
 	private Button fAlertHCRNotSupportedButton;
 	private Button fAlertObsoleteButton;
 	private Button fPerformHCRWithCompilationErrors;
 	// Timeout preference widgets
 	private JavaDebugIntegerFieldEditor fTimeoutText;
 	private JavaDebugIntegerFieldEditor fConnectionTimeoutText;
 
 	public JavaDebugPreferencePage() {
 		super();
 		setPreferenceStore(JDIDebugUIPlugin.getDefault().getPreferenceStore());
 		setDescription(DebugUIMessages.JavaDebugPreferencePage_description); 
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
 	 */
 	protected Control createContents(Composite parent) {
 		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaDebugHelpContextIds.JAVA_DEBUG_PREFERENCE_PAGE);
 		Font font = parent.getFont();
 		
 		//The main composite
 		Composite composite = new Composite(parent, SWT.NONE);
 		GridLayout layout = new GridLayout();
 		layout.numColumns = 1;
 		layout.marginHeight=0;
 		layout.marginWidth=0;
 		composite.setLayout(layout);
 		GridData data = new GridData();
 		data.verticalAlignment = GridData.FILL;
 		data.horizontalAlignment = GridData.FILL;
 		composite.setLayoutData(data);		
 		composite.setFont(font);
 		
 		PreferenceLinkArea runLink = new PreferenceLinkArea(composite, SWT.NONE,
 				"org.eclipse.debug.ui.DebugPreferencePage", DebugUIMessages.JavaDebugPreferencePage_0, //$NON-NLS-1$
 				(IWorkbenchPreferenceContainer) getContainer(),null);
 
 		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
 		runLink.getControl().setLayoutData(data);	
 	
 		Composite comp= createGroupComposite(composite, 1, DebugUIMessages.JavaDebugPreferencePage_Suspend_Execution_1); 
 		fSuspendButton= createCheckButton(comp, DebugUIMessages.JavaDebugPreferencePage_Suspend__execution_on_uncaught_exceptions_1); 
 		fSuspendOnCompilationErrors= createCheckButton(comp, DebugUIMessages.JavaDebugPreferencePage_Suspend_execution_on_co_mpilation_errors_1); 
 		fSuspendDuringEvaluations= createCheckButton(comp, DebugUIMessages.JavaDebugPreferencePage_14);
 		fOpenInspector = createCheckButton(comp, DebugUIMessages.JavaDebugPreferencePage_20);
 		
 		Composite group = new Composite(comp, SWT.NONE);
 		GridLayout groupLayout = new GridLayout();
 		groupLayout.numColumns = 2;
 		groupLayout.marginHeight=0;
 		groupLayout.marginWidth=0;
 		group.setLayout(groupLayout);
 		data = new GridData();
 		data.verticalAlignment = GridData.FILL;
 		data.horizontalAlignment = GridData.FILL;
 		group.setLayoutData(data);
 		Label label = new Label(group, SWT.NONE);
 		label.setText(DebugUIMessages.JavaDebugPreferencePage_21);
		fSuspendVMorThread = new CCombo(group, SWT.BORDER);
 		fSuspendVMorThread.setItems(new String[]{DebugUIMessages.JavaDebugPreferencePage_22, DebugUIMessages.JavaDebugPreferencePage_23});
 				
 		comp = createGroupComposite(composite, 1, DebugUIMessages.JavaDebugPreferencePage_Hot_Code_Replace_2); 
 		fAlertHCRButton= createCheckButton(comp, DebugUIMessages.JavaDebugPreferencePage_Alert_me_when_hot_code_replace_fails_1); 
 		fAlertHCRNotSupportedButton= createCheckButton(comp, DebugUIMessages.JavaDebugPreferencePage_Alert_me_when_hot_code_replace_is_not_supported_1); 
 		fAlertObsoleteButton= createCheckButton(comp, DebugUIMessages.JavaDebugPreferencePage_Alert_me_when_obsolete_methods_remain_1); 
 		fPerformHCRWithCompilationErrors= createCheckButton(comp, DebugUIMessages.JavaDebugPreferencePage_Replace_classfiles_containing_compilation_errors_1); 
 
 		fPromptUnableToInstallBreakpoint= createCheckButton(composite, DebugUIMessages.JavaDebugPreferencePage_19); 
 
 		comp = createGroupComposite(composite, 1, DebugUIMessages.JavaDebugPreferencePage_Communication_1); 
 		//Add in an intermediate composite to allow for spacing
 		Composite spacingComposite = new Composite(comp, SWT.NONE);
 		layout = new GridLayout();
 		spacingComposite.setLayout(layout);
 		data = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
 		data.horizontalSpan = 2;
 		spacingComposite.setLayoutData(data);
 		spacingComposite.setFont(font);
 		
 		int minValue;
         Preferences coreStore= JDIDebugModel.getPreferences();
         Preferences runtimeStore= JavaRuntime.getPreferences();
 		fTimeoutText = new JavaDebugIntegerFieldEditor(JDIDebugModel.PREF_REQUEST_TIMEOUT, DebugUIMessages.JavaDebugPreferencePage_Debugger__timeout__2, spacingComposite);
 		fTimeoutText.setPage(this);
 		fTimeoutText.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);
 		minValue= coreStore.getDefaultInt(JDIDebugModel.PREF_REQUEST_TIMEOUT);
 		fTimeoutText.setValidRange(minValue, Integer.MAX_VALUE);
 		fTimeoutText.setErrorMessage(MessageFormat.format(DebugUIMessages.JavaDebugPreferencePage_Value_must_be_a_valid_integer_greater_than__0__ms_1, new Object[] {new Integer(minValue)})); 
 		fTimeoutText.load();
 		fTimeoutText.setPropertyChangeListener(this);
 		fConnectionTimeoutText = new JavaDebugIntegerFieldEditor(JavaRuntime.PREF_CONNECT_TIMEOUT, DebugUIMessages.JavaDebugPreferencePage__Launch_timeout__ms___1, spacingComposite); 
 		fConnectionTimeoutText.setPage(this);
 		fConnectionTimeoutText.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);
 		minValue= runtimeStore.getDefaultInt(JavaRuntime.PREF_CONNECT_TIMEOUT);
 		fConnectionTimeoutText.setValidRange(minValue, Integer.MAX_VALUE);
 		fConnectionTimeoutText.setErrorMessage(MessageFormat.format(DebugUIMessages.JavaDebugPreferencePage_Value_must_be_a_valid_integer_greater_than__0__ms_1, new Object[] {new Integer(minValue)})); 
 		fConnectionTimeoutText.load();
 		fConnectionTimeoutText.setPropertyChangeListener(this);
 
 		setValues();
 		applyDialogFont(composite);
 		return composite;		
 	}
 		
 	/* (non-Javadoc)
 	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
 	 */
 	public void init(IWorkbench workbench) {}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
 	 */
 	public boolean performOk() {
 		IPreferenceStore store = getPreferenceStore();
 		Preferences coreStore = JDIDebugModel.getPreferences();
 		Preferences runtimeStore = JavaRuntime.getPreferences();
 		
 		store.setValue(IJDIPreferencesConstants.PREF_SUSPEND_ON_UNCAUGHT_EXCEPTIONS, fSuspendButton.getSelection());
 		store.setValue(IJDIPreferencesConstants.PREF_SUSPEND_ON_COMPILATION_ERRORS, fSuspendOnCompilationErrors.getSelection());
 		coreStore.setValue(JDIDebugModel.PREF_SUSPEND_FOR_BREAKPOINTS_DURING_EVALUATION, fSuspendDuringEvaluations.getSelection());
 		int selectionIndex = fSuspendVMorThread.getSelectionIndex();
 		int policy = IJavaBreakpoint.SUSPEND_THREAD;
 		if (selectionIndex > 0) {
 			policy = IJavaBreakpoint.SUSPEND_VM;
 		}
 		coreStore.setValue(JDIDebugPlugin.PREF_DEFAULT_BREAKPOINT_SUSPEND_POLICY, policy);
 		store.setValue(IJDIPreferencesConstants.PREF_ALERT_HCR_FAILED, fAlertHCRButton.getSelection());
 		store.setValue(IJDIPreferencesConstants.PREF_ALERT_HCR_NOT_SUPPORTED, fAlertHCRNotSupportedButton.getSelection());
 		store.setValue(IJDIPreferencesConstants.PREF_ALERT_OBSOLETE_METHODS, fAlertObsoleteButton.getSelection());
 		coreStore.setValue(JDIDebugModel.PREF_HCR_WITH_COMPILATION_ERRORS, fPerformHCRWithCompilationErrors.getSelection());
 		coreStore.setValue(JDIDebugModel.PREF_REQUEST_TIMEOUT, fTimeoutText.getIntValue());
 		runtimeStore.setValue(JavaRuntime.PREF_CONNECT_TIMEOUT, fConnectionTimeoutText.getIntValue());
 		store.setValue(IJDIPreferencesConstants.PREF_ALERT_UNABLE_TO_INSTALL_BREAKPOINT, fPromptUnableToInstallBreakpoint.getSelection());
 		store.setValue(IJDIPreferencesConstants.PREF_OPEN_INSPECT_POPUP_ON_EXCEPTION, fOpenInspector.getSelection());
 		JDIDebugUIPlugin.getDefault().savePluginPreferences();
 		JDIDebugModel.savePreferences();
 		JavaRuntime.savePreferences();
 		return true;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
 	 */
 	protected void performDefaults() {
 		IPreferenceStore store = getPreferenceStore();
 		Preferences coreStore= JDIDebugModel.getPreferences();
 		Preferences runtimeStore= JavaRuntime.getPreferences();
 		
 		fSuspendButton.setSelection(store.getDefaultBoolean(IJDIPreferencesConstants.PREF_SUSPEND_ON_UNCAUGHT_EXCEPTIONS));
 		fSuspendOnCompilationErrors.setSelection(store.getDefaultBoolean(IJDIPreferencesConstants.PREF_SUSPEND_ON_COMPILATION_ERRORS));
 		fSuspendDuringEvaluations.setSelection(coreStore.getDefaultBoolean(JDIDebugModel.PREF_SUSPEND_FOR_BREAKPOINTS_DURING_EVALUATION));
 		int value = coreStore.getDefaultInt(JDIDebugPlugin.PREF_DEFAULT_BREAKPOINT_SUSPEND_POLICY);
 		fSuspendVMorThread.select((value == IJavaBreakpoint.SUSPEND_THREAD) ? 0 : 1);
 		fAlertHCRButton.setSelection(store.getDefaultBoolean(IJDIPreferencesConstants.PREF_ALERT_HCR_FAILED));
 		fAlertHCRNotSupportedButton.setSelection(store.getDefaultBoolean(IJDIPreferencesConstants.PREF_ALERT_HCR_NOT_SUPPORTED));
 		fAlertObsoleteButton.setSelection(store.getDefaultBoolean(IJDIPreferencesConstants.PREF_ALERT_OBSOLETE_METHODS));
 		fPerformHCRWithCompilationErrors.setSelection(coreStore.getDefaultBoolean(JDIDebugModel.PREF_HCR_WITH_COMPILATION_ERRORS));
 		fTimeoutText.setStringValue(new Integer(coreStore.getDefaultInt(JDIDebugModel.PREF_REQUEST_TIMEOUT)).toString());
 		fConnectionTimeoutText.setStringValue(new Integer(runtimeStore.getDefaultInt(JavaRuntime.PREF_CONNECT_TIMEOUT)).toString());
 		fPromptUnableToInstallBreakpoint.setSelection(store.getDefaultBoolean(IJDIPreferencesConstants.PREF_ALERT_UNABLE_TO_INSTALL_BREAKPOINT));
 		fOpenInspector.setSelection(store.getDefaultBoolean(IJDIPreferencesConstants.PREF_OPEN_INSPECT_POPUP_ON_EXCEPTION));
 		super.performDefaults();	
 	}
 	
 	/**
 	 * Creates a button with the given label and sets the default 
 	 * configuration data.
 	 */
 	private Button createCheckButton(Composite parent, String label) {
 		Button button= new Button(parent, SWT.CHECK | SWT.LEFT);
 		button.setText(label);		
 		GridData data = new GridData();	
 		button.setLayoutData(data);
 		button.setFont(parent.getFont());
 		
 		return button;
 	}
 	
 	/**
 	 * Creates composite group and sets the default layout data.
 	 *
 	 * @param parent  the parent of the new composite
 	 * @param numColumns  the number of columns for the new composite
 	 * @param labelText  the text label of the new composite
 	 * @return the newly-created composite
 	 */
 	private Composite createGroupComposite(Composite parent, int numColumns, String labelText) {
 		Group comp = new Group(parent, SWT.NONE);
 		comp.setLayout(new GridLayout(numColumns, true));
 		GridData gd = new GridData();
 		gd.verticalAlignment = GridData.FILL;
 		gd.horizontalAlignment = GridData.FILL;
 		comp.setLayoutData(gd);
 		comp.setText(labelText);
 		comp.setFont(parent.getFont());
 		return comp;
 	}
 		
 	/**
 	 * Set the values of the component widgets based on the
 	 * values in the preference store
 	 */
 	private void setValues() {
 		IPreferenceStore store = getPreferenceStore();
 		Preferences coreStore = JDIDebugModel.getPreferences();
 		Preferences runtimeStore = JavaRuntime.getPreferences();
 		
 		fSuspendButton.setSelection(store.getBoolean(IJDIPreferencesConstants.PREF_SUSPEND_ON_UNCAUGHT_EXCEPTIONS));
 		fSuspendOnCompilationErrors.setSelection(store.getBoolean(IJDIPreferencesConstants.PREF_SUSPEND_ON_COMPILATION_ERRORS));
 		fSuspendDuringEvaluations.setSelection(coreStore.getBoolean(JDIDebugModel.PREF_SUSPEND_FOR_BREAKPOINTS_DURING_EVALUATION));
 		int value = coreStore.getInt(JDIDebugPlugin.PREF_DEFAULT_BREAKPOINT_SUSPEND_POLICY);
 		fSuspendVMorThread.select((value == IJavaBreakpoint.SUSPEND_THREAD ? 0 : 1));
 		fAlertHCRButton.setSelection(store.getBoolean(IJDIPreferencesConstants.PREF_ALERT_HCR_FAILED));
 		fAlertHCRNotSupportedButton.setSelection(store.getBoolean(IJDIPreferencesConstants.PREF_ALERT_HCR_NOT_SUPPORTED));
 		fAlertObsoleteButton.setSelection(store.getBoolean(IJDIPreferencesConstants.PREF_ALERT_OBSOLETE_METHODS));
 		fPerformHCRWithCompilationErrors.setSelection(coreStore.getBoolean(JDIDebugModel.PREF_HCR_WITH_COMPILATION_ERRORS));
 		fTimeoutText.setStringValue(new Integer(coreStore.getInt(JDIDebugModel.PREF_REQUEST_TIMEOUT)).toString());
 		fConnectionTimeoutText.setStringValue(new Integer(runtimeStore.getInt(JavaRuntime.PREF_CONNECT_TIMEOUT)).toString());
 		fPromptUnableToInstallBreakpoint.setSelection(store.getBoolean(IJDIPreferencesConstants.PREF_ALERT_UNABLE_TO_INSTALL_BREAKPOINT));
 		fOpenInspector.setSelection(store.getBoolean(IJDIPreferencesConstants.PREF_OPEN_INSPECT_POPUP_ON_EXCEPTION));
 	}
 
 	/**
 	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
 	 */
 	public void propertyChange(PropertyChangeEvent event) {
 		if (event.getProperty().equals(FieldEditor.IS_VALID)) {
 			boolean newValue = ((Boolean) event.getNewValue()).booleanValue();
 			// If the new value is true then we must check all field editors.
 			// If it is false, then the page is invalid in any case.
 			if (newValue) {
 				if (fTimeoutText != null && event.getSource() != fTimeoutText) {
 					fTimeoutText.refreshValidState();
 				} 
 				if (fConnectionTimeoutText != null && event.getSource() != fConnectionTimeoutText) {
 					fConnectionTimeoutText.refreshValidState();
 				}
 			} 
 			setValid(fTimeoutText.isValid() && fConnectionTimeoutText.isValid());
 			getContainer().updateButtons();
 			updateApplyButton();
 		}
 	}
 
 	/**
 	 * if the error message can be cleared or not
 	 * @return true if the error message can be cleared, false otherwise
 	 */
 	protected boolean canClearErrorMessage() {
 		if (fTimeoutText.isValid() && fConnectionTimeoutText.isValid()) {
 			return true;
 		}
 		return false;
 	}	
 }
