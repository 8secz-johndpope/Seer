 /*******************************************************************************
  * Copyright (c) 2007 Red Hat, Inc.
  * Distributed under license by Red Hat, Inc. All rights reserved.
  * This program is made available under the terms of the
  * Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Red Hat, Inc. - initial API and implementation
  ******************************************************************************/ 
 
 package org.jboss.tools.seam.ui.preferences;
 
 import java.util.ArrayList;
 
 import org.eclipse.core.resources.IProject;
 import org.eclipse.jdt.core.JavaCore;
 import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
 import org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock;
 import org.eclipse.jdt.internal.ui.preferences.ScrolledPageContent;
 import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
 import org.eclipse.jface.dialogs.Dialog;
 import org.eclipse.jface.dialogs.IDialogSettings;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.events.SelectionListener;
 import org.eclipse.swt.graphics.Font;
 import org.eclipse.swt.graphics.FontMetrics;
 import org.eclipse.swt.graphics.GC;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Combo;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.ui.forms.widgets.ExpandableComposite;
 import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
 import org.jboss.tools.seam.core.SeamCorePlugin;
 import org.jboss.tools.seam.core.SeamPreferences;
 //import org.jboss.tools.seam.ui.xpl.PixelConverter;
 
 /**
  * Find in SeamPreferences the instruction to Framework for Severity preferences
  * To modify section descriptions:
  * 1) If new option is to be added to existing description,
  *    add array of two String objects, where first is the preference name 
  *    defined in SeamPreferences, and second is label defined in 
  *    SeamPreferencesMessages (do not forget put property to SeamPreferencesMessages.properties
  *    and constant to SeamPreferencesMessages.java)
  *    
  * 2) If new section named A is to be created create constant
  *		private static SectionDescription SECTION_A = new SectionDescription(
  *			SeamPreferencesMessages.SeamValidatorConfigurationBlock_section_a,
  *			new String[][]{
  *			}
  *		);
  *    create required constant and property in SeamPreferencesMessages, 
  *    and add SECTION_A to array ALL_SECTIONS.
  * 
  * @author Viacheslav Kabanovich
  */
 public class SeamValidatorConfigurationBlock extends OptionsConfigurationBlock {
 	private static final String SETTINGS_SECTION_NAME = SeamPreferencesMessages.SEAM_VALIDATOR_CONFIGURATION_BLOCK_SEAM_VALIDATOR_CONFIGURATION_BLOCK;
 
 	private Button recognizeVarsCheckBox;
 	private Combo elVariablesCombo;
 	private Combo elPropertiesCombo;
 
 	private static SectionDescription SECTION_COMPONENT = new SectionDescription(
 		SeamPreferencesMessages.SeamValidatorConfigurationBlock_section_component,
 		new String[][]{
 			{SeamPreferences.NONUNIQUE_COMPONENT_NAME, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_nonUniqueComponentName_label},
 			{SeamPreferences.STATEFUL_COMPONENT_DOES_NOT_CONTENT_REMOVE, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_statefulComponentDoesNotContainRemove_label},
 			{SeamPreferences.STATEFUL_COMPONENT_DOES_NOT_CONTENT_DESTROY, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_statefulComponentDoesNotContainDestroy_label},
 			{SeamPreferences.STATEFUL_COMPONENT_WRONG_SCOPE, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_statefulComponentHasWrongScope_label},
 			{SeamPreferences.UNKNOWN_COMPONENT_CLASS_NAME, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_unknownComponentClassName_label},
 			{SeamPreferences.UNKNOWN_COMPONENT_PROPERTY, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_unknownComponentProperty_label}
 		}
 	);
 
 	private static SectionDescription SECTION_ENTITY = new SectionDescription(
 		SeamPreferencesMessages.SeamValidatorConfigurationBlock_section_entities,
 		new String[][]{
 			{SeamPreferences.ENTITY_COMPONENT_WRONG_SCOPE, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_entityComponentHasWrongScope_label}
 		}
 	);
 
 	private static SectionDescription SECTION_LIFECYCLE = new SectionDescription(
 		SeamPreferencesMessages.SeamValidatorConfigurationBlock_section_lifecycle,
 		new String[][]{
 			{SeamPreferences.DUPLICATE_REMOVE, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_duplicateRemove_label},
 			{SeamPreferences.DUPLICATE_DESTROY, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_duplicateDestroy_label},
 			{SeamPreferences.DUPLICATE_CREATE, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_duplicateCreate_label},
 			{SeamPreferences.DUPLICATE_UNWRAP, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_duplicateUnwrap_label},
 			{SeamPreferences.DESTROY_METHOD_BELONGS_TO_STATELESS_SESSION_BEAN, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_destroyMethodBelongsToStatelessSessionBean_label},
 			{SeamPreferences.CREATE_DOESNT_BELONG_TO_COMPONENT, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_createDoesNotBelongToComponent_label},
 			{SeamPreferences.UNWRAP_DOESNT_BELONG_TO_COMPONENT, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_unwrapDoesNotBelongToComponent_label},
 			{SeamPreferences.OBSERVER_DOESNT_BELONG_TO_COMPONENT, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_observerDoesNotBelongToComponent_label},
 		}
 	);
 
 	private static SectionDescription SECTION_FACTORY = new SectionDescription(
 		SeamPreferencesMessages.SeamValidatorConfigurationBlock_section_factory,
 		new String[][]{
 			{SeamPreferences.UNKNOWN_FACTORY_NAME, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_unknownFactoryName_label},
 		}
 	);
 
 	private static SectionDescription SECTION_BIJECTION = new SectionDescription(
 		SeamPreferencesMessages.SeamValidatorConfigurationBlock_section_bijection,
 		new String[][]{
 			{SeamPreferences.MULTIPLE_DATA_BINDER, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_multipleDataBinder_label},
 			{SeamPreferences.UNKNOWN_DATA_MODEL, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_unknownDataModel_label},
 		}
 	);
 
 	private static SectionDescription SECTION_VARIABLE = new SectionDescription(
 		SeamPreferencesMessages.SeamValidatorConfigurationBlock_section_variable,
 		new String[][]{
			{SeamPreferences.DUPLICATE_VARIABLE_NAME, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_duplicateVariableName_label},
 			{SeamPreferences.UNKNOWN_VARIABLE_NAME, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_unknownVariableName_label},
 		}
 	);
 
 	private static SectionDescription SECTION_EL = new SectionDescription(
 		SeamPreferencesMessages.SeamValidatorConfigurationBlock_section_el,
 		new String[][]{
 			{SeamPreferences.EL_SYNTAX_ERROR, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_elSyntaxError_label},
 			{SeamPreferences.UNKNOWN_EL_VARIABLE_NAME, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_unknownElVariableName_label},
 			{SeamPreferences.UNKNOWN_EL_VARIABLE_PROPERTY_NAME, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_unknownElVariablePropertyName_label},
 			{SeamPreferences.UNPAIRED_GETTER_OR_SETTER, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_unpairedGetterOrSetter_label},
 		}
 	);
 
 	private static SectionDescription SECTION_SETTINGS = new SectionDescription(
 		SeamPreferencesMessages.SeamValidatorConfigurationBlock_section_settings,
 		new String[][]{
 			{SeamPreferences.INVALID_PROJECT_SETTINGS, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_invalidSeamProjectSettings_label},
 			{SeamPreferences.INVALID_XML_VERSION, SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_invalidXMLVersion_label}
 		}
 	);
 
 	private static SectionDescription[] ALL_SECTIONS = new SectionDescription[]{
 		SECTION_COMPONENT,
 		SECTION_ENTITY,
 		SECTION_LIFECYCLE, 
 		SECTION_FACTORY,
 		SECTION_BIJECTION, 
 		SECTION_VARIABLE,
 		SECTION_EL,
 		SECTION_SETTINGS
 	};
 
 	private static final String ERROR = SeamPreferences.ERROR;
 	private static final String WARNING = SeamPreferences.WARNING;
 	private static final String IGNORE = SeamPreferences.IGNORE;
 
 	private static final String ENABLED= JavaCore.ENABLED;
 	private static final String DISABLED= JavaCore.DISABLED;
 
 	//private PixelConverter fPixelConverter;
 
 	private static Key[] getKeys() {
 		ArrayList<Key> keys = new ArrayList<Key>();
 		for (int i = 0; i < ALL_SECTIONS.length; i++) {
 			for (int j = 0; j < ALL_SECTIONS[i].options.length; j++) {
 				keys.add(ALL_SECTIONS[i].options[j].key);
 			}
 		}
 		keys.add(getSeamKey(SeamPreferences.CHECK_VARS));
 		return keys.toArray(new Key[0]);
 	}
 
 	public SeamValidatorConfigurationBlock(IStatusChangeListener context,
 			IProject project,
 			IWorkbenchPreferenceContainer container) {
 		super(context, project, getKeys(), container);
 	}
 
 	@Override
 	protected Control createContents(Composite parent) {
 		//fPixelConverter = new PixelConverter(parent);
 		setShell(parent.getShell());
 
 		Composite mainComp = new Composite(parent, SWT.NONE);
 		mainComp.setFont(parent.getFont());
 		GridLayout layout= new GridLayout();
 		layout.marginHeight = 0;
 		layout.marginWidth = 0;
 		mainComp.setLayout(layout);
 
 		Composite commonComposite = createStyleTabContent(mainComp);
 		GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
 		gridData.heightHint = convertHeightInCharsToPixels(parent,20);
 		commonComposite.setLayoutData(gridData);
 
 		validateSettings(null, null, null);
 
 		return mainComp;
 	}
 
 	private int convertHeightInCharsToPixels(Control control,int chars) {
 		Font font = control.getFont();
 		GC gc = new GC(font.getDevice());
 		gc.setFont(font);
 		FontMetrics fFontMetrics = gc.getFontMetrics();
 		gc.dispose();
 		return Dialog.convertHeightInCharsToPixels(fFontMetrics, chars);
 	}
 
 	private Composite createStyleTabContent(Composite folder) {
 		String[] errorWarningIgnore = new String[] {ERROR, WARNING, IGNORE};
 		String[] enableDisableValues= new String[] {ENABLED, DISABLED};
 
 		String[] errorWarningIgnoreLabels = new String[] {
 			SeamPreferencesMessages.SEAM_VALIDATOR_CONFIGURATION_BLOCK_ERROR,  
 			SeamPreferencesMessages.SEAM_VALIDATOR_CONFIGURATION_BLOCK_WARNING, 
 			SeamPreferencesMessages.SEAM_VALIDATOR_CONFIGURATION_BLOCK_IGNORE
 		};
 
 		int nColumns = 3;
 
 		final ScrolledPageContent sc1 = new ScrolledPageContent(folder);
 
 		Composite composite = sc1.getBody();
 		GridLayout layout= new GridLayout(nColumns, false);
 		layout.marginHeight= 0;
 		layout.marginWidth= 0;
 		composite.setLayout(layout);
 
 		Label description= new Label(composite, SWT.LEFT | SWT.WRAP);
 		description.setFont(description.getFont());
 		description.setText(SeamPreferencesMessages.SeamValidatorConfigurationBlock_common_description); 
 		description.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, false, nColumns - 1, 1));
 
 		int defaultIndent = 0;
 
 		for (int i = 0; i < ALL_SECTIONS.length; i++) {
 			SectionDescription section = ALL_SECTIONS[i];
 			String label = section.label; 
 			ExpandableComposite excomposite = createStyleSection(composite, label, nColumns);
 
 			Composite inner = new Composite(excomposite, SWT.NONE);
 			inner.setFont(composite.getFont());
 			inner.setLayout(new GridLayout(nColumns, false));
 			excomposite.setClient(inner);
 
 			for (int j = 0; j < section.options.length; j++) {
 				OptionDescription option = section.options[j];
 				label = option.label;
 				Combo combo = addComboBox(inner, label, option.key, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent);
 				if(option.label == SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_unknownElVariableName_label) {
 					elVariablesCombo = combo;
 					combo.addSelectionListener(new SelectionListener(){
 						public void widgetDefaultSelected(SelectionEvent e) {
 							updateELCombox();
 						}
 						public void widgetSelected(SelectionEvent e) {
 							updateELCombox();
 						}
 					});
 				} else if(option.label == SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_unknownElVariablePropertyName_label) {
 					elPropertiesCombo = combo;
 					combo.addSelectionListener(new SelectionListener(){
 						public void widgetDefaultSelected(SelectionEvent e) {
 							updateELCombox();
 						}
 						public void widgetSelected(SelectionEvent e) {
 							updateELCombox();
 						}
 					});
 				}
 			}
 
 			if(section==SECTION_EL) {
 				label = SeamPreferencesMessages.SeamValidatorConfigurationBlock_pb_checkVars_label; 
 				recognizeVarsCheckBox = addCheckBox(inner, label, getSeamKey(SeamPreferences.CHECK_VARS), enableDisableValues, defaultIndent);
 			}
 		}
 
 		IDialogSettings section = SeamCorePlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION_NAME);
 		restoreSectionExpansionStates(section);
 
 		updateELCombox();
 
 		return sc1;
 	}
 
 	@Override
 	public void performDefaults() {
 		super.performDefaults();
 		updateELCombox();
 	}
 
 	private void updateELCombox() {
 		if(elPropertiesCombo.getSelectionIndex()==2 && elVariablesCombo.getSelectionIndex()==2) {
 			recognizeVarsCheckBox.setEnabled(false);
 		} else {
 			recognizeVarsCheckBox.setEnabled(true);
 		}
 	}
 
 	@Override
 	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
 		String title= SeamPreferencesMessages.SeamValidatorConfigurationBlock_needsbuild_title; 
 		String message;
 		if (workspaceSettings) {
 			message= SeamPreferencesMessages.SeamValidatorConfigurationBlock_needsfullbuild_message; 
 		} else {
 			message= SeamPreferencesMessages.SeamValidatorConfigurationBlock_needsprojectbuild_message; 
 		}
 		return new String[] { title, message };
 	}
 
 	@Override
 	protected void validateSettings(Key changedKey, String oldValue,
 			String newValue) {
 		if (!areSettingsEnabled()) {
 			return;
 		}
 
 		//updateEnableStates();
 
 		fContext.statusChanged(new StatusInfo());		
 	}
 
 	protected static Key getSeamKey(String key) {
 		return getKey(SeamCorePlugin.PLUGIN_ID, key);
 	}	
 
 	static class SectionDescription {
 		String label;
 		OptionDescription[] options;
 
 		public SectionDescription(String label, String[][] optionLabelsAndKeys) {
 			this.label = label;
 			options = new OptionDescription[optionLabelsAndKeys.length];
 			for (int i = 0; i < options.length; i++) {
 				options[i] = new OptionDescription(optionLabelsAndKeys[i][0], optionLabelsAndKeys[i][1]);
 			}
 		}
 	}
 
 	static class OptionDescription {
 		String label;
 		Key key;
 
 		public OptionDescription(String keyName, String label) {
 			this.label = label;
 			key = getSeamKey(keyName);
 		}
 	}
 }
