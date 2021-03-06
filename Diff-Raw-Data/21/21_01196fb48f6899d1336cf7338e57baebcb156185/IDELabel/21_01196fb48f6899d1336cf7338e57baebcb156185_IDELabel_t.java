  /*******************************************************************************
   * Copyright (c) 2007-2009 Red Hat, Inc.
   * Distributed under license by Red Hat, Inc. All rights reserved.
   * This program is made available under the terms of the
   * Eclipse Public License v1.0 which accompanies this distribution,
   * and is available at http://www.eclipse.org/legal/epl-v10.html
   *
   * Contributor:
   *     Red Hat, Inc. - initial API and implementation
   ******************************************************************************/
 package org.jboss.tools.ui.bot.ext.types;
 
 /**
  * Base label constants for all widgets. Naming convention is (except buttons
  * and menus) based on Eclipse platform class names of each part (e.g.
  * NewJavaProjectWizardPageOne)
  * 
  * @author jpeterka 
  */
 public class IDELabel {
 	public class Menu {
 		public static final String FILE = "File";
 		public static final String NEW = "New";
 		public static final String PROJECT = "Project";
 		public static final String OTHER = "Other...";
 		public static final String WINDOW = "Window";
 		public static final String SHOW_VIEW = "Show View";
 		public static final String OPEN_PERSPECTIVE = "Open Perspective";
 		public static final String OPEN_WITH =  "Open With";
 		public static final String TEXT_EDITOR = "Text Editor";
 		public static final String EDIT = "Edit";
 		public static final String SELECT_ALL = "Select All";
 		public static final String CLOSE = "Close";
 		public static final String OPEN = "Open";
 		public static final String RENAME = "Rename";
 		public static final String JSP_FILE = "JSP File";
 		public static final String PROPERTIES = "Properties";
 		public static final String XHTML_FILE = "XHTML File";
 		public static final String HELP = "Help";
 		public static final String ABOUT_JBOSS_DEVELOPER_STUDIO = "About JBoss Developer Studio";
 		public static final String HIBERNATE_CODE_GENERATION = "Hibernate Code Generation...";
 		public static final String HIBERNATE_CODE_GENERATION_CONF = "Hibernate Code Generation Configurations...";
 		public static final String REMOVE = "Remove";
 		public static final String IMPORT = "Import...";
 		public static final String RUN_AS = "Run As";
 		public static final String WEB_PROJECT_JBT_JSF = "JBoss Tools JSF";
 		public static final String PACKAGE_EXPLORER_JBT = "JBoss Tools";
 		public static final String PACKAGE_EXPLORER_CONFIGURE = "Configure";
 		public static final String ADD_JSF_CAPABILITIES = "Add JSF Capabilities...";
 		public static final String CLOSE_PROJECT = "Close Project";
 		public static final String OPEN_PROJECT = "Open Project";
 		public static final String DELETE = "Delete";
 		public static final String JBT_REMOVE_JSF_CAPABILITIES = "Remove JSF Capabilities";
 		public static final String START = "Start";
 		public static final String STOP = "Stop";
 		public static final String STRUTS_PROJECT = "Struts Project";
 		public static final String PREFERENCES = "Preferences";
 		public static final String JBT_REMOVE_STRUTS_CAPABILITIES = "Remove Struts Capabilities";
     public static final String ADD_STRUTS_CAPABILITIES = "Add Struts Capabilities...";
     public static final String WEB_PROJECT_JBT_STRUTS = "JBoss Tools Struts";
     public static final String RUN = "Run";
     public static final String RUN_ON_SERVER = "Run on Server";
     public static final String ADD_AND_REMOVE="Add and Remove...";
     public static final String RUN_AS_JAVA_APPLICATION="Java Application";
     public static final String TOGGLE_BREAKPOINT="Toggle Breakpoint";
     public static final String DEBUG_AS = "Debug As";
     public static final String DEBUG_AS_DROOLS_APPLICATION = "Drools Application";
     public static final String OPEN_GUVNOR_CONSOLE = "Open Guvnor Console";
     public static final String GUVNOR = "Guvnor";
     public static final String GUVNOR_UPDATE = "Update";
     public static final String GUVNOR_COMMIT = "Commit";
     public static final String GUVNOR_ADD = "Add...";
     public static final String GUVNOR_DELETE = "Delete...";
     public static final String GUVNOR_DISCONNECT = "Disconnect";
    public static final String NAVIGATION = "Navigation";
    public static final String MAXIMIZE_ACTIVE_VIEW_OR_EDITOR = "Maximize Active View or Editor";
 	}
 
 	public class Button {
 		public static final String NEXT = "Next >";
 		public static final String BACK = "< Back";
 		public static final String CANCEL = "Cancel";
 		public static final String FINISH = "Finish";
 		public static final String OK = "OK";
 		public static final String YES = "Yes";
 		public static final String NO = "No";
 		public static final String CLOSE = "Close";
 		public static final String RUN = "Run";
 		public static final String APPLY = "Apply";
 		public static final String ADD = "Add...";
 		public static final String NEW = "New...";
 		public static final String CONTINUE = "Continue";
 		public static final String REMOVE = "Remove";
 		public static final String EDIT = "Edit...";
 		public static final String ADD_WITHOUT_DOTS = "Add";
 	}
 
 	public class Shell {
 		public static final String NEW_JAVA_PROJECT = "New Java Project";
 		public static final String NEW_JAVA_CLASS = "New Java Class";
 		// JBT
 		// public static final String NEW_HIBERNATE_MAPPING_FILE = "New Hibernate XML Mapping file (hbm.xml)";
 		// JBDS
 		public static final String NEW_HIBERNATE_MAPPING_FILE = "Create Hibernate XML Mapping file (hbm.xml)";		
 		public static final String NEW = "New";
 		public static final String SAVE_RESOURCE = "Save Resource";
 		public static final String RENAME_RESOURCE = "Rename Resource";
 		public static final String NEW_JSP_FILE = "New File JSP";
 		public static final String PROPERTIES = "Properties";
 		public static final String NEW_XHTML_FILE = "New File XHTML";
 		public static final String IMPORT_JSF_PROJECT = "Import JSF Project";
 		public static final String IMPORT = "Import";
 		public static final String DELETE_SERVER = "Delete Server";
 		public static final String NEW_STRUTS_PROJECT = "New Struts Project";
 	  public static final String PREFERENCES = "Preferences";
 	  public static final String NEW_SERVER_RUNTIME_ENVIRONMENT = "New Server Runtime Environment";
 	  public static final String OPEN_ASSOCIATED_PERSPECTIVE = "Open Associated Perspective?";
 	  public static final String DELETE_RESOURCES = "Delete Resources";
 	  public static final String IMPORT_STRUTS_PROJECT = "Import Struts Project";
 	  public static final String UNSUPPORTED_CONTENT_TYPE = "Unsupported Content Type";
 	  public static final String NEW_SERVER = "New Server";
 	  public static final String RUN_ON_SERVER = "Run On Server";
 	  public static final String WARNING = "Warning";
 	  public static final String DROOLS_RUNTIME = "Drools Runtime";
 	  public static final String NEW_DROOLS_PROJECT = "";
 	  public static final String RENAME_COMPILATION_UNIT = "Rename Compilation Unit";
 	  public static final String RENAME_JAVA_PROJECT = "Rename Java Project";
 	  public static final String CONFIRM_PERSPECTIVE_SWITCH = "Confirm Perspective Switch";
 	  public static final String NEW_SEAM_RUNTIME = "New Seam Runtime";
 	  public static final String NEW_ESB_RUNTIME = "New JBoss ESB Runtime";
 	  public static final String CONFIRM_DELETE = "Confirm Delete";  
 	  public static final String SHOW_VIEW = "Show View";
 	}
 
 	public class EntityGroup {
 		public static final String HIBERNATE = "Hibernate";
 		public static final String JAVA = "Java";
 		public static final String SEAM = "Seam";
 		public static final String STRUTS = "Struts";
 		public static final String JBOSS_TOOLS_WEB = "JBoss Tools Web";
 		public static final String JPA = "JPA";
 		public static final String DROOLS = "Drools";
 		public static final String GUVNOR = "Guvnor";
 	}
 	
 	public class EntityLabel {
 		public static final String HIBERNATE_MAPPING_FILE = "Hibernate XML Mapping file (hbm.xml)";
 		public static final String HIBERNATE_REVERSE_FILE = "Hibernate Reverse Engineering File(reveng.xml)";
 		public static final String HIBERNATE_CONSOLE = "Hibernate Console Configuration";
 		public static final String JAVA_CLASS = "Class";
 		public static final String JAVA_PROJECT =  "Java Project";
 		public static final String SEAM_PROJECT = "Seam Web Project";
 		public static final String HIBERNATE_CONFIGURATION_FILE = "Hibernate Configuration File (cfg.xml)";
 		public static final String STRUTS_PROJECT = "Struts Project";
 		public static final String JPA_PROJECT = "JPA Project";
 		public static final String DROOLS_PROJECT = "Drools Project";
 		public static final String DROOLS_RULE = "Rule Resource";
 		public static final String GUIDED_DROOLS_RULE = "Guided Rule";
 		public static final String DSL_DROOLS_FILE = "Domain Specific Language";
 		public static final String RESOURCES_FROM_GUVNOR = "Resources from Guvnor";
 	}
 
 	public class JavaProjectWizard {
 		public static final String PROJECT_NAME = "Project name:";
 	}
 
 	public class NewClassCreationWizard {
 		public static final String CLASS_NAME = "Name:";
 		public static final String PACKAGE_NAME = "Package:";
 	}
 
 	public class ShowViewDialog {
 		public static final String JAVA_GROUP = "Java";
 		public static final String PROJECT_EXPLORER = "Project Explorer";
 
 	}
 
 	public class View {
 		public static final String WELCOME = "Welcome";
 		public static final String PROJECT_EXPLORER = "Project Explorer";
 		public static final String PACKAGE_EXPLORER = "Package Explorer";
 		public static final String DATA_SOURCE_EXPLORER = "Data Source Explorer";
 		public static final String SERVERS = "Servers";
 		public static final String WEB_PROJECTS = "Web Projects";
 		public static final String PROBLEMS = "Problems";
 		public static final String DEBUG = "Debug";
 		public static final String GUVNOR_REPOSITORIES = "Guvnor Repositories";
 	}
 	
 	public class ViewGroup {
 		public static final String GENERAL = "General";
 		public static final String JAVA = "Java";
 		public static final String DATA_MANAGEMENT = "Data Management";
 		public static final String SERVER = "Server";
 		public static final String JBOSS_TOOLS_WEB = "JBoss Tools Web";
 		public static final String DEBUG = "Debug";
 		public static final String GUVNOR = "Guvnor";
 	}
 
 	public class SelectPerspectiveDialog {
 		public static final String JAVA = "Java";
 		public static final String HIBERNATE = "Hibernate";
 		public static final String SEAM = "Seam";
 		public static final String WEB_DEVELOPMENT = "Web Development";
 		public static final String DB_DEVELOPMENT = "Database Development";
 		public static final String JPA = "JPA";
 		public static final String DEBUG = "Debug";
 		public static final String GUVNOR_REPOSITORY_EXPLORING = "Guvnor Repository Exploring";
 	}
 	/**
 	 * Hibernate Console Wizard (ConsoleConfigurationCreationWizard) Labels (
 	 * @author jpeterka
 	 *
 	 */
 	public class HBConsoleWizard {
 		public static final String MAIN_TAB = "Main";
 		public static final String OPTIONS_TAB = "Options";
 		public static final String CLASSPATH_TAB = "Classpath";
 		public static final String MAPPINGS_TAB = "Mappings";
 		public static final String COMMON_TAB = "Common";
 		public static final String PROJECT_GROUP = "Project:";
 		public static final String CONFIGURATION_FILE_GROUP = "Configuration file:";
 		public static final String SETUP_BUTTON = "Setup...";
 		public static final String CREATE_NEW_BUTTON = "Create new...";
 		public static final String USE_EXISTING_BUTTON = "Use existing...";
 		public static final String DATABASE_DIALECT = "Database dialect:";
 		public static final String DRIVER_CLASS = "Driver class:";
 		public static final String CONNECTION_URL = "Connection URL:";
 		public static final String USERNAME = "Username:";
 		public static final String CREATE_CONSOLE_CONFIGURATION = "Create a console configuration";
 	}
 	
 	public class HBLaunchConfigurationDialog {
 
 		public static final String MAIN_TAB = "Main";
 		public static final String EXPORTERS_TAB = "Exporters";
 		public static final String REFRESH_TAB = "Refresh";
 		public static final String COMMON_TAB = "Common";
 		
 	}
 	
 	public class HBConfigurationWizard {
 		public static final String FILE_NAME = "File name:";
 	}
 
 	public static class RenameResourceDialog {
 
 		public static final String NEW_NAME = "New name:";
 		
 	}
 	
 	public static class WebProjectsTree {
 
 		public static final String WEB_CONTENT = "WebContent";
 		public static final String CONFIGURATION = "Configuration";
 		public static final String WEB_XML = "web.xml";
 		public static final String CONTEXT_PARAMS = "Context Params";
 		public static final String JAVAX_FACES_CONFIG_FILES = "javax.faces.CONFIG_FILES";
 		public static final String DEFAULT = "default";
 		public static final String SERVLETS = "Servlets";
 		public static final String ACTION_STRUTS = "action:org.apache.struts.action.ActionServlet";
 		public static final String CONFIG = "config";
 		public static final String TAG_LIBRARIES = "Tag Libraries";
 		
 	}
 	
 	public static class NewJSPFileDialog {
 
 		public static final String NAME = "Name*";
 		public static final String TEMPLATE = "Template";
 		public static final String TEMPLATE_JSF_BASE_PAGE = "JSFBasePage";
 		
 	}
 	
 	public static class PropertiesDialog {
 
 		public static final String PARAM_VALUE = "Param-Value";
 		
 	}
 
 	public static final class NewXHTMLFileDialog {
 
 		public static final String NAME = "Name*";
 		public static final String TEMPLATE = "Template";
 		public static final String TEMPLATE_FACELET_FORM_XHTML = "FaceletForm.xhtml";
 		
 	}
 	
 	public static final class ServerName {
 
 	  public static final String JBOSS_EAP_4_3_RUNTIME_SERVER = "JBoss EAP 4.3 Runtime Server";
 	  // Server with this Label is created during JBDS installation for bundled EAP
 	  public static final String JBOSS_EAP = "jboss-eap";
 	    
 	}
 	 
    public static final class ServerRuntimeName {
 
      public static final String JBOSS_EAP_4_3 = "JBoss EAP 4.3 Runtime";
      // Server Runtime with this Label is created during JBDS installation for bundled EAP
      public static final String JBOSS_EAP = "jboss-eap Runtime";
      public static final String JBOSS_EAP_5_0 = "JBoss EAP 5.0 Runtime";
 
    }
 	
    public static final class ServerJobName {
 
      public static final String STARTING_JBOSS_EAP_43_RUNTIME = "Starting JBoss EAP 4.3 Runtime Server";
      public static final String STOPPING_JBOSS_EAP_43_RUNTIME = "Stoppig JBoss EAP 4.3 Runtime Server";
      public static final String STARTING_JBOSS_EAP = "Starting jboss-eap";
      public static final String STOPPING_JBOSS_EAP = "Stopping jboss-eap";
      
    }
    
    public static class ImportJSFProjectDialog {
 
      public static final String RUNTIME = "Runtime*";
      public static final String CHOICE_LIST_IS_EMPTY = "Choice list is empty.";
      
    }
    
    public class NewStrutsProjectDialog{
      
      public static final String NAME = "Project Name*";
      public static final String TEMPLATE = "Template*";
      public static final String TEMPLATE_KICK_START = "KickStart";
      
    }
    
    public static class PreferencesDialog {
 
      public static final String SERVER_GROUP = "Server";
      public static final String RUNTIME_ENVIRONMENTS = "Runtime Environments";
      public static final String DROOLS_GROUP = "Drools";
      public static final String INSTALLED_DROOLS_RUNTIMES = "Installed Drools Runtimes";     
      
    }
    
    public static class JBossServerRuntimeDialog {
 
      public static final String NAME = "Name";
      public static final String HOME_DIRECTORY = "Home Directory";
      
    }
    public static final class ServerGroup {
 
      public static final String JBOSS_EAP_4_3 = "JBoss Enterprise Middleware";
      public static final String JBOSS_EAP_5_0 = "JBoss Enterprise Middleware";
        
    }
    public static final class ServerRuntimeType {
 
      public static final String JBOSS_EAP_4_3 = "JBoss Enterprise Application Platform 4.3 Runtime";
      public static final String JBOSS_EAP_5_0 = "JBoss Enterprise Application Platform 5.0 Runtime";
        
    }
    public static final class ServerType {
 
      public static final String JBOSS_EAP_4_3 = "JBoss Enterprise Application Platform 4.3";
      public static final String JBOSS_EAP_5_0 = "JBoss Enterprise Application Platform 5.0";
        
    }
    public static final class DroolsRuntimeDialog {
 
      public static final String NAME = "Name: ";
      public static final String PATH = "Path: ";
      public static final int COLUMN_NAME_INDEX = 0;
      public static final int COLUMN_LOCATION_INDEX = 1;
        
    }
    public static final class NewDroolsProjectDialog {
 
      public static final String NAME = "Project name:";
        
    }
    
    public static final class NewDroolsRuleDialog {
 
      public static final String FILE_NAME = "File name:";
      public static final String RULE_PACKAGE_NAME = "Rule package name:";
        
    }
    
    public static class ProblemsTree {
 
      public static final String WARNINGS = "Warnings";
      public static final String ERRORS = "Errors";
      
    }
    
    public static class ConsoleView {
 
      public static final String BUTTON_CLEAR_CONSOLE_TOOLTIP = "Clear Console";
      
    }
    
    public static class DebugView {
 
      public static final String BUTTON_STEP_OVER_TOOLTIP = "Step Over (F6)";
      public static final String BUTTON_RESUME_TOOLTIP = "Resume (F8)";
      
    }
    
    public static class DroolsEditor {
 
      public static final String TEXT_EDITOR_TAB = "Text Editor";
      public static final String RETE_TREE_TAB = "Rete Tree";
      
    }
    
    public static final class NewGuidedDroolsRuleDialog {
 
      public static final String FILE_NAME = "File name:";
        
    }
    
    public static final class GuidedDroolsRuleEditor {
 
      public static final String WHEN_ADD_DIALOG_TITLE = "Add new condition to the rule";
      public static final String WHEN_ADD_FACT_COMBO = "Fact";
      public static final String ADD_FIELD_TO_THIS_CONDITION_TOOLTIP = "Add a field to this condition, or bind a varible to this fact.";
      public static final String REMOVE_THIS_CONDITION_TOOLTIP = "Remove this condition.";
      public static final String UPDATE_CONSTRAINTS_DIALOG_TITLE = "Update constraints";
      public static final String ADD_RESTRICTION_ON_A_FIELD_COMBO_VALUE = "empty";
      public static final String WHEN_COMBO_CONSTRAINTS_VALUE = "is equal to";
      public static final String CHOOSE_VALUE_EDITOR_TYPE_TOOLTIP = "Choose value editor type";
      public static final String SELECT_VALUE_EDITOR_TYPE_DIALOG_TITLE = "Select value editor type";
      public static final String SELECT_VALUE_EDITOR_TYPE_COMBO_LABEL = "Field value:";
      public static final String SELECT_VALUE_EDITOR_TYPE_COMBO_VALUE = "Literal value";
      public static final String FIELD_VALUE_COMBO_VALUE = "true";
        
    }
    
    public static final class NewDslDroolsFileDialog {
 
      public static final String FILE_NAME = "File name:";
        
    }
    
    public static final class DslDroolsFileEditor {
      
      public static final String ADD_LANGUAGE_MAPPING_DIALOG_TITLE = "New language mapping";
      public static final String LANGUAGE_EXPRESSION_TEXT_LABEL = "Language expression:";
      public static final String RULE_MAPPING_TEXT_LABEL = "Rule mapping:";
      public static final String SCOPE_COMBO_LABEL = "Scope:";
      public static final String SCOPE_COMBO_VALUE = "condition";
      
    }
    
    public static final class GuvnorRepositories {
      
      public static final String ADD_GUVNOR_REPOSITORY_TOOLTIP = "Add a Guvnor respository connection";
      public static final String REMOVE_GUVNOR_REPOSITORY_TOOLTIP = "Delete Guvnor repository connection";
      public static final String REMOVE_GUVNOR_REPOSITORY_DIALOG_TITLE = "Remove repository connection";
      public static final String GUVNOR_REPOSITORY_ROOT_TREE_ITEM = "http://localhost:8080/drools-guvnor/org.drools.guvnor.Guvnor/webdav";
      public static final String PACKAGES_TREE_ITEM = "packages/";
      public static final String MORTGAGE_TREE_ITEM = "mortgages/";
    }
    
    public static final class GuvnorConsole {
      
      public static final String GUVNOR_CONSOLE_TITLE = "JBoss Guvnor";
      public static final String BUTTON_YES_INSTALL_SAMPLES = "Yes, please install samples";
      
    }
  }
