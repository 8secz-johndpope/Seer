 /*******************************************************************************
  * Copyright (c) 2004 - 2006 University Of British Columbia and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     University Of British Columbia - initial API and implementation
  *******************************************************************************/
 package org.eclipse.mylar.tasks.ui;
 
 import java.io.File;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.ProjectScope;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.core.runtime.Platform;
 import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
 import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
 import org.eclipse.core.runtime.preferences.IEclipsePreferences;
 import org.eclipse.core.runtime.preferences.IScopeContext;
 import org.eclipse.jface.dialogs.MessageDialog;
 import org.eclipse.jface.dialogs.ProgressMonitorDialog;
 import org.eclipse.jface.preference.IPreferenceStore;
 import org.eclipse.jface.resource.ImageDescriptor;
 import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
 import org.eclipse.mylar.context.core.ContextCorePlugin;
 import org.eclipse.mylar.context.core.MylarStatusHandler;
 import org.eclipse.mylar.internal.context.core.MylarPreferenceContstants;
 import org.eclipse.mylar.internal.tasks.core.TaskDataManager;
 import org.eclipse.mylar.internal.tasks.ui.IDynamicSubMenuContributor;
 import org.eclipse.mylar.internal.tasks.ui.ITaskHighlighter;
 import org.eclipse.mylar.internal.tasks.ui.ITaskListNotification;
 import org.eclipse.mylar.internal.tasks.ui.ITaskListNotificationProvider;
 import org.eclipse.mylar.internal.tasks.ui.ITasksUiConstants;
 import org.eclipse.mylar.internal.tasks.ui.TaskListBackupManager;
 import org.eclipse.mylar.internal.tasks.ui.TaskListColorsAndFonts;
 import org.eclipse.mylar.internal.tasks.ui.TaskListNotificationIncoming;
 import org.eclipse.mylar.internal.tasks.ui.TaskListNotificationManager;
 import org.eclipse.mylar.internal.tasks.ui.TaskListNotificationQueryIncoming;
 import org.eclipse.mylar.internal.tasks.ui.TaskListNotificationReminder;
 import org.eclipse.mylar.internal.tasks.ui.TaskListPreferenceConstants;
 import org.eclipse.mylar.internal.tasks.ui.TaskListSynchronizationScheduler;
 import org.eclipse.mylar.internal.tasks.ui.WorkspaceAwareContextStore;
 import org.eclipse.mylar.internal.tasks.ui.util.TaskListSaveManager;
 import org.eclipse.mylar.internal.tasks.ui.util.TaskListWriter;
 import org.eclipse.mylar.internal.tasks.ui.util.TasksUiExtensionReader;
 import org.eclipse.mylar.internal.tasks.ui.views.TaskListView;
 import org.eclipse.mylar.internal.tasks.ui.views.TaskRepositoriesView;
 import org.eclipse.mylar.tasks.core.AbstractQueryHit;
 import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
 import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
 import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
 import org.eclipse.mylar.tasks.core.DateRangeContainer;
 import org.eclipse.mylar.tasks.core.DelegatingTaskExternalizer;
 import org.eclipse.mylar.tasks.core.ITask;
 import org.eclipse.mylar.tasks.core.ITaskActivityListener;
 import org.eclipse.mylar.tasks.core.ITaskDataHandler;
 import org.eclipse.mylar.tasks.core.ITaskListExternalizer;
 import org.eclipse.mylar.tasks.core.RepositoryTaskAttribute;
 import org.eclipse.mylar.tasks.core.Task;
 import org.eclipse.mylar.tasks.core.TaskComment;
 import org.eclipse.mylar.tasks.core.TaskRepository;
 import org.eclipse.mylar.tasks.core.TaskRepositoryManager;
 import org.eclipse.mylar.tasks.core.AbstractRepositoryTask.RepositoryTaskSyncState;
 import org.eclipse.mylar.tasks.core.web.WebClientUtil;
 import org.eclipse.mylar.tasks.ui.editors.ITaskEditorFactory;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.ui.IStartup;
 import org.eclipse.ui.IWindowListener;
 import org.eclipse.ui.IWorkbench;
 import org.eclipse.ui.IWorkbenchWindow;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.plugin.AbstractUIPlugin;
 import org.osgi.framework.Bundle;
 import org.osgi.framework.BundleContext;
 import org.osgi.service.prefs.BackingStoreException;
 
 /**
  * @author Mik Kersten
  * 
  * TODO: this class is in serious need of refactoring
  */
 public class TasksUiPlugin extends AbstractUIPlugin implements IStartup {
 
 	public static final String LABEL_VIEW_REPOSITORIES = "Task Repositories view";
 
 	public static final String PLUGIN_ID = "org.eclipse.mylar.tasklist";
 	
 	private static final String PROPERTY_PREFIX = "project.repository";
 
 	private static final String PROJECT_REPOSITORY_KIND = PROPERTY_PREFIX + ".kind";
 
 	private static final String PROJECT_REPOSITORY_URL = PROPERTY_PREFIX + ".url";
 
 	private static final String NAME_DATA_DIR = ".mylar";
 
 	private static final char DEFAULT_PATH_SEPARATOR = '/';
 
 	private static final int NOTIFICATION_DELAY = 5000;
 
 	private static TasksUiPlugin INSTANCE;
 
 	private static TaskListManager taskListManager;
 
 	private static TaskRepositoryManager taskRepositoryManager;
 
 	private static TaskListSynchronizationScheduler synchronizationScheduler;
 
 	private static RepositorySynchronizationManager synchronizationManager;
 
 	private static Map<String, AbstractRepositoryConnectorUi> repositoryConnectorUis = new HashMap<String, AbstractRepositoryConnectorUi>();
 
 	private TaskListSaveManager taskListSaveManager;
 
 	private TaskListNotificationManager taskListNotificationManager;
 
 	private TaskListBackupManager taskListBackupManager;
 
 	private TaskDataManager offlineTaskManager;
 
 	private List<ITaskEditorFactory> taskEditors = new ArrayList<ITaskEditorFactory>();
 
 	private ArrayList<IHyperlinkDetector> hyperlinkDetectors = new ArrayList<IHyperlinkDetector>();
 
 	private TaskListWriter taskListWriter;
 
 	private ITaskHighlighter highlighter;
 
 	private static boolean shellActive = true;
 
 	private boolean initialized = false;
 
 	private Map<String, Image> brandingIcons = new HashMap<String, Image>();
 
 	private Map<String, ImageDescriptor> overlayIcons = new HashMap<String, ImageDescriptor>();
 
 	private boolean eclipse_3_3_workbench = false;
 
 	public enum TaskListSaveMode {
 		ONE_HOUR, THREE_HOURS, DAY;
 		@Override
 		public String toString() {
 			switch (this) {
 			case ONE_HOUR:
 				return "1 hour";
 			case THREE_HOURS:
 				return "3 hours";
 			case DAY:
 				return "1 day";
 			default:
 				return "3 hours";
 			}
 		}
 
 		public static TaskListSaveMode fromString(String string) {
 			if (string == null)
 				return null;
 			if (string.equals("1 hour"))
 				return ONE_HOUR;
 			if (string.equals("3 hours"))
 				return THREE_HOURS;
 			if (string.equals("1 day"))
 				return DAY;
 			return null;
 		}
 
 		public static long fromStringToLong(String string) {
 			long hour = 3600 * 1000;
 			switch (fromString(string)) {
 			case ONE_HOUR:
 				return hour;
 			case THREE_HOURS:
 				return hour * 3;
 			case DAY:
 				return hour * 24;
 			default:
 				return hour * 3;
 			}
 		}
 	}
 
 	public enum ReportOpenMode {
 		EDITOR, INTERNAL_BROWSER, EXTERNAL_BROWSER;
 	}
 
 	private static ITaskActivityListener CONTEXT_TASK_ACTIVITY_LISTENER = new ITaskActivityListener() {
 
 		public void taskActivated(ITask task) {
 			ContextCorePlugin.getContextManager().activateContext(task.getHandleIdentifier());
 		}
 
 		public void tasksActivated(List<ITask> tasks) {
 			for (ITask task : tasks) {
 				taskActivated(task);
 			}
 		}
 
 		public void taskDeactivated(ITask task) {
 			ContextCorePlugin.getContextManager().deactivateContext(task.getHandleIdentifier());
 		}
 
 		public void activityChanged(DateRangeContainer week) {
 			// ignore
 		}
 
 		public void taskListRead() {
 			// ignore
 		}
 
 		public void calendarChanged() {
 			// ignore
 		}
 	};
 
 	/**
 	 * TODO: move into reminder mechanisms
 	 */
 	private IWindowListener WINDOW_LISTENER = new IWindowListener() {
 		/**
 		 * bug 1002249: too slow to save state here
 		 */
 		public void windowDeactivated(IWorkbenchWindow window) {
 			shellActive = false;
 		}
 
 		public void windowActivated(IWorkbenchWindow window) {
 			// getDefault().checkTaskListBackup();
 			shellActive = true;
 		}
 
 		public void windowOpened(IWorkbenchWindow window) {
 			// ignore
 		}
 
 		public void windowClosed(IWorkbenchWindow window) {
 			taskListManager.saveTaskList();
 		}
 	};
 
 	private static ITaskListNotificationProvider REMINDER_NOTIFICATION_PROVIDER = new ITaskListNotificationProvider() {
 
 		public Set<ITaskListNotification> getNotifications() {
 			Date currentDate = new Date();
 			Collection<ITask> allTasks = TasksUiPlugin.getTaskListManager().getTaskList().getAllTasks();
 			Set<ITaskListNotification> reminders = new HashSet<ITaskListNotification>();
 			for (ITask task : allTasks) {
 				if (!task.isCompleted() && task.getScheduledForDate() != null && !task.hasBeenReminded()
 						&& task.getScheduledForDate().compareTo(currentDate) < 0) {
 					reminders.add(new TaskListNotificationReminder(task));
 					task.setReminded(true);
 				}
 			}
 			return reminders;
 		}
 	};
 
 	private static ITaskListNotificationProvider INCOMING_NOTIFICATION_PROVIDER = new ITaskListNotificationProvider() {
 
 		public Set<ITaskListNotification> getNotifications() {
 			Set<ITaskListNotification> notifications = new HashSet<ITaskListNotification>();
 			// Incoming Changes
 			for (TaskRepository repository : getRepositoryManager().getAllRepositories()) {
 				for (AbstractRepositoryTask repositoryTask : TasksUiPlugin.getTaskListManager().getTaskList()
 						.getRepositoryTasks(repository.getUrl())) {
 					if (repositoryTask.getSyncState() == RepositoryTaskSyncState.INCOMING
 							&& repositoryTask.isNotified() == false) {
 						TaskListNotificationIncoming notification = new TaskListNotificationIncoming(repositoryTask);
 
 						if (repositoryTask.getTaskData() != null) {
 							List<TaskComment> taskComments = repositoryTask.getTaskData().getComments();
 							if (taskComments != null && taskComments.size() > 0) {
 								TaskComment lastComment = taskComments.get(taskComments.size() - 1);
 								if (lastComment != null) {
 									notification.setDescription(lastComment.getText());
 								}
 							} else {
 								String description = repositoryTask.getTaskData().getDescription();
 								if (description != null) {
 									notification.setDescription(description);
 								}
 							}
 
 							AbstractRepositoryConnector connector = getRepositoryManager().getRepositoryConnector(
 									repositoryTask.getRepositoryKind());
 							if (connector != null) {
 								ITaskDataHandler offlineHandler = connector.getTaskDataHandler();
 								if (offlineHandler != null && repositoryTask.getTaskData().getLastModified() != null) {
 									Date modified = offlineHandler.getDateForAttributeType(
 											RepositoryTaskAttribute.DATE_MODIFIED, repositoryTask.getTaskData()
 													.getLastModified());
 									notification.setDate(modified);
 								}
 							}
 
 						}
 
 						notifications.add(notification);
 						repositoryTask.setNotified(true);
 					}
 				}
 			}
 			// New query hits
 			for (AbstractRepositoryQuery query : TasksUiPlugin.getTaskListManager().getTaskList().getQueries()) {
 				for (AbstractQueryHit hit : query.getHits()) {
 					if (hit.getCorrespondingTask() == null && hit.isNotified() == false) {
 						notifications.add(new TaskListNotificationQueryIncoming(hit));
 						hit.setNotified(true);
 					}
 				}
 			}
 			return notifications;
 		}
 	};
 
 	private final IPropertyChangeListener PREFERENCE_LISTENER = new IPropertyChangeListener() {
 
 		public void propertyChange(PropertyChangeEvent event) {
 			if (event.getProperty().equals(TaskListPreferenceConstants.MULTIPLE_ACTIVE_TASKS)) {
 				TaskListView.getFromActivePerspective().togglePreviousAction(
 						!getPreferenceStore().getBoolean(TaskListPreferenceConstants.MULTIPLE_ACTIVE_TASKS));
 				getTaskListManager().getTaskActivationHistory().clear();
 
 			}
 			// TODO: do we ever get here?
 			if (event.getProperty().equals(MylarPreferenceContstants.PREF_DATA_DIR)) {
 				if (event.getOldValue() instanceof String) {
 					reloadDataDirectory(true);
 				}
 			}
 		}
 
 	};
 
 	private final org.eclipse.jface.util.IPropertyChangeListener PROPERTY_LISTENER = new org.eclipse.jface.util.IPropertyChangeListener() {
 
 		public void propertyChange(org.eclipse.jface.util.PropertyChangeEvent event) {
 			if (event.getProperty().equals(MylarPreferenceContstants.PREF_DATA_DIR)) {
 				if (event.getOldValue() instanceof String) {
 					reloadDataDirectory(true);
 				}
 			}
 		}
 
 	};
 
 	public TasksUiPlugin() {
 		super();
 		INSTANCE = this;
 	}
 
 	@Override
 	public void start(BundleContext context) throws Exception {
 		super.start(context);
 		// NOTE: Startup order is very sensitive
 		try {
 			// TODO: move this back to the extension point, bug 167784
 			ContextCorePlugin.setContextStore(new WorkspaceAwareContextStore());
 			
 			WebClientUtil.initCommonsLoggingSettings();
 			initializeDefaultPreferences(getPreferenceStore());
 			taskListWriter = new TaskListWriter();
 
 			File dataDir = new File(getDataDirectory());
 			dataDir.mkdirs();
 			migrateContextStoreFrom06Format(false);
 			String path = getDataDirectory() + File.separator + ITasksUiConstants.DEFAULT_TASK_LIST_FILE;
 
 			File taskListFile = new File(path);
 			taskListManager = new TaskListManager(taskListWriter, taskListFile);
 			taskRepositoryManager = new TaskRepositoryManager(taskListManager.getTaskList());
 			synchronizationManager = new RepositorySynchronizationManager();
 
 			// NOTE: initializing extensions in start(..) has caused race
 			// conditions previously
 			TasksUiExtensionReader.initStartupExtensions(taskListWriter);
 			readOfflineReports();
 			for (ITaskListExternalizer externalizer : taskListWriter.getExternalizers()) {
 				if (externalizer instanceof DelegatingTaskExternalizer) {
 					((DelegatingTaskExternalizer) externalizer).init(offlineTaskManager);
 				}
 			}
 			taskRepositoryManager.readRepositories(getRepositoriesFilePath());
 			taskListWriter.setTaskDataManager(offlineTaskManager);
 			
 			// NOTE: task list must be read before Task List view can be initialized
 			taskListManager.init();
 			taskListManager.addActivityListener(CONTEXT_TASK_ACTIVITY_LISTENER);
 			taskListManager.readExistingOrCreateNewList();
 			initialized = true;
 			
 			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
 				public void run() {
 					try {												
 						TasksUiExtensionReader.initWorkbenchUiExtensions();
 						PlatformUI.getWorkbench().addWindowListener(WINDOW_LISTENER);
 
 						// Needs to happen asynchronously to avoid bug 159706
 						if (taskListManager.getTaskList().getActiveTask() != null) {
 							taskListManager.activateTask(taskListManager.getTaskList().getActiveTask());
 						}
 						taskListManager.initActivityHistory();
 
 						taskListNotificationManager = new TaskListNotificationManager();
 						taskListNotificationManager.addNotificationProvider(REMINDER_NOTIFICATION_PROVIDER);
 						taskListNotificationManager.addNotificationProvider(INCOMING_NOTIFICATION_PROVIDER);
 						taskListNotificationManager.startNotification(NOTIFICATION_DELAY);
 						getPreferenceStore().addPropertyChangeListener(taskListNotificationManager);
 
 						taskListBackupManager = new TaskListBackupManager();
 						getPreferenceStore().addPropertyChangeListener(taskListBackupManager);
 
 						synchronizationScheduler = new TaskListSynchronizationScheduler(true);
 						synchronizationScheduler.startSynchJob();
 
 						taskListSaveManager = new TaskListSaveManager();
 						taskListManager.setTaskListSaveManager(taskListSaveManager);
 
 						ContextCorePlugin.getDefault().getPluginPreferences().addPropertyChangeListener(
 								PREFERENCE_LISTENER);
 
 						getPreferenceStore().addPropertyChangeListener(PROPERTY_LISTENER);
 						getPreferenceStore().addPropertyChangeListener(synchronizationScheduler);
 						getPreferenceStore().addPropertyChangeListener(taskListManager);
 
 						// TODO: get rid of this, hack to make decorators show up on startup
 						TaskRepositoriesView repositoriesView = TaskRepositoriesView.getFromActivePerspective();
 						if (repositoriesView != null) {
 							repositoriesView.getViewer().refresh();
 						}
 					} catch (Exception e) {
 						MylarStatusHandler.fail(e, "Mylar Tasks UI start failed", false);
 					}
 				}
 			});
 
 			Bundle bundle = Platform.getBundle("org.eclipse.ui.workbench");
 			if (bundle.getLocation().contains("_3.3.")) {
 				eclipse_3_3_workbench = true;
 			}
 		} catch (Exception e) {
 			e.printStackTrace();
 			MylarStatusHandler.fail(e, "Mylar Task List initialization failed", false);
 		}
 	}
  
 	/**
 	 * Will run with every startup in case somebody copies over an old data
 	 * directory. Called upon import. Called upon change of task data directory
 	 * (from preference page).
 	 */
 	private void migrateContextStoreFrom06Format(boolean withProgress) {
 		TaskListDataMigration migrationOperation = new TaskListDataMigration(new File(getDataDirectory()));
 		try {
 			if (!withProgress) {
 				migrationOperation.run(new NullProgressMonitor());
 			} else {
 				IWorkbench wb = PlatformUI.getWorkbench();
 				IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
 				Shell shell = win != null ? win.getShell() : null;
 				new ProgressMonitorDialog(shell).run(false, false, migrationOperation);
 			}
 		} catch (Exception e) {
 			MylarStatusHandler.fail(e, "Error occurred while migrating mylar data", false);
 		}
 	}
 
 	public void earlyStartup() {
 		// ignore
 	}
 
 	@Override
 	public void stop(BundleContext context) throws Exception {
 		super.stop(context);
 		INSTANCE = null;
 		try {
 			if (PlatformUI.isWorkbenchRunning()) {
 				getPreferenceStore().removePropertyChangeListener(taskListNotificationManager);
 				getPreferenceStore().removePropertyChangeListener(taskListBackupManager);
 				getPreferenceStore().removePropertyChangeListener(taskListManager);
 				getPreferenceStore().removePropertyChangeListener(synchronizationScheduler);
 				getPreferenceStore().removePropertyChangeListener(PROPERTY_LISTENER);
 				taskListManager.getTaskList().removeChangeListener(taskListSaveManager);
 				taskListManager.dispose();
 				TaskListColorsAndFonts.dispose();
 				if (ContextCorePlugin.getDefault() != null) {
 					ContextCorePlugin.getDefault().getPluginPreferences().removePropertyChangeListener(
 							PREFERENCE_LISTENER);
 				}
 				PlatformUI.getWorkbench().removeWindowListener(WINDOW_LISTENER);
 			}
 		} catch (Exception e) {
 			MylarStatusHandler.log(e, "Mylar Task List stop terminated abnormally");
 		}
 	}
 
 	public String getDefaultDataDirectory() {
 		return ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + '/' + NAME_DATA_DIR;
 	}
 
 	public String getDataDirectory() {
 		return getPreferenceStore().getString(MylarPreferenceContstants.PREF_DATA_DIR);
 	}
 
 	public void setDataDirectory(String newPath) {
 		getTaskListManager().saveTaskList();
 		// getTaskListSaveManager().saveTaskList(true);
 		// taskListSaveManager.saveTaskList(true, false);
 		ContextCorePlugin.getContextManager().saveActivityHistoryContext();
 		getPreferenceStore().setValue(MylarPreferenceContstants.PREF_DATA_DIR, newPath);
 		ContextCorePlugin.getDefault().getContextStore().notifyContextStoreMoved();
 	}
 
 	/**
 	 * 
 	 * @param withProgress
 	 */
 	public void reloadDataDirectory(boolean withProgress) {
 		// In case new data folder has hold style task data
 		migrateContextStoreFrom06Format(withProgress);
 		getTaskListManager().resetTaskList();
 		getTaskListManager().getTaskActivationHistory().clear();
 		getRepositoryManager().readRepositories(getRepositoriesFilePath());
 		ContextCorePlugin.getContextManager().loadActivityMetaContext();
 		getTaskListManager().setTaskListFile(new File(getDataDirectory() + File.separator + ITasksUiConstants.DEFAULT_TASK_LIST_FILE));
 		getTaskListManager().readExistingOrCreateNewList();
 		getTaskListManager().parseTaskActivityInteractionHistory();
 	}
 
 	@Override
 	protected void initializeDefaultPreferences(IPreferenceStore store) {
 		store.setDefault(MylarPreferenceContstants.PREF_DATA_DIR, getDefaultDataDirectory());
 
 		store.setDefault(TaskListPreferenceConstants.NOTIFICATIONS_ENABLED, true);
 		store.setDefault(TaskListPreferenceConstants.SELECTED_PRIORITY, Task.PriorityLevel.P5.toString());
 		store.setDefault(TaskListPreferenceConstants.REPORT_OPEN_EDITOR, true);
 		store.setDefault(TaskListPreferenceConstants.REPORT_OPEN_INTERNAL, false);
 		store.setDefault(TaskListPreferenceConstants.REPORT_DISABLE_INTERNAL, false);
 		store.setDefault(TaskListPreferenceConstants.ACTIVATE_ON_OPEN, false);
 		store.setDefault(TaskListPreferenceConstants.REPORT_OPEN_EXTERNAL, false);
 		// store.setDefault(TaskListPreferenceConstants.REPOSITORY_SYNCH_ON_STARTUP,
 		// false);
 
 		store.setDefault(TaskListPreferenceConstants.REPOSITORY_SYNCH_SCHEDULE_ENABLED, true);
 		store.setDefault(TaskListPreferenceConstants.REPOSITORY_SYNCH_SCHEDULE_MILISECONDS, "" + (20 * 60 * 1000));
 
 		// store.setDefault(TaskListPreferenceConstants.BACKUP_AUTOMATICALLY,
 		// true);
 		// store.setDefault(TaskListPreferenceConstants.BACKUP_FOLDER,
 		// ContextCorePlugin.getDefault().getDataDirectory()
 		// + DEFAULT_PATH_SEPARATOR + DEFAULT_BACKUP_FOLDER_NAME);
 		store.setDefault(TaskListPreferenceConstants.BACKUP_SCHEDULE, 1);
 		store.setDefault(TaskListPreferenceConstants.BACKUP_MAXFILES, 20);
 		store.setDefault(TaskListPreferenceConstants.BACKUP_LAST, 0f);
 
 		store.setDefault(TaskListPreferenceConstants.FILTER_ARCHIVE_MODE, true);
 		store.setDefault(TaskListPreferenceConstants.MULTIPLE_ACTIVE_TASKS, false);
 		store.setValue(TaskListPreferenceConstants.MULTIPLE_ACTIVE_TASKS, false);
 
 		// store.setDefault(TaskListPreferenceConstants.PLANNING_STARTDAY, 2);
 		// store.setDefault(TaskListPreferenceConstants.PLANNING_ENDDAY, 6);
 		store.setDefault(TaskListPreferenceConstants.PLANNING_STARTHOUR, 9);
 		store.setDefault(TaskListPreferenceConstants.PLANNING_ENDHOUR, 18);
 
 		store.setDefault(TaskListPreferenceConstants.SAVE_TASKLIST_MODE, TaskListSaveMode.THREE_HOURS.toString());
 	}
 
 	public static TaskListManager getTaskListManager() {
 		return taskListManager;
 	}
 
 	public TaskListNotificationManager getTaskListNotificationManager() {
 		return taskListNotificationManager;
 	}
 
 	/**
 	 * Returns the shared instance.
 	 */
 	public static TasksUiPlugin getDefault() {
 		return INSTANCE;
 	}
 
 	// private void checkTaskListBackup() {
 	// Date currentTime = new Date();
 	// if (currentTime.getTime() > lastBackup.getTime() +
 	// AUTOMATIC_BACKUP_SAVE_INTERVAL) {//
 	// TaskListSaveMode.fromStringToLong(getPrefs().getString(SAVE_TASKLIST_MODE)))
 	// // {
 	// TasksUiPlugin.getDefault().getTaskListSaveManager().createTaskListBackupFile();
 	// lastBackup = new Date();
 	// }
 	// }
 
 	private Map<String, List<IDynamicSubMenuContributor>> menuContributors = new HashMap<String, List<IDynamicSubMenuContributor>>();
 
 	public Map<String, List<IDynamicSubMenuContributor>> getDynamicMenuMap() {
 		return menuContributors;
 	}
 
 	public void addDynamicPopupContributor(String menuPath, IDynamicSubMenuContributor contributor) {
 		List<IDynamicSubMenuContributor> contributors = menuContributors.get(menuPath);
 		if (contributors == null) {
 			contributors = new ArrayList<IDynamicSubMenuContributor>();
 			menuContributors.put(menuPath, contributors);
 		}
 		contributors.add(contributor);
 	}
 
 	public boolean isMultipleActiveTasksMode() {
 		return getPreferenceStore().getBoolean(TaskListPreferenceConstants.MULTIPLE_ACTIVE_TASKS);
 	}
 
 	public String[] getSaveOptions() {
 		String[] options = { TaskListSaveMode.ONE_HOUR.toString(), TaskListSaveMode.THREE_HOURS.toString(),
 				TaskListSaveMode.DAY.toString() };
 		return options;
 	}
 
 	public String getBackupFolderPath() {
 		return getDataDirectory() + DEFAULT_PATH_SEPARATOR + ITasksUiConstants.DEFAULT_BACKUP_FOLDER_NAME;
 	}
 
 	public ITaskHighlighter getHighlighter() {
 		return highlighter;
 	}
 
 	public void setHighlighter(ITaskHighlighter highlighter) {
 		this.highlighter = highlighter;
 	}
 
 	public List<ITaskEditorFactory> getTaskEditorFactories() {
 		return taskEditors;
 	}
 
 	public void addContextEditor(ITaskEditorFactory contextEditor) {
 		if (contextEditor != null)
 			this.taskEditors.add(contextEditor);
 	}
 
 	// /**
 	// * Public for testing.
 	// */
 	// public TaskListSaveManager getTaskListSaveManager() {
 	// return taskListSaveManager;
 	// }
 
 	public boolean isShellActive() {
 		return TasksUiPlugin.shellActive;
 	}
 
 	public static TaskRepositoryManager getRepositoryManager() {
 		return taskRepositoryManager;
 	}
 
 	public void addBrandingIcon(String repositoryType, Image icon) {
 		brandingIcons.put(repositoryType, icon);
 	}
 
 	public Image getBrandingIcon(String repositoryType) {
 		return brandingIcons.get(repositoryType);
 	}
 
 	public void addOverlayIcon(String repositoryType, ImageDescriptor icon) {
 		overlayIcons.put(repositoryType, icon);
 	}
 
 	public ImageDescriptor getOverlayIcon(String repositoryType) {
 		return overlayIcons.get(repositoryType);
 	}
 
 	public boolean isInitialized() {
 		return initialized;
 	}
 
 	public IHyperlinkDetector[] getTaskHyperlinkDetectors() {
 		return hyperlinkDetectors.toArray(new IHyperlinkDetector[1]);
 	}
 
 	public void addTaskHyperlinkDetector(IHyperlinkDetector listener) {
 		if (listener != null)
 			this.hyperlinkDetectors.add(listener);
 	}
 
 	public TaskListBackupManager getBackupManager() {
 		return taskListBackupManager;
 	}
 
 	// TODO: clean-up
 	private void readOfflineReports() {
 		IPath offlineReportsPath = getOfflineReportsFilePath();
 
 		try {
 			offlineTaskManager = new TaskDataManager(taskRepositoryManager, offlineReportsPath.toFile(), true);
 		} catch (Throwable t) {
 			MylarStatusHandler
 					.log(t,
 							"Could not restore offline repository tasks file, creating new one, likely cause is format update.");
 			boolean deleted = offlineReportsPath.toFile().delete();
 			if (!deleted) {
 				MylarStatusHandler.log(t, "could not delete offline repository tasks file");
 			}
 			try {
 				offlineTaskManager = new TaskDataManager(taskRepositoryManager, offlineReportsPath.toFile(), false);
 			} catch (Exception e1) {
 				MylarStatusHandler.log(e1, "could not reset offline repository tasks file");
 			}
 		}
 	}
 
 	/**
 	 * Returns the path to the file caching the offline bug reports. PUBLIC FOR
 	 * TESTING
 	 */
 	public IPath getOfflineReportsFilePath() {
 		IPath stateLocation = Platform.getStateLocation(TasksUiPlugin.getDefault().getBundle());
 		IPath configFile = stateLocation.append("offlineReports");
 		return configFile;
 	}
 
 	public TaskDataManager getTaskDataManager() {
 		if (offlineTaskManager == null) {
 			MylarStatusHandler.fail(null, "Offline reports file not created, try restarting.", true);
 		}
 		return offlineTaskManager;
 	}
 
 	public static void addRepositoryConnectorUi(AbstractRepositoryConnectorUi repositoryConnectorUi) {
 		if (!repositoryConnectorUis.values().contains(repositoryConnectorUi)) {
 			repositoryConnectorUis.put(repositoryConnectorUi.getRepositoryType(), repositoryConnectorUi);
 		}
 	}
 
 	public static AbstractRepositoryConnectorUi getRepositoryUi(String kind) {
 		return repositoryConnectorUis.get(kind);
 	}
 
 	public static TaskListSynchronizationScheduler getSynchronizationScheduler() {
 		return synchronizationScheduler;
 	}
 
 	public static RepositorySynchronizationManager getSynchronizationManager() {
 		return synchronizationManager;
 	}
 
 	public String getRepositoriesFilePath() {
 		return getDataDirectory() + File.separator + TaskRepositoryManager.DEFAULT_REPOSITORIES_FILE;
 	}
 
 	/**
 	 * Associate a Task Repository with a workbench project
 	 * 
 	 * @param resource
 	 *            project or resource belonging to a project
 	 * @param repository
 	 *            task repository to associate with given project
 	 * @throws CoreException
 	 */
 	public void setRepositoryForResource(IResource resource, TaskRepository repository) throws CoreException {
 		if (resource == null || repository == null || !resource.getProject().isOpen()) {
 			return;
 		}
 		IProject project = resource.getProject();
 		if (project == null)
 			return;
 		IScopeContext projectScope = new ProjectScope(project);
 		IEclipsePreferences projectNode = projectScope.getNode(PLUGIN_ID);
 		if (projectNode != null) {
 			projectNode.put(PROJECT_REPOSITORY_KIND, repository.getKind());
 			projectNode.put(PROJECT_REPOSITORY_URL, repository.getUrl());
 			try {
 				projectNode.flush();
 			} catch (BackingStoreException e) {
 				MylarStatusHandler.fail(e, "Failed to save task repository to project association preference", false);
 			}
 		}
 	}
 
 	/**
 	 * Retrieve the task repository that has been associated with the given
 	 * project (or resource belonging to a project)
 	 */
 	public TaskRepository getRepositoryForResource(IResource resource, boolean silent) {
 		if (resource == null)
 			return null;
 		IProject project = resource.getProject();
 		if (project == null)
 			return null;
 		TaskRepository taskRepository = null;
 		IScopeContext projectScope = new ProjectScope(project);
 		IEclipsePreferences projectNode = projectScope.getNode(PLUGIN_ID);
 		if (projectNode != null) {
 			String kind = projectNode.get(PROJECT_REPOSITORY_KIND, "");
 			String urlString = projectNode.get(PROJECT_REPOSITORY_URL, "");
 			taskRepository = getRepositoryManager().getRepository(kind, urlString);
 			if (taskRepository == null && !silent) {
 				MessageDialog
 						.openInformation(null, "No Repository Found",
 								"No repository was found. Associate a Task Repository with this project via the project's property page.");
 			}
 		}
 		return taskRepository;
 	}
 
 	public boolean isEclipse_3_3_workbench() {
 		return eclipse_3_3_workbench;
 	}
 
 	/**
 	 * Public for testing.
 	 */
 	public TaskListSaveManager getTaskListSaveManager() {
 		return taskListSaveManager;
 	}
 
 	public String getNextNewRepositoryTaskId() {
 		return getTaskDataManager().getNewRepositoryTaskId();
 	}
 }
