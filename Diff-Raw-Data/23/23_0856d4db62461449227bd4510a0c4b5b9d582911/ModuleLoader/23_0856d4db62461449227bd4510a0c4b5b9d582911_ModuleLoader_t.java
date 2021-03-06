 /*
  * (c) Copyright 2010-2011 AgileBirds
  *
  * This file is part of OpenFlexo.
  *
  * OpenFlexo is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * OpenFlexo is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with OpenFlexo. If not, see <http://www.gnu.org/licenses/>.
  *
  */
 package org.openflexo.module;
 
 import java.awt.Frame;
 import java.awt.Window;
 import java.io.File;
 import java.util.ArrayList;
 import java.util.Enumeration;
 import java.util.Hashtable;
 import java.util.List;
 import java.util.Locale;
 import java.util.Vector;
 import java.util.concurrent.Callable;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import javax.swing.JOptionPane;
 import javax.swing.SwingUtilities;
 import javax.swing.UIManager;
 import javax.swing.UnsupportedLookAndFeelException;
 
 import org.openflexo.ApplicationData;
 import org.openflexo.GeneralPreferences;
 import org.openflexo.action.SubmitDocumentationAction;
 import org.openflexo.ch.FCH;
 import org.openflexo.components.NewProjectComponent;
 import org.openflexo.components.OpenProjectComponent;
 import org.openflexo.components.ProgressWindow;
 import org.openflexo.components.SaveDialog;
 import org.openflexo.drm.DocResourceManager;
 import org.openflexo.foundation.FlexoEditor;
 import org.openflexo.foundation.rm.FlexoProject;
 import org.openflexo.foundation.rm.SaveResourceException;
 import org.openflexo.foundation.utils.ProjectExitingCancelledException;
 import org.openflexo.foundation.utils.ProjectInitializerException;
 import org.openflexo.foundation.utils.ProjectLoadingCancelledException;
 import org.openflexo.help.FlexoHelp;
 import org.openflexo.localization.FlexoLocalization;
 import org.openflexo.localization.Language;
 import org.openflexo.module.external.ExternalCEDModule;
 import org.openflexo.module.external.ExternalDMModule;
 import org.openflexo.module.external.ExternalIEModule;
 import org.openflexo.module.external.ExternalModuleDelegater;
 import org.openflexo.module.external.ExternalOEModule;
 import org.openflexo.module.external.ExternalWKFModule;
 import org.openflexo.module.external.IModule;
 import org.openflexo.module.external.IModuleLoader;
 import org.openflexo.prefs.FlexoPreferences;
 import org.openflexo.swing.FlexoSwingUtils;
 import org.openflexo.view.ModuleBar;
 import org.openflexo.view.controller.FlexoController;
 import org.openflexo.view.menu.ToolsMenu;
 import org.openflexo.view.menu.WindowMenu;
 
 /**
  * This class handles computation of available modules and modules loading. Only one instance of this class is instancied, and available all
  * over Flexo Application Suite. This is the ONLY ONE WAY to access external modules from a given module.
  * 
  * @author sguerin
  */
 public final class ModuleLoader implements IModuleLoader {
 
 	private static final Logger logger = Logger.getLogger(ModuleLoader.class.getPackage().getName());
 
 	private static volatile ModuleLoader _instance = null;
 
 	private boolean allowsDocSubmission;
 
 	private Module switchingToModule = null;
 
 	private Module activeModule = null;
 
 	/**
 	 * Hashtable where are stored Module instances (instance of FlexoModule associated to a Module instance key.
 	 */
 	private Hashtable<Module, FlexoModule> _modules = new Hashtable<Module, FlexoModule>();
 
 	/**
 	 * Vector of Module instance representing all available modules
 	 */
 	private ArrayList<Module> _availableModules = new ArrayList<Module>();
 
 	private ModuleLoader() {
 		super();
 		if (!UserType.isCurrentUserTypeDefined()) {
 			throw new IllegalStateException("Cannot initialize ModuleLoader if UserType is not defined. "
 					+ "Please call UserType.setCurrentUserType(<UserType>) before" + " initializing the ModuleLoader.");
 		}
 		ExternalModuleDelegater.registerModuleLoader(this);
 		initialize();
 	}
 
 	private static final Object monitor = new Object();
 
 	public static ModuleLoader instance() {
 		if (_instance == null) {
 			synchronized (monitor) {
 				if (_instance == null) {
 					_instance = new ModuleLoader();
 				}
 			}
 		}
 		return _instance;
 	}
 
 	public void reloadProject() throws ProjectLoadingCancelledException, ModuleLoadingException, ProjectInitializerException {
 		if (getProject() == null) {
 			if (logger.isLoggable(Level.WARNING)) {
 				logger.warning("No project currently loaded. Returning.");
 			}
 			return;
 		}
 		openProject(getProject().getProjectDirectory(), null);
 	}
 
 	public void newProject(File projectDirectory, Module module) throws ProjectLoadingCancelledException, ModuleLoadingException {
 		if (getProject() == null || getProjectLoader().saveProject(getProject(), true)) {
 			if (projectDirectory == null) {
 				projectDirectory = NewProjectComponent.getProjectDirectory();
 			}
 			FlexoProject project = getProjectLoader().newProject(projectDirectory).getProject();
 			loadModuleWithProject(module, project);
 		}
 	}
 
 	public void openProject(File projectDirectory, Module module) throws ProjectLoadingCancelledException, ModuleLoadingException,
 			ProjectInitializerException {
 		if (getProject() == null || getProjectLoader().saveProject(getProject(), true)) {
 			if (projectDirectory == null) {
 				projectDirectory = OpenProjectComponent.getProjectDirectory();
 			}
 			FlexoEditor editor = getProjectLoader().loadProject(projectDirectory);
 			FlexoProject project = editor.getProject();
 			loadModuleWithProject(module, project);
 		}
 	}
 
 	private void loadModuleWithProject(Module module, FlexoProject project) throws ModuleLoadingException, ProjectLoadingCancelledException {
 		if (module == null) {
 			if (activeModule != null) {
 				module = activeModule;
 			} else {
 				module = new ApplicationData().getFavoriteModule();
 			}
 		}
 		ProjectLoader.instance().closeCurrentProject();
 		for (FlexoModule fm : new ArrayList<FlexoModule>(_modules.values())) {
 			fm.closeWithoutConfirmation(false);
 		}
 		switchToModule(module, project);
 		GeneralPreferences.addToLastOpenedProjects(project.getProjectDirectory());
 	}
 
 	private ProjectLoader getProjectLoader() {
 		return ProjectLoader.instance();
 	}
 
 	/**
 	 * @param moduleClass
 	 *            a module implementation class
 	 * @return the Module definition for the given moduleImplementation class or null if the Module is not available for the currentUserType
 	 */
 	public Module getModule(Class<? extends FlexoModule> moduleClass) {
 		for (Module candidate : UserType.getCurrentUserType().getModules()) {
 			if (moduleClass.equals(candidate.getModuleClass())) {
 				return candidate;
 			}
 		}
 		if (logger.isLoggable(Level.WARNING)) {
 			logger.warning("Module for " + moduleClass.getName() + " is not available in "
 					+ UserType.getCurrentUserType().getBusinessName2());
 		}
 		return null;
 	}
 
 	/**
 	 * @param moduleName
 	 *            the name of a module
 	 * @return the Module definition for the given moduleName or null if the Module is not available for the currentUserType or the module
 	 *         name is unknown.
 	 * @see #isAvailable(Module)
 	 * @see UserType
 	 */
 	public Module getModule(String moduleName) {
 		for (Module candidate : UserType.getCurrentUserType().getModules()) {
 			if (candidate.getName().equals(moduleName)) {
 				return candidate.isAvailable() ? candidate : null;
 			}
 		}
 		if (logger.isLoggable(Level.WARNING)) {
 			logger.warning("Module named " + moduleName + " is either unknown, either not available in "
 					+ UserType.getCurrentUserType().getBusinessName2());
 		}
 		return null;
 	}
 
 	public boolean allowsDocSubmission() {
 		return allowsDocSubmission;
 	}
 
 	public void setAllowsDocSubmission(boolean allowsDocSubmission) {
 		this.allowsDocSubmission = allowsDocSubmission;
 		SubmitDocumentationAction.actionType.setAllowsDocSubmission(allowsDocSubmission);
 	}
 
 	@Override
 	public IModule getActiveModule() {
 		if (FlexoModule.getActiveModule() != null) {
 			return FlexoModule.getActiveModule().getModule();
 		} else {
 			return null;
 		}
 	}
 
 	/**
 	 * Given a list of modules, check if user type has right to use them and register them consequently
 	 * 
 	 */
 	private void initialize() {
 		for (Module module : UserType.getCurrentUserType().getModules()) {
 			if (module.getModuleClass() != null) {
 				registerModule(module);
 			}
 		}
 		if (GeneralPreferences.getLanguage() != null && GeneralPreferences.getLanguage().equals(Language.FRENCH)) {
 			Locale.setDefault(Locale.FRANCE);
 		} else {
 			Locale.setDefault(Locale.US);
 		}
 		FlexoHelp.configure(GeneralPreferences.getLanguage() != null ? GeneralPreferences.getLanguage().getIdentifier() : "ENGLISH",
 				UserType.getCurrentUserType().getIdentifier());
 	}
 
 	/**
 	 * Internally used to register module with class name moduleClass
 	 * 
 	 * @param module
 	 *            the module to register
 	 */
 	private void registerModule(Module module) {
 		if (module.register()) {
 			_availableModules.add(module);
 		}
 	}
 
 	/**
 	 * Return all loaded modules as an Enumeration of FlexoModule instances
 	 * 
 	 * @return Enumeration
 	 */
 	public Enumeration<FlexoModule> loadedModules() {
 		return new Vector<FlexoModule>(_modules.values()).elements();
 	}
 
 	/**
 	 * Return all unloaded modules but available modules as a Vector of Module instances
 	 * 
 	 * @return Vector
 	 */
 	public List<Module> unloadedButAvailableModules() {
 		ArrayList<Module> returned = new ArrayList<Module>();
 		returned.addAll(_availableModules);
 		for (Enumeration<FlexoModule> e = loadedModules(); e.hasMoreElements();) {
 			returned.remove(e.nextElement().getModule());
 		}
 		return returned;
 	}
 
 	/**
 	 * @param module
 	 *            module to ignore while checking.
 	 * @return if there is still a loaded module requiring project (ignoring moduleToIgnore)
 	 */
 	public boolean isThereAnyLoadedModuleWithAProjectExcept(Module module) {
 		for (Module m : _modules.keySet()) {
 			if (!m.equals(module)) {
 				if (m.requireProject()) {
 					return true;
 				}
 			}
 		}
 		return false;
 	}
 
 	/**
 	 * Return all loaded modules as a Vector of Module instances
 	 * 
 	 * @return Vector
 	 */
 	public List<Module> availableModules() {
 		return _availableModules;
 	}
 
 	public void unloadModule(Module module) {
 		if (isLoaded(module)) {
 			if (logger.isLoggable(Level.INFO)) {
 				logger.info("Unloading module " + module.getName());
 			}
 			_modules.remove(module);
 			WindowMenu.notifyModuleUnloaded(module);
 			ModuleBar.notifyStaticallyModuleHasBeenUnLoaded(module);
 		} else {
 			if (logger.isLoggable(Level.WARNING)) {
 				logger.warning("Unable to unload unloaded module " + module.getName());
 			}
 		}
 		if (activeModule == module) {
 			activeModule = null;
 		}
 	}
 
 	private FlexoModule loadModule(Module module, FlexoProject project) throws Exception {
 		boolean createProgress = !ProgressWindow.hasInstance();
 		if (createProgress) {
 			ProgressWindow.showProgressWindow(FlexoLocalization.localizedForKey("loading_module") + " " + module.getLocalizedName(), 8);
 		}
 		ProgressWindow.setProgressInstance(FlexoLocalization.localizedForKey("loading_module") + " " + module.getLocalizedName());
 		FlexoModule returned = doInternalLoadModule(module, project);
 		_modules.put(module, returned);
 		activeModule = module;
 		if (createProgress) {
 			ProgressWindow.hideProgressWindow();
 		}
 		WindowMenu.notifyModuleLoaded(module);
 		ModuleBar.notifyStaticallyModuleHasBeenLoaded(module);
 		return returned;
 	}
 
 	private class ModuleLoaderCallable implements Callable<FlexoModule> {
 
 		private final Module module;
 		private final FlexoProject project;
 
 		public ModuleLoaderCallable(Module module, FlexoProject project) {
 			this.module = module;
 			// TODO Auto-generated constructor stub
 			this.project = project;
 		}
 
 		@Override
 		public FlexoModule call() throws Exception {
 			if (logger.isLoggable(Level.INFO)) {
 				logger.info("Loading module " + module.getName());
 			}
 			Object[] params;
 			if (module.requireProject()) {
 				params = new Object[1];
 				params[0] = project.getResourceManagerInstance().getEditor();
 			} else {
 				params = new Object[0];
 			}
 			FlexoModule flexoModule = (FlexoModule) module.getConstructor().newInstance(params);
 			FCH.ensureHelpEntryForModuleHaveBeenCreated(flexoModule);
 			return flexoModule;
 		}
 
 	}
 
 	private FlexoModule doInternalLoadModule(Module module, FlexoProject project) throws Exception {
 		ModuleLoaderCallable loader = new ModuleLoaderCallable(module, project);
 		return FlexoSwingUtils.syncRunInEDT(loader);
 	}
 
 	public boolean isAvailable(Module module) {
 		return _availableModules.contains(module);
 	}
 
 	public boolean isLoaded(Module module) {
 		return _modules.get(module) != null;
 	}
 
 	public boolean isActive(Module module) {
 		return getActiveModule() == module;
 	}
 
 	public FlexoModule getModuleInstance(Module module, FlexoProject project) throws ModuleLoadingException {
 		if (module == null) {
 			if (logger.isLoggable(Level.WARNING)) {
 				logger.warning("Trying to get module instance for module null");
 			}
 			return null;
 		}
 		if (isAvailable(module)) {
 			if (isLoaded(module)) {
 				FlexoModule flexoModule = _modules.get(module);
 				if (module.requireProject() && flexoModule.getProject() != project) {
 					throw new IllegalStateException(
 							"Cannot currently switch project for a loaded module. Close the module first and then load it with the chosen project.");
 				}
 				return flexoModule;
 			} else {
 				if (module.requireProject() && project == null) {
 					throw new IllegalArgumentException("Module " + module.getName() + " needs a project. project cannot be null");
 				}
 				try {
 					return loadModule(module, project);
 				} catch (Exception e) {
 					e.printStackTrace();
 					throw new ModuleLoadingException(module);
 				}
 			}
 		} else {
 			if (logger.isLoggable(Level.WARNING)) {
 				logger.warning("Sorry, module " + module.getName() + " not available.");
 			}
 			return null;
 		}
 	}
 
 	public ExternalWKFModule getWKFModule(FlexoProject project) throws ModuleLoadingException {
 		return (ExternalWKFModule) getModuleInstance(Module.WKF_MODULE, project);
 	}
 
 	public ExternalIEModule getIEModule(FlexoProject project) throws ModuleLoadingException {
 		return (ExternalIEModule) getModuleInstance(Module.IE_MODULE, project);
 	}
 
 	public ExternalDMModule getDMModule(FlexoProject project) throws ModuleLoadingException {
 		return (ExternalDMModule) getModuleInstance(Module.DM_MODULE, project);
 	}
 
 	public ExternalCEDModule getCEDModule(FlexoProject project) throws ModuleLoadingException {
 		return (ExternalCEDModule) getModuleInstance(Module.VPM_MODULE, project);
 	}
 
 	public ExternalOEModule getOEModule(FlexoProject project) throws ModuleLoadingException {
 		return (ExternalOEModule) getModuleInstance(Module.VE_MODULE, project);
 	}
 
 	private synchronized void setSwitchingTo(Module module) {
 		switchingToModule = module;
 	}
 
 	public FlexoModule switchToModule(Module module, FlexoProject project) throws ModuleLoadingException {
 		logger.info("switchToModule: " + module);
 		// todo getting rid of this switchingToModule hack !
 		if (switchingToModule != null) {
 			return null;
 		}
 		if (activeModule == module) {
 			return _modules.get(module);
 		}
 		setSwitchingTo(module);
 		try {
 			if (logger.isLoggable(Level.INFO)) {
 				logger.info("Switch to module " + module.getName());
 			}
 			FlexoModule moduleInstance = getModuleInstance(module, project);
 			if (moduleInstance != null) {
 				activeModule = module;
 				moduleInstance.focusOn();
 				return moduleInstance;
 			}
 			throw new ModuleLoadingException(module);
 		} finally {
 			setSwitchingTo(null);
 		}
 	}
 
 	public void activateModule(Module m) {
 		if (activeModule != m) {
 			_modules.get(m).processFocusOn();
 			activeModule = m;
 		}
 	}
 
 	/**
 	 * Called for quitting. Ask if saving must be performed, and exit on request.
 	 * 
 	 * @param askConfirmation
 	 *            if flexo must ask confirmation to the user
 	 * @throws ProjectExitingCancelledException
 	 *             whenever user decide to not quit
 	 */
 	public void quit(boolean askConfirmation) throws ProjectExitingCancelledException {
 		if (askConfirmation) {
 			proceedQuit();
 		} else {
 			proceedQuitWithoutConfirmation();
 		}
 	}
 
 	private void proceedQuit() throws ProjectExitingCancelledException {
 		if (logger.isLoggable(Level.INFO)) {
 			logger.info("Exiting FLEXO Application Suite...");
 		}
 		FlexoProject currentProject = findCurrentProject();
 		if (ProjectLoader.someResourcesNeedsSaving(currentProject)) {
 			SaveDialog reviewer = new SaveDialog(FlexoController.getActiveFrame());
 			if (reviewer.getRetval() == JOptionPane.YES_OPTION) {
 				try {
 					ProjectLoader.doSaveProject(currentProject);
 					proceedQuitWithoutConfirmation();
 				} catch (SaveResourceException e) {
 					e.printStackTrace();
 					if (FlexoController.confirm(FlexoLocalization.localizedForKey("error_during_saving") + "\n"
 							+ FlexoLocalization.localizedForKey("would_you_like_to_exit_anyway"))) {
 						proceedQuitWithoutConfirmation();
 					}
 				}
 			} else if (reviewer.getRetval() == JOptionPane.NO_OPTION) {
 				proceedQuitWithoutConfirmation();
 			} else { // CANCEL
 				if (logger.isLoggable(Level.INFO)) {
 					logger.info("Exiting FLEXO Application Suite... CANCELLED");
 				}
 				throw new ProjectExitingCancelledException();
 			}
 		} else {
 			if (FlexoController.confirm(FlexoLocalization.localizedForKey("really_quit"))) {
 				proceedQuitWithoutConfirmation();
 			}
 		}
 	}
 
 	private void proceedQuitWithoutConfirmation() {
 		if (activeModule != null) {
 			GeneralPreferences.setFavoriteModuleName(activeModule.getName());
 			FlexoPreferences.savePreferences(true);
 		}
 
 		for (Enumeration<FlexoModule> en = loadedModules(); en.hasMoreElements();) {
 			en.nextElement().moduleWillClose();
 		}
 		if (allowsDocSubmission() && !isAvailable(Module.DRE_MODULE) && DocResourceManager.instance().getSessionSubmissions().size() > 0) {
 			if (FlexoController.confirm(FlexoLocalization.localizedForKey("you_have_submitted_documentation_without_having_saved_report")
 					+ "\n" + FlexoLocalization.localizedForKey("would_you_like_to_save_your_submissions"))) {
 				new ToolsMenu.SaveDocSubmissionAction().actionPerformed(null);
 			}
 		}
 		if (isAvailable(Module.DRE_MODULE)) {
 			if (DocResourceManager.instance().needSaving()) {
 				if (FlexoController.confirm(FlexoLocalization.localizedForKey("documentation_resource_center_not_saved") + "\n"
 						+ FlexoLocalization.localizedForKey("would_you_like_to_save_documenation_resource_center"))) {
 					DocResourceManager.instance().save();
 				}
 			}
 		}
 		if (logger.isLoggable(Level.INFO)) {
 			logger.info("Exiting FLEXO Application Suite... DONE");
 		}
 		System.exit(0);
 	}
 
 	/**
 	 * Loop over loaded modules and ask to the controller of each module if a project is loaded. Usage of this method is not recommended
 	 * since it won't be available in a further release supporting multiple projects loaded at the same time.
 	 * 
 	 * @return the project currently loaded or null whenever there is no loaded project.
 	 * @throws IllegalStateException
 	 *             whenever 2 distinct modules have 2 distinct projects loaded.
 	 */
 	public FlexoProject getProject() {
 		return findCurrentProject();
 	}
 
 	private FlexoProject findCurrentProject() {
 		FlexoProject project = null;
 
 		for (FlexoModule flexoModule : _modules.values()) {
 			if (flexoModule.getModule().requireProject()) {
 				FlexoProject moduleProject = flexoModule.getProject();
 				if (project == null) {
 					project = moduleProject;
 				} else {
 					if (!project.equals(moduleProject)) {
 						throw new IllegalStateException("Found distinct projects in modules.");
 					}
 				}
 			}
 		}
 		return project;
 	}
 
 	@Override
 	public ExternalIEModule getIEModuleInstance(FlexoProject project) throws ModuleLoadingException {
 		return getIEModule(project);
 	}
 
 	@Override
 	public ExternalDMModule getDMModuleInstance(FlexoProject project) throws ModuleLoadingException {
 		return getDMModule(project);
 	}
 
 	@Override
 	public ExternalWKFModule getWKFModuleInstance(FlexoProject project) throws ModuleLoadingException {
 		return getWKFModule(project);
 	}
 
 	@Override
 	public ExternalCEDModule getCEDModuleInstance(FlexoProject project) throws ModuleLoadingException {
 		return getCEDModule(project);
 	}
 
 	@Override
 	public ExternalOEModule getOEModuleInstance(FlexoProject project) throws ModuleLoadingException {
 		return getOEModule(project);
 	}
 
 	/**
 	 * @param value
 	 *            the look and feel identifier
 	 * @throws ClassNotFoundException
 	 *             - if the LookAndFeel class could not be found
 	 * @throws UnsupportedLookAndFeelException
 	 *             - if lnf.isSupportedLookAndFeel() is false
 	 * @throws IllegalAccessException
 	 *             - if the class or initializer isn't accessible
 	 * @throws InstantiationException
 	 *             - if a new instance of the class couldn't be created
 	 * @throws ClassCastException
 	 *             - if className does not identify a class that extends LookAndFeel
 	 */
 	// todo move this method somewhere else
 	public static void setLookAndFeel(String value) throws ClassNotFoundException, InstantiationException, IllegalAccessException,
 			UnsupportedLookAndFeelException {
 		if (UIManager.getLookAndFeel().getClass().getName().equals(value)) {
 			return;
 		}
 		UIManager.setLookAndFeel(value);
 		for (Frame frame : Frame.getFrames()) {
 			for (Window window : frame.getOwnedWindows()) {
 				SwingUtilities.updateComponentTreeUI(window);
 			}
 			SwingUtilities.updateComponentTreeUI(frame);
 		}
 	}
 
 	@Override
 	public boolean isWKFLoaded() {
 		return isLoaded(Module.WKF_MODULE);
 	}
 
	public void closeAllModulesWithoutConfirmation() {
		for (FlexoModule module : new ArrayList<FlexoModule>(_modules.values())) {
			module.closeWithoutConfirmation(false);
		}
	}

 }
