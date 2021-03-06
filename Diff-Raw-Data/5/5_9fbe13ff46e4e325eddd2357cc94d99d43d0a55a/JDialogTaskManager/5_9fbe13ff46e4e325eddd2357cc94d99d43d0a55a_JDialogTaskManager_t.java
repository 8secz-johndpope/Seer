 package org.cytoscape.work.internal.task;
 
 
 import java.awt.Window;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 import java.util.concurrent.Future;
 import java.util.concurrent.ScheduledExecutorService;
 import java.util.concurrent.ThreadFactory;
 import java.util.concurrent.TimeUnit;
 
 import javax.swing.JDialog;
 import javax.swing.SwingUtilities;
 
 import org.cytoscape.work.AbstractTaskManager;
 import org.cytoscape.work.Task;
 import org.cytoscape.work.TaskFactory;
 import org.cytoscape.work.TaskIterator;
 import org.cytoscape.work.TunableRecorder;
 import org.cytoscape.work.internal.tunables.JDialogTunableMutator;
 import org.cytoscape.work.swing.DialogTaskManager;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * Uses Swing components to create a user interface for the <code>Task</code>.
  *
  * This will not work if the application is running in headless mode.
  */
 public class JDialogTaskManager extends AbstractTaskManager<JDialog,Window> implements DialogTaskManager {
 
 	private static final Logger logger = LoggerFactory.getLogger(JDialogTaskManager.class);
 
 	/**
 	 * The delay between the execution of the <code>Task</code> and
 	 * showing its task dialog.
 	 *
 	 * When a <code>Task</code> is executed, <code>JDialogTaskManager</code>
 	 * will not show its task dialog immediately. It will delay for a
 	 * period of time before showing the dialog. This way, short lived
 	 * <code>Task</code>s won't have a dialog box.
 	 */
 	static final long DELAY_BEFORE_SHOWING_DIALOG = 1;
 
 	/**
 	 * The time unit of <code>DELAY_BEFORE_SHOWING_DIALOG</code>.
 	 */
 	static final TimeUnit DELAY_TIMEUNIT = TimeUnit.SECONDS;
 
 	/**
 	 * Used for calling <code>Task.run()</code>.
 	 */
 	private ExecutorService taskExecutorService;
 
 	/**
 	 * Used for opening dialogs after a specific amount of delay.
 	 */
 	private ScheduledExecutorService timedDialogExecutorService;
 
 	/**
 	 * Used for calling <code>Task.cancel()</code>.
 	 * <code>Task.cancel()</code> must be called in a different
 	 * thread from the thread running Swing. This is done to
 	 * prevent Swing from freezing if <code>Task.cancel()</code>
 	 * takes too long to finish.
 	 *
 	 * This can be the same as <code>taskExecutorService</code>.
 	 */
 	private ExecutorService cancelExecutorService;
 
 	// Parent component of Task Monitor GUI.
 	private Window parent;
 
 	private final JDialogTunableMutator dialogTunableMutator;
 
 	/**
 	 * Construct with default behavior.
 	 * <ul>
 	 * <li><code>owner</code> is set to null.</li>
 	 * <li><code>taskExecutorService</code> is a cached thread pool.</li>
 	 * <li><code>timedExecutorService</code> is a single thread executor.</li>
 	 * <li><code>cancelExecutorService</code> is the same as <code>taskExecutorService</code>.</li>
 	 * </ul>
 	 */
 	public JDialogTaskManager(final JDialogTunableMutator tunableMutator) {
 		super(tunableMutator);
 		this.dialogTunableMutator = tunableMutator;
 
 		parent = null;
 		taskExecutorService = Executors.newCachedThreadPool();
 		addShutdownHook(taskExecutorService);
 		
 		timedDialogExecutorService = Executors.newSingleThreadScheduledExecutor();
 		addShutdownHook(timedDialogExecutorService);
 		
 		cancelExecutorService = taskExecutorService;
 	}
 
 	/**
 	 * Adds a shutdown hook to the JVM that shuts down an
 	 * <code>ExecutorService</code>. <code>ExecutorService</code>s
 	 * need to be told to shut down, otherwise the JVM won't
 	 * cleanly terminate.
 	 */
 	void addShutdownHook(final ExecutorService serviceToShutdown) {
 		// Used to create a thread that is executed by the shutdown hook
 		ThreadFactory threadFactory = Executors.defaultThreadFactory();
 
 		Runnable shutdownHook = new Runnable() {
 			public void run() {
 				serviceToShutdown.shutdownNow();
 			}
 		};
 		Runtime.getRuntime().addShutdownHook(threadFactory.newThread(shutdownHook));
 	}
 
 	/**
 	 * @param parent JDialogs created by this TaskManager will use this 
 	 * to set the parent of the dialog. 
 	 */
 	@Override 
 	public void setExecutionContext(final Window parent) {
 		this.parent = parent;
 	}
 
 
 	@Override 
 	public JDialog getConfiguration(TaskFactory factory, Object tunableContext) {
 		throw new UnsupportedOperationException("There is no configuration available for a DialogTaskManager");	
 	}
 
 
 	@Override
 	public void execute(final TaskIterator iterator) {
 		execute(iterator, null);
 	}
 
 	/**
 	 * For users of this class.
 	 */
 	public void execute(final TaskIterator taskIterator, Object tunableContext) {
 		final SwingTaskMonitor taskMonitor = new SwingTaskMonitor(cancelExecutorService, parent);
 		
 		final Task first; 
 
 		try {
 			dialogTunableMutator.setConfigurationContext(parent);
 
 			if ( tunableContext != null && !displayTunables(tunableContext) ) {
 				taskMonitor.cancel();
 				return;
 			}
 
 			taskMonitor.setExpectedNumTasks( taskIterator.getNumTasks() );
 
 			// Get the first task and display its tunables.  This is a bit of a hack.  
 			// We do this outside of the thread so that the task monitor only gets
 			// displayed AFTER the first tunables dialog gets displayed.
 			first = taskIterator.next();
 			if (!displayTunables(first)) {
 				taskMonitor.cancel();
 				return;
 			}
 
 		} catch (Exception exception) {
 			logger.warn("Caught exception getting and validating task. ", exception);	
 			taskMonitor.showException(exception);
 			return;
 		}
 
 		// create the task thread
 		final Runnable tasks = new TaskThread(first, taskMonitor, taskIterator); 
 
 		// submit the task thread for execution
 		final Future<?> executorFuture = taskExecutorService.submit(tasks);
 
 		openTaskMonitorOnDelay(taskMonitor, executorFuture);
 	}
 
 	// This creates a thread on delay that conditionally displays the task monitor gui
 	// if the task thread has not yet finished.
 	private void openTaskMonitorOnDelay(final SwingTaskMonitor taskMonitor, 
 	                                    final Future<?> executorFuture) {
 		final Runnable timedOpen = new Runnable() {
 			public void run() {
 				if (!(executorFuture.isDone() || executorFuture.isCancelled())) {
 					taskMonitor.setFuture(executorFuture);
 					SwingUtilities.invokeLater(new Runnable() {
 						@Override
 						public void run() {
 							if (!taskMonitor.isClosed()) {
 								taskMonitor.open();
 							}
 						}
 					});
 				}
 			}
 		};
 
 		timedDialogExecutorService.schedule(timedOpen, DELAY_BEFORE_SHOWING_DIALOG, DELAY_TIMEUNIT);
 	}
 
 	private class TaskThread implements Runnable {
 		
 		private final SwingTaskMonitor taskMonitor;
 		private final TaskIterator taskIterator;
 		private final Task first;
 
 		TaskThread(final Task first, final SwingTaskMonitor tm, final TaskIterator ti) {
 			this.first = first;
 			this.taskMonitor = tm;
 			this.taskIterator = ti;
 		}
 		
 		public void run() {
 			try {
 				// actually run the first task 
 				// don't dispaly the tunables here - they were handled above. 
 				taskMonitor.setTask(first);
 				first.run(taskMonitor);
 
 				if (taskMonitor.cancelled())
 					return;
 
 				// now execute all subsequent tasks
 				while (taskIterator.hasNext()) {
 					final Task task = taskIterator.next();
 					taskMonitor.setTask(task);
 
 					// hide the dialog to avoid swing threading issues
 					// while displaying tunables
 					taskMonitor.showDialog(false);
 
 					if (!displayTunables(task)) {
 						taskMonitor.cancel();
 						return;
 					}
 
 					taskMonitor.showDialog(true);
 
 					task.run(taskMonitor);
 
 					if (taskMonitor.cancelled())
 						break;
 				}
			} catch (Throwable exception) {
 				logger.warn("Caught exception executing task. ", exception);
				taskMonitor.showException(new Exception(exception));
 			}
 
 			// clean up the task monitor
 			SwingUtilities.invokeLater(new Runnable() {
 				@Override
 				public void run() {
 					if (taskMonitor.isOpened() && !taskMonitor.isShowingException())
 						taskMonitor.close();
 				}
 			});
 		}
 	}
 
 	private boolean displayTunables(final Object task) throws Exception {
 		if (task == null) {
 			return true;
 		}
 		
 		boolean ret = dialogTunableMutator.validateAndWriteBack(task);
 
 		for ( TunableRecorder ti : tunableRecorders ) 
 			ti.recordTunableState(task);
 
 		return ret;
 	}
 
 }
 
