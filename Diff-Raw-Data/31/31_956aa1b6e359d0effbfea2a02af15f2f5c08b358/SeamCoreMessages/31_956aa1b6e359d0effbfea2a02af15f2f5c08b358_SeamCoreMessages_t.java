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
 package org.jboss.tools.seam.core;
 
 import org.eclipse.osgi.util.NLS;
 
 public class SeamCoreMessages {
 	private static final String BUNDLE_NAME = "org.jboss.tools.seam.core.messages"; //$NON-NLS-1$
 
 	static {
 		NLS.initializeMessages(BUNDLE_NAME, SeamCoreMessages.class);
 	}
 
 	public static String SeamCoreBuilder_1;
 	public static String ANT_COPY_UTILS_COPY_FAILED;
 	public static String ANT_COPY_UTILS_COULD_NOT_FIND_FOLDER;
 	public static String ANT_COPY_UTILS_CANNOT_COPY_JDBC_DRIVER_JAR;
 	public static String DATA_SOURCE_XML_DEPLOYER_DEPLOYING_DATASOURCE_TO_SERVER;
 	public static String DATA_SOURCE_XML_DEPLOYER_NO_SERVER_SELECTED_TO_DEPLOY_DATASOURCE_TO;
 	public static String DATA_SOURCE_XML_DEPLOYER_SERVER_DID_NOT_SUPPORT_DEPLOY_OF_DATASOURCE;
 	public static String DATA_SOURCE_XML_DEPLOYER_COULD_NOT_DEPLOY_DATASOURCE;
 	public static String JAVA_SCANNER_CANNOT_GET_COMPILATION_UNIT_FOR;
 	public static String SEAM_EL_VALIDATOR_SETTER;
 	public static String SEAM_EL_VALIDATOR_GETTER;
 	public static String LIBRARY_SCANNER_CANNOT_PROCESS_JAVA_CLASSES;
 	public static String SEAM_VALIDATION_CONTEXT_LINKED_RESOURCE_PATH_MUST_NOT_BE_NULL;
 	public static String SEAM_VALIDATION_CONTEXT_VARIABLE_NAME_MUST_NOT_BE_NULL;
 	public static String SEAM_CORE_VALIDATOR_ERROR_VALIDATING_SEAM_CORE;
 	public static String SEAM_VALIDATION_HELPER_RESOURCE_MUST_NOT_BE_NULL;
 	public static String SEAM_CORE_VALIDATOR_FACTORY_METHOD_MUST_HAVE_NAME; 
 	public static String SEAM_EL_VALIDATOR_ERROR_VALIDATING_SEAM_EL;
 	public static String ERROR_JBOSS_AS_TARGET_SERVER_IS_EMPTY;
 	public static String ERROR_JBOSS_AS_TARGET_SERVER_UNKNOWN;
 	public static String ERROR_JBOSS_AS_TARGET_SERVER_NO_SERVERS_DEFINED;
 	public static String ERROR_JBOSS_AS_TARGET_SERVER_INCOMPATIBLE;
 	public static String ERROR_JBOSS_AS_TARGET_RUNTIME_IS_EMPTY;
 	public static String ERROR_JBOSS_AS_TARGET_RUNTIME_UNKNOWN;
 	public static String SEAM_FACET_INSTALL_ABSTRACT_DELEGATE_ERRORS_OCCURED;
 	public static String SEAM_FACET_INSTALL_ABSTRACT_DELEGATE_CHECK_ERROR_LOG_VIEW;
 	public static String SEAM_FACET_INSTALL_ABSTRACT_DELEGATE_ERROR;
 	public static String SeamFacetAbstractInstallDelegate_Could_not_activate_Hibernate_nature_on_project;
 	public static String SeamFacetAbstractInstallDelegate_Could_not_save_changes_to_preferences;
 	public static String SeamFacetAbstractInstallDelegate_Error;
 	public static String SeamFacetAbstractInstallDelegate_Restrict_raw_XHTML_Documents;
 	public static String RENAME_SEAM_COMPONENT_PROCESSOR_TITLE;
 	public static String RENAME_SEAM_COMPONENT_PROCESSOR_THIS_IS_NOT_A_SEAM_COMPONENT;
 	public static String RENAME_SEAM_CONTEXT_VARIABLE_PROCESSOR_TITLE;
 	public static String RENAME_SEAM_CONTEXT_VARIABLE_PROCESSOR_CAN_NOT_FIND_CONTEXT_VARIABLE;
 	public static String SEAM_RENAME_PROCESSOR_COMPONENT_HAS_DECLARATION_FROM_JAR;
 	public static String SEAM_RENAME_PROCESSOR_OUT_OF_SYNC_FILE;
 	public static String SEAM_RENAME_PROCESSOR_ERROR_PHANTOM_FILE;
 	public static String SEAM_RENAME_PROCESSOR_ERROR_READ_ONLY_FILE;
 	public static String SEAM_RENAME_PROCESSOR_LOCATION_NOT_FOUND;
 	public static String SEAM_RENAME_PROCESSOR_DECLARATION_NOT_FOUND;
 	public static String SEAM_RENAME_PROCESSOR_COMPONENT_HAS_BROKEN_DECLARATION;
 	public static String SEAM_RENAME_METHOD_PARTICIPANT_SETTER_WARNING;
 	public static String SEAM_RENAME_METHOD_PARTICIPANT_GETTER_WARNING;
	public static String SEAM_NATURE_DESCRIPTION;
 }
