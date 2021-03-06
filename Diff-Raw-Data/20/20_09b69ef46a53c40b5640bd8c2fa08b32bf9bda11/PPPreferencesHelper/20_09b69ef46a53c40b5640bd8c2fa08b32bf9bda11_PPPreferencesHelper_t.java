 /**
  * Copyright (c) 2011 Cloudsmith Inc. and other contributors, as listed below.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *   Cloudsmith
  * 
  */
 package org.cloudsmith.geppetto.pp.dsl.ui.preferences;
 
 import org.cloudsmith.geppetto.pp.dsl.ui.builder.PPBuildJob;
 import org.cloudsmith.geppetto.pp.dsl.ui.pptp.PptpTargetProjectHandler;
 import org.cloudsmith.geppetto.pp.dsl.validation.IValidationAdvisor;
 import org.cloudsmith.geppetto.pp.dsl.validation.ValidationPreference;
 import org.eclipse.core.resources.IWorkspace;
 import org.eclipse.jface.preference.IPreferenceStore;
 import org.eclipse.jface.util.IPropertyChangeListener;
 import org.eclipse.jface.util.PropertyChangeEvent;
 import org.eclipse.xtext.ui.editor.preferences.IPreferenceStoreAccess;
 import org.eclipse.xtext.ui.editor.preferences.IPreferenceStoreInitializer;
 
 import com.google.inject.Inject;
 import com.google.inject.Singleton;
 
 /**
  * A facade that helps with preference checking.
  * (The idea is to not litter the code with specifics about how preferences are stated, where they are stored etc.)
  * 
  */
 @Singleton
 public class PPPreferencesHelper implements IPreferenceStoreInitializer, IPropertyChangeListener {
 
 	private int autoInsertOverrides = 0;
 
 	private final static String OVERRIDE_AUTO_INSERT = "org.cloudsmith.geppetto.override.autoinsert";
 
 	public final static int AUTO_INSERT_BRACKETS = 0x01;
 
 	public final static int AUTO_INSERT_BRACES = 0x02;
 
 	public final static int AUTO_INSERT_PARENTHESES = 0x04;
 
 	public final static int AUTO_INSERT_COMMENT = 0x08;
 
 	public final static int AUTO_INSERT_SQ = 0x10;
 
 	public final static int AUTO_INSERT_DQ = 0x20;
 
 	private IPreferenceStore store;
 
 	@Inject
 	private PptpTargetProjectHandler pptpHandler;
 
 	private static final String defaultProjectPath = "lib/*:environments/$environment/*:manifests/*:modules/*"; //$NON-NLS-1$
 
 	private static final String defaultPuppetEnvironment = "production"; //$NON-NLS-1$
 
 	private static final String defaultForgeURI = "http://forge.puppetlabs.com"; //$NON-NLS-1$
 
 	@Inject
 	IWorkspace workspace;
 
 	public PPPreferencesHelper() {
 		configureAutoInsertOverride();
 	}
 
 	private void configureAutoInsertOverride() {
 		String propValue = System.getProperty(OVERRIDE_AUTO_INSERT, "0");
 
 		try {
 			autoInsertOverrides = Integer.parseInt(propValue);
 		}
 		catch(NumberFormatException e) {
 			autoInsertOverrides = 0;
 		}
 	}
 
 	/**
 	 * @return
 	 */
 	public ValidationPreference getcircularDependencyPreference() {
 		return ValidationPreference.fromString(store.getString(PPPreferenceConstants.PROBLEM_CIRCULAR_DEPENDENCY));
 	}
 
 	public String getForgeURI() {
 		return store.getString(PPPreferenceConstants.FORGE_LOCATION);
 	}
 
 	public ValidationPreference getInterpolatedNonBraceEnclosedHypens() {
 		return ValidationPreference.fromString(store.getString(PPPreferenceConstants.PROBLEM_INTERPOLATED_HYPHEN));
 	}
 
 	public String getPptpVersion() {
 		String result = store.getString(PPPreferenceConstants.PUPPET_TARGET_VERSION);
 		// TODO: Until there is an actual 2.8 pptp, return 2.7
		// PE 2.0 includes puppet 2.7
		if("2.8".equals(result) || "PE 2.0".equals(result))
 			return "2.7";
 		return result;
 	}
 
 	synchronized public IValidationAdvisor.ComplianceLevel getValidationComplianceLevel() {
 		String result = store.getString(PPPreferenceConstants.PUPPET_TARGET_VERSION);
 		if("2.6".equals(result))
 			return IValidationAdvisor.ComplianceLevel.PUPPET_2_6;
 		if("2.8".equals(result))
 			return IValidationAdvisor.ComplianceLevel.PUPPET_2_8;
 
 		// for 2.7 and default
 		return IValidationAdvisor.ComplianceLevel.PUPPET_2_7;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * org.eclipse.xtext.ui.editor.preferences.IPreferenceStoreInitializer#initialize(org.eclipse.xtext.ui.editor.preferences.IPreferenceStoreAccess)
 	 */
 	@Override
 	synchronized public void initialize(IPreferenceStoreAccess access) {
 		// if already initialized
 		if(store != null)
 			return;
 		store = access.getWritablePreferenceStore();
 		store.setDefault(PPPreferenceConstants.AUTO_EDIT_STRATEGY, 0);
 		store.setDefault(PPPreferenceConstants.PUPPET_TARGET_VERSION, "2.7");
 		store.setDefault(PPPreferenceConstants.PUPPET_PROJECT_PATH, defaultProjectPath);
 		store.setDefault(PPPreferenceConstants.PUPPET_ENVIRONMENT, defaultPuppetEnvironment);
 		store.setDefault(PPPreferenceConstants.FORGE_LOCATION, defaultForgeURI);
 
 		store.setDefault(PPPreferenceConstants.PROBLEM_INTERPOLATED_HYPHEN, ValidationPreference.WARNING.toString());
 		store.setDefault(PPPreferenceConstants.PROBLEM_CIRCULAR_DEPENDENCY, ValidationPreference.WARNING.toString());
 
 		autoInsertOverrides = (int) store.getLong(PPPreferenceConstants.AUTO_EDIT_STRATEGY);
 		access.getWritablePreferenceStore().addPropertyChangeListener(this);
 
 	}
 
 	public boolean isAutoBraceInsertWanted() {
 		return (autoInsertOverrides & AUTO_INSERT_BRACES) == 0;
 	}
 
 	public boolean isAutoBracketInsertWanted() {
 		return (autoInsertOverrides & AUTO_INSERT_BRACKETS) == 0;
 	}
 
 	public boolean isAutoDqStringInsertWanted() {
 		return (autoInsertOverrides & AUTO_INSERT_DQ) == 0;
 	}
 
 	public boolean isAutoMLCommentInsertWanted() {
 		return (autoInsertOverrides & AUTO_INSERT_COMMENT) == 0;
 	}
 
 	public boolean isAutoParenthesisInsertWanted() {
 		return (autoInsertOverrides & AUTO_INSERT_PARENTHESES) == 0;
 	}
 
 	public boolean isAutoSqStringInsertWanted() {
 		return (autoInsertOverrides & AUTO_INSERT_SQ) == 0;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
 	 */
 	@Override
 	public void propertyChange(PropertyChangeEvent event) {
 		// System.err.println("Preference changed value: " + event.getProperty());
 		if(PPPreferenceConstants.AUTO_EDIT_STRATEGY.equals(event.getProperty()))
 			autoInsertOverrides = Integer.valueOf((String) event.getNewValue());
 
 		// If pptp changes, recheck the workspace
 		if(PPPreferenceConstants.PUPPET_TARGET_VERSION.equals(event.getProperty())) {
 			pptpHandler.initializePuppetWorkspace();
 			PPBuildJob job = new PPBuildJob(workspace);
 			job.schedule();
 		}
 		if(PPPreferenceConstants.PUPPET_ENVIRONMENT.equals(event.getProperty())) {
 			PPBuildJob job = new PPBuildJob(workspace);
 			job.schedule();
 		}
 		if(PPPreferenceConstants.PUPPET_PROJECT_PATH.equals(event.getProperty())) {
 			PPBuildJob job = new PPBuildJob(workspace);
 			job.schedule();
 		}
 		if(PPPreferenceConstants.PROBLEM_INTERPOLATED_HYPHEN.equals(event.getProperty())) {
 			PPBuildJob job = new PPBuildJob(workspace);
 			job.schedule();
 		}
 		if(PPPreferenceConstants.PROBLEM_CIRCULAR_DEPENDENCY.equals(event.getProperty())) {
 			PPBuildJob job = new PPBuildJob(workspace);
 			job.schedule();
 		}
 	}
 }
